package nz.fiore.quartxz.verticle;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

/**
 * Created by fiorenzo on 21/08/16.
 */
public class MainVerticle extends AbstractVerticle {

    public static String JOBS_PATH = "/api/v1/jobs";
    public static String TEST_PATH = "/test";

    private final static Logger logger = LoggerFactory.getLogger(MainVerticle.class);

    public static boolean local = false;
    public static boolean test = true;
    String address = "localhost";
    int port = 8080;


    @Override
    public void start() throws Exception {

        String addressProperty = System.getProperty("http.address");
        if (addressProperty != null && !addressProperty.trim().isEmpty()) {
            address = addressProperty;
        }
        String portProperty = System.getProperty("http.port");
        if (portProperty != null && !portProperty.trim().isEmpty()) {
            port = Integer.valueOf(portProperty);
        }

        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());
        router.route().handler(BodyHandler.create());

        QuartzVerticle quartzVerticle = new QuartzVerticle(router, vertx);
        vertx.deployVerticle(quartzVerticle, new DeploymentOptions().setWorker(true));

        //only for test purpose
        if (test) {
            router.get(TEST_PATH).handler(this::test);
        }

        logger.info("START -> address: " + address + ", port: " + port);
        HttpServerOptions options = new HttpServerOptions();
        options.setCompressionSupported(true);
        vertx.createHttpServer(options)
                .requestHandler(router::accept)
                .listen(port, address);

    }

    @Override
    public void stop() throws Exception {
        logger.info("STOP");
    }


    private void test(RoutingContext routingContext) {
        logger.info(routingContext.getBodyAsJson().toString());
        HttpServerResponse response = routingContext.response();
        response.putHeader("content-type", "application/json").end(
                new JsonObject().put("val", "ok")
                        .encodePrettily()
        );
    }


}
