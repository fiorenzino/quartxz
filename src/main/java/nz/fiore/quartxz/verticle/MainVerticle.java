package nz.fiore.quartxz.verticle;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import nz.fiore.quartxz.model.VertxJob;
import nz.fiore.quartxz.model.VertxJobDetail;
import org.quartz.*;
import org.quartz.impl.JobDetailImpl;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.matchers.GroupMatcher;
import org.quartz.impl.triggers.CronTriggerImpl;

import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Created by fiorenzo on 21/08/16.
 */
public class MainVerticle extends AbstractVerticle {

    public static String JOBS_PATH = "/api/v1/jobs";
    public static String TEST_PATH = "/test";

    private final static Logger logger = LoggerFactory.getLogger(MainVerticle.class);
    private boolean local = true;
    private Scheduler scheduler;


    @Override
    public void start() throws Exception {
        System.out.println("START");
//        httpClient = vertx.createHttpClient(new HttpClientOptions().setSsl(true).setTrustAll(true));

        String address = System.getProperty("http.address");
        String port = System.getProperty("http.port");
        if (local) {
            address = "0.0.0.0";
            port = "8080";
            this.scheduler = new StdSchedulerFactory().getDefaultScheduler();
            this.scheduler.start();
        } else {
            this.scheduler = new StdSchedulerFactory("quartz-mysql.properties").getDefaultScheduler();
            this.scheduler.start();
        }
        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());
        router.route().handler(BodyHandler.create());
        router.get(JOBS_PATH + "/:id").handler(this::find);
        router.put(JOBS_PATH + "/:id").handler(this::update);
        router.delete(JOBS_PATH + "/:id").handler(this::delete);
        router.post(JOBS_PATH).handler(this::create);
        router.get(JOBS_PATH).handler(this::list);
        router.get(TEST_PATH).handler(this::test);


