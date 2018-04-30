package com.deshaion.transfertest.util;

import com.deshaion.transfertest.dao.AccountsDao;
import com.deshaion.transfertest.dao.TransferDao;
import com.deshaion.transfertest.dao.impl.AccountsDaoImpl;
import com.deshaion.transfertest.dao.impl.TransferDaoImpl;
import com.deshaion.transfertest.service.TransferService;
import com.deshaion.transfertest.service.impl.TransferServiceImpl;
import dagger.Module;
import dagger.Provides;
import io.vertx.reactivex.core.shareddata.SharedData;
import io.vertx.reactivex.ext.jdbc.JDBCClient;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import javax.inject.Singleton;

@Module
@AllArgsConstructor
@NoArgsConstructor
public class AppModule {

    private JDBCClient jdbcClient;
    private SharedData sharedData;

    @Provides @Singleton
    AccountsDao provideAccountsDao() {
        return new AccountsDaoImpl(jdbcClient);
    }

    @Provides @Singleton
    TransferDao provideTransferDao() {
        return new TransferDaoImpl(jdbcClient);
    }

    @Provides @Singleton
    TransferService provideTransferService() {
        return new TransferServiceImpl(jdbcClient, sharedData, provideTransferDao(), provideAccountsDao());
    }
}
