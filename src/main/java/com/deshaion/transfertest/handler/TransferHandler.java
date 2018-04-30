package com.deshaion.transfertest.handler;

import com.deshaion.transfertest.dao.TransferDao;
import com.deshaion.transfertest.model.Transfer;
import com.deshaion.transfertest.service.TransferService;
import com.deshaion.transfertest.util.JsonUtils;
import io.vertx.core.json.Json;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import lombok.extern.log4j.Log4j2;

import javax.inject.Inject;

@Log4j2
public class TransferHandler extends BaseHandler {

    private final TransferDao transferDao;
    private final TransferService transferService;

    @Inject
    public TransferHandler(TransferDao transferDao, TransferService transferService) {
        this.transferDao = transferDao;
        this.transferService = transferService;
    }

    public void addRoutes(Router router) {
        router.post("/api/:apiVersion/transfers").handler(this::handleCreateTransfer);
        router.get("/api/:apiVersion/transfers").handler(this::handleGetAllTransfers);
    }

    private void handleGetAllTransfers(RoutingContext context) {
        sendResponse(context, transferDao.getAll(), Json::encodePrettily);
    }

    private void handleCreateTransfer(RoutingContext context) {
        Transfer transfer = new Transfer(JsonUtils.keysToUpperCase(context.getBodyAsJson()));

        sendResponse(context, transferService.createTransfer(transfer), this::noContent);
    }
}
