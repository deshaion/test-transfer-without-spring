package com.deshaion.transfertest.handler;

import io.vertx.core.http.HttpMethod;
import io.vertx.reactivex.ext.web.Route;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;

import javax.inject.Inject;

public class ExceptionHandler extends BaseHandler {

    @Inject
    public ExceptionHandler() {
    }

    public void addRoutes(Router router) {
        //TODO There is still I can't handle all exceptions
        Route route = router.route().method(HttpMethod.GET).method(HttpMethod.POST).method(HttpMethod.PUT).method(HttpMethod.DELETE).path("/api/*");
        route.failureHandler(this::failureHandler);
    }

    private void failureHandler(RoutingContext context) {
        serviceUnavailable(context, context.failure());
    }
}
