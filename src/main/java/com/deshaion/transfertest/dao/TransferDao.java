package com.deshaion.transfertest.dao;

import com.deshaion.transfertest.model.Transfer;
import io.reactivex.Single;
import io.vertx.ext.sql.UpdateResult;
import io.vertx.reactivex.ext.sql.SQLConnection;

import java.util.List;

public interface TransferDao {
    Single<UpdateResult> insert(SQLConnection connection, Transfer transfer);

    Single<List<Transfer>> getAll();

    Single<Boolean> isTransferExist(String transferRequestId);
}
