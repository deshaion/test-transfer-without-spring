package com.deshaion.transfertest.service.impl;

import com.deshaion.transfertest.dao.AccountsDao;
import com.deshaion.transfertest.dao.TransferDao;
import com.deshaion.transfertest.model.Transfer;
import com.deshaion.transfertest.service.TransferService;
import com.deshaion.transfertest.service.logic.TransferProcessor;
import com.deshaion.transfertest.service.logic.TransferProcessorImpl;
import io.reactivex.Completable;
import io.vertx.core.Future;
import io.vertx.reactivex.core.impl.AsyncResultCompletable;
import io.vertx.reactivex.core.shareddata.SharedData;
import io.vertx.reactivex.ext.jdbc.JDBCClient;
import lombok.extern.log4j.Log4j2;

import javax.inject.Inject;
import java.util.Objects;

@Log4j2
public class TransferServiceImpl implements TransferService {
    private final JDBCClient jdbcClient;
    private final SharedData sharedData;
    private final TransferDao transferDao;
    private final AccountsDao accountsDao;

    @Inject
    public TransferServiceImpl(JDBCClient jdbcClient, SharedData sharedData, TransferDao transferDao, AccountsDao accountsDao) {
        this.jdbcClient = jdbcClient;
        this.sharedData = sharedData;
        this.transferDao = transferDao;
        this.accountsDao = accountsDao;
    }

    @Override
    public Completable createTransfer(Transfer transfer) {
        LOGGER.info("Create transfer {}", transfer);

        return new AsyncResultCompletable(handler -> {
            Future<Void> future = Future.future();
            future.setHandler(handler);

            if (Objects.equals(transfer.getSourceAccountId(), transfer.getTargetAccountId())) {
                future.fail("Source and target can't be the same.");
                return;
            }

            TransferProcessor transferProcessor = TransferProcessorImpl.builder()
                .transfer(transfer)
                .futureResult(future)
                .accountsDao(accountsDao)
                .transferDao(transferDao)
                .jdbcClient(jdbcClient)
                .sharedData(sharedData)
                .build();

            transferProcessor.process();
        });
    }
}
