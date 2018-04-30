package com.deshaion.transfertest.service.logic;

import com.deshaion.transfertest.dao.AccountsDao;
import com.deshaion.transfertest.dao.TransferDao;
import com.deshaion.transfertest.model.Transfer;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.ext.sql.UpdateResult;
import io.vertx.reactivex.core.shareddata.Lock;
import io.vertx.reactivex.core.shareddata.SharedData;
import io.vertx.reactivex.ext.jdbc.JDBCClient;
import io.vertx.reactivex.ext.sql.SQLConnection;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;

import java.math.BigDecimal;

@Builder
@AllArgsConstructor
@Log4j2
public class TransferProcessorImpl implements TransferProcessor {
    private final Transfer transfer;
    private final JDBCClient jdbcClient;
    private final SharedData sharedData;
    private final TransferDao transferDao;
    private final AccountsDao accountsDao;
    private final Future<Void> futureResult;

    private Lock firstLock = null;
    private Lock secondLock = null;
    private SQLConnection connection = null;

    @Override
    public void process() {
        LOGGER.debug("start process");

        String firstLockAccount = transfer.getSourceAccountId();
        String secondLockAccount = transfer.getTargetAccountId();
        if (firstLockAccount.compareTo(secondLockAccount) > 0) {
            firstLockAccount = transfer.getTargetAccountId();
            secondLockAccount = transfer.getSourceAccountId();
        }

        String finalSecondLock = secondLockAccount;
        sharedData.getLockWithTimeout(firstLockAccount, 30000, lock -> handleFirstLockGetting(lock, finalSecondLock));
    }

    private void handleFirstLockGetting(AsyncResult<Lock> lock, String secondLockAccount) {
        if (lock.failed()) {
            processFail(lock.cause());
            return;
        }
        LOGGER.debug("handleFirstLockGetting");

        firstLock = lock.result();
        sharedData.getLockWithTimeout(secondLockAccount, 30000, this::handleSecondLockGetting);
    }

    private void handleSecondLockGetting(AsyncResult<Lock> lock) {
        if (lock.failed()) {
            processFail(lock.cause());
            return;
        }
        LOGGER.debug("handleSecondLockGetting");

        secondLock = lock.result();
        transferDao.isTransferExist(transfer.getRequestId()).subscribe(this::handleExistenceChecking, this::processFail);
    }

    private void handleExistenceChecking(Boolean isTransferExist) {
        if (isTransferExist) {
            processFail(String.format("Request %s is already processed.", transfer.getRequestId()));
            return;
        }

        LOGGER.debug("handleExistenceChecking");

        jdbcClient.getConnection(this::handleOpenedConnection);
    }

    private void handleOpenedConnection(AsyncResult<SQLConnection> conn) {
        if (conn.failed()) {
            processFail(conn.cause());
            return;
        }

        LOGGER.debug("handleOpenedConnection");

        connection = conn.result();
        conn.result().setAutoCommit(false, this::handleStartTransaction);
    }

    private void handleStartTransaction(AsyncResult<Void> transaction) {
        if (transaction.failed()) {
            processFail(transaction.cause());
        }

        LOGGER.debug("handleStartTransaction");

        accountsDao.updateBalance(connection, transfer.getSourceAccountId(), transfer.getAmount().multiply(BigDecimal.valueOf(-1)))
            .subscribe(this::handleUpdateSourceBalance, this::processFail);
    }

    private void handleUpdateSourceBalance(UpdateResult res) {
        if (res.getUpdated() < 1) {
            processFail(String.format("Account %s has no enough money on the balance or account doesn't exist", transfer.getSourceAccountId()));
            return;
        }

        LOGGER.debug("handleUpdateSourceBalance");

        accountsDao.updateBalance(connection, transfer.getTargetAccountId(), transfer.getAmount())
            .subscribe(this::handleUpdateTargetBalance, this::processFail);
    }

    private void handleUpdateTargetBalance(UpdateResult res) {
        if (res.getUpdated() < 1) {
            processFail(String.format("Account %s has no enough money on the balance or account doesn't exist", transfer.getTargetAccountId()));
            return;
        }

        LOGGER.debug("handleUpdateTargetBalance");

        transferDao.insert(connection, transfer).subscribe(this::handleTransferInserting, this::processFail);
    }

    private void handleTransferInserting(UpdateResult res) {
        LOGGER.debug("handleTransferInserting");

        connection.commit(this::handleCommit);
    }

    private void handleCommit(AsyncResult<Void> commit) {
        LOGGER.debug("handleCommit");

        if (commit.failed()) {
            processFail(commit.cause());
            return;
        }

        futureResult.complete();
        close();
    }

    private void close() {
        if (connection != null) {
            connection.close();
        }
        if (secondLock != null) {
            secondLock.release();
        }
        if (firstLock != null) {
            firstLock.release();
        }
    }

    private void processFail(String cause) {
        close();
        futureResult.fail(cause);
    }

    private void processFail(Throwable cause) {
        close();
        futureResult.fail(cause);
    }
}
