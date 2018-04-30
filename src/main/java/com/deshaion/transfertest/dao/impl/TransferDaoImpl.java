package com.deshaion.transfertest.dao.impl;

import com.deshaion.transfertest.dao.TransferDao;
import com.deshaion.transfertest.model.Transfer;
import io.reactivex.Single;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.sql.UpdateResult;
import io.vertx.reactivex.ext.jdbc.JDBCClient;
import io.vertx.reactivex.ext.sql.SQLConnection;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.util.List;
import java.util.stream.Collectors;

@Log4j2
@AllArgsConstructor
public class TransferDaoImpl implements TransferDao {

    private final JDBCClient jdbcClient;

    private static final String SQL_INSERT = "INSERT INTO Transfer (transferId, requestId, sourceAccountId, targetAccountId, amount, created) " +
        "VALUES (NULL, ?, (SELECT accountId FROM account WHERE token = ?), (SELECT accountId FROM account WHERE token = ?), (CAST(? AS DECIMAL(10,2))), NOW())";
    private static final String SQL_QUERY_ALL = "SELECT * FROM Transfer";
    private static final String SQL_CHECK_EXIST = "SELECT requestId FROM Transfer WHERE requestId = ?";

    @Override
    public Single<UpdateResult> insert(SQLConnection connection, Transfer transfer) {
        JsonArray params = new JsonArray()
            .add(transfer.getRequestId())
            .add(transfer.getSourceAccountId())
            .add(transfer.getTargetAccountId())
            .add(transfer.getAmount().toString());

        return connection.rxUpdateWithParams(SQL_INSERT, params);
    }

    @Override
    public Single<List<Transfer>> getAll() {
        return jdbcClient.rxQuery(SQL_QUERY_ALL)
            .map(ar -> ar.getRows().stream()
                .map(Transfer::new)
                .collect(Collectors.toList())
            );
    }

    @Override
    public Single<Boolean> isTransferExist(String transferRequestId) {
        JsonArray params = new JsonArray().add(transferRequestId);

        return jdbcClient.rxQueryWithParams(SQL_CHECK_EXIST, params)
            .map(ar -> ar.getRows().size() > 0);
    }
}