        logger.info("address: " + address + ", port: " + port);
        HttpServerOptions options = new HttpServerOptions();
        options.setCompressionSupported(true);
        vertx.createHttpServer(options)
                .requestHandler(router::accept)
                .listen(
                        Integer.valueOf(port), address);

    }

    @Override
    public void stop() throws Exception {
        System.out.println("STOP");
        this.scheduler.shutdown();
        this.scheduler = null;
    }

    private JsonObject toJson(JobDetail jobDetail) {
        return new JsonObject();
    }

    private void test(RoutingContext routingContext) {
        logger.info(routingContext.getBodyAsJson().toString());
        HttpServerResponse response = routingContext.response();
        response.putHeader("content-type", "application/json").end(
                new JsonObject().put("val", "ok")
                        .encodePrettily()
        );
    }


    private void find(RoutingContext routingContext) {
        String id = routingContext.request().getParam("id");
        HttpServerResponse response = routingContext.response();
        JobDetail jobDetail = null;
        if (id == null) {
            sendError(400, response);
        } else {
            try {
                jobDetail = this.scheduler.getJobDetail(new JobKey(id));
            } catch (SchedulerException e) {
                e.printStackTrace();
            }
            if (jobDetail == null) {
                sendError(404, response);
            } else {
                response.putHeader("content-type", "application/json").end(toJson(jobDetail).encodePrettily());
            }
        }
    }

    private void delete(RoutingContext routingContext) {
        String id = routingContext.request().getParam("id");
        HttpServerResponse response = routingContext.response();
        if (id == null) {
            sendError(400, response);
        } else {

            try {
                this.scheduler.deleteJob(new JobKey(id));
            } catch (SchedulerException e) {
                e.printStackTrace();
                sendError(404, response);
            }
            response.putHeader("content-type", "application/json").end();
        }
    }

    private void update(RoutingContext routingContext) {
        String id = routingContext.request().getParam("id");
        HttpServerResponse response = routingContext.response();
        JobDetail jobDetail = null;
        if (id == null) {
            sendError(400, response);
        } else {
            try {
                jobDetail = this.scheduler.getJobDetail(new JobKey(id));
            } catch (SchedulerException e) {
                e.printStackTrace();
            }
            if (jobDetail == null) {
                sendError(404, response);
            } else {
                response.putHeader("content-type", "application/json").end(toJson(jobDetail).encodePrettily());
            }
        }
    }

    private void create(RoutingContext routingContext) {
        VertxJobDetail vertxJobDetail = new VertxJobDetail(routingContext.getBodyAsJson());
        HttpClient httpClient;
        if (vertxJobDetail.isSsl()) {
            httpClient = vertx.createHttpClient(new HttpClientOptions().setSsl(true).setTrustAll(true));
        } else {
            httpClient = vertx.createHttpClient();
        }
        HttpServerResponse response = routingContext.response();
        String uuid = UUID.randomUUID().toString();
        if (vertxJobDetail == null) {
            logger.info("VERTX DETAIL IS NULL");
            sendError(400, response);
        } else {
            JobDetailImpl jobDetail = new JobDetailImpl();
            jobDetail.setJobClass(VertxJob.class);
            JobKey jobKey = new JobKey(uuid);
            jobDetail.setKey(jobKey);
            JobDataMap jobDataMap = new JobDataMap();
            jobDataMap.put("host", vertxJobDetail.getHost());
            jobDataMap.put("port", vertxJobDetail.getPort());
            jobDataMap.put("cron", vertxJobDetail.getCron());
            jobDataMap.put("path", vertxJobDetail.getPath());
            jobDataMap.put("method", vertxJobDetail.getMethod());
            jobDataMap.put("username", vertxJobDetail.getUsername());
            jobDataMap.put("password", vertxJobDetail.getPassword());
            jobDataMap.put("jsonObject", vertxJobDetail.getJsonObject());
            jobDataMap.put("httpClient", httpClient);
            jobDetail.setJobDataMap(jobDataMap);

            logger.info("scheduling: " + uuid + ", detail: " + vertxJobDetail.toString());

            CronTriggerImpl trigger = new CronTriggerImpl();
            trigger.setName(uuid);
            Date date = null;
            try {
                trigger.setCronExpression(vertxJobDetail.getCron());
            } catch (ParseException e) {
                e.printStackTrace();
            }
            try {
                date = this.scheduler.scheduleJob(jobDetail, trigger);
                logger.info("scheduling: " + uuid + ", date: " + date);
            } catch (SchedulerException e) {
                e.printStackTrace();
            }
            if (date == null) {
                sendError(400, response);
            } else {
                vertxJobDetail.setUuid(uuid);
                response.setStatusCode(201)
                        .putHeader("content-type",
                                "application/json; charset=utf-8").end(Json.encodePrettily(vertxJobDetail.toJson()));
            }
        }
    }

    private void list(RoutingContext routingContext) {
        JsonArray arr = new JsonArray();
        try {
            for (String groupName : scheduler.getJobGroupNames()) {
                for (JobKey jobKey : scheduler.getJobKeys(GroupMatcher.jobGroupEquals(groupName))) {
                    String id = jobKey.getName();
                    //get job's trigger
                    List<Trigger> triggers = (List<Trigger>) scheduler.getTriggersOfJob(jobKey);
                    Date nextFireTime = triggers.get(0).getNextFireTime();
                    JsonObject jsonObject = new JsonObject().put("id", id).put("groupName", groupName).put("next", nextFireTime);
                    System.out.println("[jobName] : " + id + " - " + nextFireTime);
                    arr.add(jsonObject);
                }

            }
        } catch (SchedulerException e) {
            e.printStackTrace();
        }
        routingContext.response().putHeader("content-type", "application/json").end(arr.encodePrettily());
    }

    private void sendError(int statusCode, HttpServerResponse response) {
        response.setStatusCode(statusCode).end();
    }

}
