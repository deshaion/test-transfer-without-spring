package com.deshaion.transfertest.dao;

import com.deshaion.transfertest.model.Account;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.vertx.ext.sql.UpdateResult;
import io.vertx.reactivex.ext.sql.SQLConnection;

import java.math.BigDecimal;
import java.util.List;

public interface AccountsDao {
    Single<Account> insert(Account account);

    Single<List<Account>> getAll();

    Maybe<Account> get(String accountId);

    Maybe<Account> update(String accountId, Account account);

    Completable delete(String accountId);

    Completable deleteAll();

    Single<UpdateResult> updateBalance(SQLConnection connection, String accountId, BigDecimal balanceChange);
}
