package com.deshaion.transfertest.handler;

import com.deshaion.transfertest.Main;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import lombok.extern.log4j.Log4j2;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
@Log4j2
public class AccountHandlerTest {
    private final static int PORT = 8080;
    private Vertx vertx;

    @Before
    public void before(TestContext context) {
        vertx = Vertx.vertx();
        final DeploymentOptions options = new DeploymentOptions().setConfig(
            new JsonObject("" +
                "{\n" +
                "    \"url\": \"jdbc:hsqldb:mem:test?shutdown=true\",\n" +
                "    \"driver_class\": \"org.hsqldb.jdbcDriver\",\n" +
                "    \"user\": \"SA\",\n" +
                "    \"password\": \"\",\n" +
                "    \"max_pool_size\": 30\n" +
                "}\n")
        );
        vertx.deployVerticle(new Main.MainVerticle(), options, context.asyncAssertSuccess());
    }

    @After
    public void after(TestContext context) {
        vertx.close(context.asyncAssertSuccess());
    }

    @Test(timeout = 3000L)
    public void testAccountAdd(TestContext context) throws Exception {
        Async async = context.async();
        vertx.setTimer(100, id->{
            HttpClientRequest request = vertx.createHttpClient()
                .post(PORT, "localhost", "/api/1.0/accounts", response -> {
                    response.bodyHandler(LOGGER::info);
                    context.assertEquals(201, response.statusCode());
                    async.complete();
                });

            String body = "name=Ivan&balance=100.0&currency=USD";
            request.putHeader("content-length", String.valueOf(body.length()));
            request.putHeader("content-type", "application/x-www-form-urlencoded");
            request.write(body);
            request.end();
        });
    }
}
