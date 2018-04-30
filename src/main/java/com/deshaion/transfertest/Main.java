package com.deshaion.transfertest;

import com.deshaion.transfertest.handler.AccountHandler;
import com.deshaion.transfertest.handler.ExceptionHandler;
import com.deshaion.transfertest.handler.TransferHandler;
import com.deshaion.transfertest.util.AppComponent;
import com.deshaion.transfertest.util.AppModule;
import com.deshaion.transfertest.util.DaggerAppComponent;
import com.deshaion.transfertest.util.LocalDateTimeSerializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.reactivex.Completable;
import io.vertx.core.Future;
import io.vertx.core.Launcher;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.http.HttpServer;
import io.vertx.reactivex.core.impl.AsyncResultCompletable;
import io.vertx.reactivex.ext.jdbc.JDBCClient;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.handler.BodyHandler;
import lombok.extern.log4j.Log4j2;

import java.time.LocalDateTime;

public class Main {
    public static void main(String[] args) {
        String[] vertxArgs = new String[args.length + 1];
        vertxArgs[0] = MainVerticle.class.getName();
        System.arraycopy(args, 0, vertxArgs, 1, args.length);

        Launcher.executeCommand("run", vertxArgs);
    }

    @Log4j2
    public static class MainVerticle extends AbstractVerticle {

        @Override
        public void start() throws Exception {
            LOGGER.debug("Config: {}", config());

            SimpleModule module = new SimpleModule();
            module.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer());
            Json.prettyMapper.registerModule(module);
            Json.mapper.registerModule(module);

            final JDBCClient jdbcClient = JDBCClient.createShared(vertx, new JsonObject()
                .put("url", config().getValue("url"))
                .put("driver_class", config().getValue("driver_class"))
                .put("max_pool_size", config().getValue("max_pool_size"))
                .put("user", config().getValue("user"))
                .put("password", config().getValue("password")));

            AppComponent appComponent = DaggerAppComponent.builder()
                .appModule(new AppModule(jdbcClient, vertx.sharedData()))
                .build();

            AccountHandler accountHandler = appComponent.accountHandler();
            TransferHandler transferHandler = appComponent.transferHandler();
            ExceptionHandler exceptionHandler = appComponent.exceptionHandler();

            Router router = Router.router(vertx);
            router.route().handler(BodyHandler.create());

            accountHandler.addRoutes(router);
            transferHandler.addRoutes(router);
            exceptionHandler.addRoutes(router);

            createDbTables(jdbcClient).subscribe(() -> {
                HttpServer httpServer = vertx.createHttpServer().requestHandler(router::accept).listen(config().getInteger("http.port", 8080));
                LOGGER.info("HTTP server started on port {}", httpServer.actualPort());
            });
        }

        private Completable createDbTables(JDBCClient jdbcClient) {
            final String SQL_CREATE = "CREATE TABLE account\n" +
                "(\n" +
                "    accountId INT PRIMARY KEY NOT NULL IDENTITY,\n" +
                "    token VARCHAR(64),\n" +
                "    name VARCHAR(255),\n" +
                "    active BOOLEAN,\n" +
                "    created TIMESTAMP DEFAULT now NOT NULL,\n" +
                "    updated TIMESTAMP\n" +
                ");\n" +
                "" +
                "CREATE TABLE accountbalance\n" +
                "(\n" +
                "    accountBalanceId INT PRIMARY KEY IDENTITY,\n" +
                "    accountId INT,\n" +
                "    balance DECIMAL(10,2),\n" +
                "    currency VARCHAR(3),\n" +
                "    updated TIMESTAMP,\n" +
                "    CONSTRAINT AccountBalances_ACCOUNTS_ID_fk FOREIGN KEY (accountId) REFERENCES account (accountId) ON DELETE CASCADE ON UPDATE CASCADE\n" +
                ");\n" +
                "" +
                "CREATE TABLE transfer\n" +
                "(\n" +
                "    transferId INT PRIMARY KEY IDENTITY,\n" +
                "    requestId VARCHAR(64) NOT NULL,\n" +
                "    sourceAccountId INT,\n" +
                "    targetAccountId INT,\n" +
                "    amount DECIMAL(10,2) DEFAULT 0 NOT NULL,\n" +
                "    created TIMESTAMP DEFAULT NOW() NOT NULL,\n" +
                "    CONSTRAINT Transfers_ACCOUNTS_ID_fk_source FOREIGN KEY (sourceAccountId) REFERENCES account (accountId) ON DELETE CASCADE,\n" +
                "    CONSTRAINT Transfers_ACCOUNTS_ID_fk_target FOREIGN KEY (targetAccountId) REFERENCES account (accountId) ON DELETE CASCADE\n" +
                ");";

            final String SQL_CREATE_INDEX = "" +
                "CREATE UNIQUE INDEX Accounts_token_uindex ON account (token);" +
                "CREATE INDEX AccountBalance_accountId_index ON accountbalance (accountId);" +
                "CREATE UNIQUE INDEX Transfers_requestId_uindex ON transfer (requestId);";

            return new AsyncResultCompletable(handler -> {
                Future<Void> future = Future.future();
                future.setHandler(handler);

                jdbcClient.getConnection(conn -> {
                    if (conn.failed()) {
                        future.fail(conn.cause());
                    }

                    conn.result().execute(SQL_CREATE, res -> {
                        if (res.failed()) {
                            future.fail(res.cause());
                            return;
                        }
                        LOGGER.info("Tables have been created!");

                        conn.result().execute(SQL_CREATE_INDEX, indexCompleted -> {
                            if (indexCompleted.failed()) {
                                future.fail(indexCompleted.cause());
                                return;
                            }
                            LOGGER.info("Indexes have been created!");
                            future.complete();
                        });
                    });
                });
            });
        }
    }
}
