package com.deshaion.transfertest.util;

import com.deshaion.transfertest.handler.AccountHandler;
import com.deshaion.transfertest.handler.ExceptionHandler;
import com.deshaion.transfertest.handler.TransferHandler;
import dagger.Component;

import javax.inject.Singleton;

@Component(modules = AppModule.class)
@Singleton
public interface AppComponent {
    AccountHandler accountHandler();
    TransferHandler transferHandler();
    ExceptionHandler exceptionHandler();
}
