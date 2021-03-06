package com.deshaion.transfertest.dao.impl;

import com.deshaion.transfertest.dao.AccountsDao;
import com.deshaion.transfertest.model.Account;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLOptions;
import io.vertx.ext.sql.UpdateResult;
import io.vertx.reactivex.core.impl.AsyncResultSingle;
import io.vertx.reactivex.ext.jdbc.JDBCClient;
import io.vertx.reactivex.ext.sql.SQLConnection;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Log4j2
@AllArgsConstructor
public class AccountsDaoImpl implements AccountsDao {

    private final JDBCClient jdbcClient;

    private static final String SQL_INSERT = "INSERT INTO account (accountId, token, name, active, created) VALUES (NULL, ?, ?, ?, NOW())";
    private static final String SQL_INSERT_BALANCE = "INSERT INTO accountbalance (accountBalanceId, accountId, balance, currency, updated) VALUES (NULL, ?, (CAST(? AS DECIMAL(10,2))), ?, NOW())";

    private static final String SQL_QUERY = "SELECT Account.token, Account.name, Account.active, Account.created, Account.updated, " +
        "CAST(AccountBalance.balance AS VARCHAR(100)) Balance, AccountBalance.currency " +
        "FROM Account, AccountBalance WHERE Account.active AND Account.token = ? AND Account.accountId = AccountBalance.accountId";

    private static final String SQL_QUERY_ALL = "SELECT Account.token, Account.name, Account.active, Account.created, Account.updated, " +
        "CAST(AccountBalance.balance AS VARCHAR(100)) Balance, AccountBalance.currency " +
        "FROM Account, AccountBalance WHERE Account.active AND Account.accountId = AccountBalance.accountId";

    private static final String SQL_UPDATE = "UPDATE account SET name = ?, active = ?, updated = NOW() WHERE token = ?";
    private static final String SQL_DELETE = "UPDATE account SET active = false WHERE token = ?";
    private static final String SQL_DELETE_ALL = "UPDATE account SET active = false";

    private static final String  SQL_UPDATE_BALANCE = "UPDATE AccountBalance SET balance = balance + (CAST(? AS DECIMAL(10,2))), updated = NOW() " +
        "WHERE accountId = (SELECT accountId FROM account WHERE active AND token = ?) AND balance + (CAST(? AS DECIMAL(10,2))) >= 0";

    @Override
    public Single<Account> insert(Account account) {
        return new AsyncResultSingle<>(handler -> {
            Future<Account> future = Future.future();
            future.setHandler(handler);

            AccountInserter accountInserter = AccountInserter.builder()
                .jdbcClient(jdbcClient)
                .account(account)
                .future(future)
                .build();

            accountInserter.process();
        });
    }

    @AllArgsConstructor
    @Builder
    @Log4j2
    private static class AccountInserter {
        private final JDBCClient jdbcClient;
        private Future<Account> future;
        private SQLConnection connection;
        private Account account;
        private String newToken;

        public void process() {
            LOGGER.debug("Create {}", account);

            jdbcClient.getConnection(this::handleOpenedConnection);
        }

        private void handleOpenedConnection(AsyncResult<SQLConnection> conn) {
            if (conn.failed()) {
                processFail(conn.cause());
                return;
            }

            LOGGER.debug("handleOpenedConnection");

            connection = conn.result();
            connection.setAutoCommit(false, this::handleStartTransaction);
        }

        private void handleStartTransaction(AsyncResult<Void> transaction) {
            if (transaction.failed()) {
                processFail(transaction.cause());
            }

            LOGGER.debug("handleStartTransaction");
            newToken = UUID.randomUUID().toString();

            JsonArray accountParams = new JsonArray()
                .add(newToken)
                .add(account.getName())
                .add(true);

            connection
                .setOptions(new SQLOptions().setAutoGeneratedKeys(true))
                .updateWithParams(SQL_INSERT, accountParams, this::handleAccountInsert);

        }

        private void handleAccountInsert(AsyncResult<UpdateResult> accountInsert) {
            if (accountInsert.failed()) {
                processFail(accountInsert.cause());
                return;
            }

            JsonArray accountBalanceParams = new JsonArray()
                .add(accountInsert.result().getKeys().getInteger(0))
                .add(account.getBalance().toString())
                .add(account.getCurrency());

            connection.updateWithParams(SQL_INSERT_BALANCE, accountBalanceParams, this::handleAccountBalanceInsert);
        }

        private void handleAccountBalanceInsert(AsyncResult<UpdateResult> balanceInsert) {
            if (balanceInsert.failed()) {
                processFail(balanceInsert.cause());
                return;
            }

            connection.commit(this::handleCommit);
        }

        private void handleCommit(AsyncResult<Void> commit) {
            if (commit.failed()) {
                processFail(commit.cause());
                return;
            }

            Account accountReturn = Account.builder()
                .token(newToken)
                .name(account.getName())
                .active(true)
                .balance(account.getBalance())
                .currency(account.getCurrency())
                .build();

            future.complete(accountReturn);
            connection.close();
        }

        private void processFail(Throwable cause) {
            if (connection != null) {
                connection.close();
            }
            future.fail(cause);
        }
    }

    @Override
    public Single<List<Account>> getAll() {
        return jdbcClient.rxQuery(SQL_QUERY_ALL)
            .map(ar -> ar.getRows().stream()
                .map(Account::new)
                .collect(Collectors.toList())
            );
    }

    @Override
    public Maybe<Account> get(String accountId) {
        return jdbcClient.rxQueryWithParams(SQL_QUERY, new JsonArray().add(accountId))
            .map(ResultSet::getRows)
            .toObservable()
            .flatMapIterable(e -> e)
            .singleElement().map(Account::new);
    }

    @Override
    public Maybe<Account> update(String accountId, Account account) {
        JsonArray accountParams = new JsonArray()
            .add(account.getName())
            .add(account.getActive())
            .add(accountId);

        return jdbcClient.rxUpdateWithParams(SQL_UPDATE, accountParams)
                    .flatMapMaybe(v -> get(accountId));
    }

    @Override
    public Completable delete(String accountId) {
        return jdbcClient.rxUpdateWithParams(SQL_DELETE, new JsonArray().add(accountId)).toCompletable();
    }

    @Override
    public Completable deleteAll() {
        return jdbcClient.rxUpdate(SQL_DELETE_ALL).toCompletable();
    }

    @Override
    public Single<UpdateResult> updateBalance(SQLConnection connection, String accountId, BigDecimal balanceChange) {
        JsonArray accountParams = new JsonArray()
            .add(balanceChange.toString())
            .add(accountId)
            .add(balanceChange.toString());

        return connection.rxUpdateWithParams(SQL_UPDATE_BALANCE, accountParams);
    }
}
