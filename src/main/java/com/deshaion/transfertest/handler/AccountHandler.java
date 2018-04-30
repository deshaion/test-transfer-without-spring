package com.deshaion.transfertest.handler;

import com.deshaion.transfertest.dao.AccountsDao;
import com.deshaion.transfertest.model.Account;
import com.deshaion.transfertest.util.JsonUtils;
import io.vertx.core.json.Json;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import lombok.extern.log4j.Log4j2;

import javax.inject.Inject;
import java.math.BigDecimal;

@Log4j2
public class AccountHandler extends BaseHandler {

    private final AccountsDao accountsDao;

    @Inject
    public AccountHandler(AccountsDao accountsDao) {
        this.accountsDao = accountsDao;
    }

    public void addRoutes(Router router) {
        router.get("/api/:apiVersion/accounts").handler(this::handleGetAllAccounts);
        router.get("/api/:apiVersion/accounts/:id").handler(this::handleGetAccount);
        router.post("/api/:apiVersion/accounts").handler(this::handleAddAccount);
        router.put("/api/:apiVersion/accounts/:id").handler(this::handleUpdateAccount);
        router.delete("/api/:apiVersion/accounts/:id").handler(this::handleDeleteAccount);
        router.delete("/api/:apiVersion/accounts").handler(this::handleDeleteAllAccounts);
    }

    private void handleGetAllAccounts(RoutingContext context) {
        sendResponse(context, accountsDao.getAll(), Json::encodePrettily);
    }

    private void handleGetAccount(RoutingContext context) {
        String accountId = context.request().getParam("id");
        if (accountId == null) {
            badRequest(context);
            return;
        }
        sendResponse(context, accountsDao.get(accountId), Json::encodePrettily);
    }

    private void handleUpdateAccount(RoutingContext context) {
        try {
            String accountId = context.request().getParam("id");
            final Account newAccount = new Account(JsonUtils.keysToUpperCase(context.getBodyAsJson()));
            if (accountId == null) {
                badRequest(context);
                return;
            }
            sendResponse(context, accountsDao.update(accountId, newAccount), Json::encodePrettily);
        } catch (Exception ex) {
            badRequest(context, ex);
        }
    }

    private void handleAddAccount(RoutingContext context) {
        Account account = Account.builder()
            .name(context.request().getParam("name"))
            .balance(new BigDecimal(context.request().getParam("balance")))
            .currency(context.request().getParam("currency"))
            .build();

        sendResponse(context, accountsDao.insert(account), Json::encodePrettily, this::created);
    }

    private void handleDeleteAccount(RoutingContext context) {
        String accountId = context.request().getParam("id");
        if (accountId == null) {
            badRequest(context);
            return;
        }
        sendResponse(context, accountsDao.delete(accountId), this::noContent);
    }

    private void handleDeleteAllAccounts(RoutingContext context) {
        sendResponse(context, accountsDao.deleteAll(), this::noContent);
    }
}
