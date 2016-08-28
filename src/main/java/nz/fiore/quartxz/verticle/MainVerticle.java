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
import org.quartz.Scheduler;
import org.quartz.impl.StdSchedulerFactory;

/**
 * Created by fiorenzo on 21/08/16.
 */
public class MainVerticle extends AbstractVerticle {

    public static String JOBS_PATH = "/api/v1/jobs";
    public static String TEST_PATH = "/test";

    private final static Logger logger = LoggerFactory.getLogger(MainVerticle.class);

    public static boolean local = false;
    public static boolean test = true;
    private Scheduler scheduler;
    int port = 8080;


    @Override
    public void start() throws Exception {

        String portProperty = System.getProperty("http.port");
        if (portProperty != null && !portProperty.trim().isEmpty()) {
            port = Integer.valueOf(portProperty);
        }

        if (MainVerticle.local) {
            logger.info("LOCAL DB");
            this.scheduler = new StdSchedulerFactory().getDefaultScheduler();
            this.scheduler.start();
        } else {
            logger.info("NO LOCAL DB");
            this.scheduler = new StdSchedulerFactory("quartz-mysql.properties").getScheduler();
            this.scheduler.start();
        }

        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());

        QuartzVerticle quartzVerticle = new QuartzVerticle(router, vertx, this.scheduler);
        vertx.deployVerticle(quartzVerticle, new DeploymentOptions().setWorker(true).setWorkerPoolSize(30));

        //only for test purpose
        if (test) {
            router.get(TEST_PATH).handler(this::test);
        }

        logger.info("START -> port: " + port);
        HttpServerOptions options = new HttpServerOptions();
        options.setCompressionSupported(true);
        vertx.createHttpServer(options)
                .requestHandler(router::accept)
                .listen(port);

    }

    @Override
    public void stop() throws Exception {
        logger.info("STOP");
        this.scheduler.shutdown();
        this.scheduler = null;
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
