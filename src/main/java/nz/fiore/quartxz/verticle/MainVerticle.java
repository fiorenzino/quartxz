package nz.fiore.quartxz.verticle;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerResponse;
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
    private boolean local = false;
    private boolean test = true;
    private Scheduler scheduler;

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
        if (local) {
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
        router.route().handler(BodyHandler.create());
        router.get(JOBS_PATH + "/:id").handler(this::find);
        router.put(JOBS_PATH + "/:id").handler(this::update);
        router.delete(JOBS_PATH + "/:id").handler(this::delete);
        router.post(JOBS_PATH).handler(this::create);
        router.get(JOBS_PATH).handler(this::list);

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
        this.scheduler.shutdown();
        this.scheduler = null;
    }

    private JsonObject toJson(JobDetail jobDetail, Scheduler scheduler, Date next) throws Exception {
        final JsonArray jsonArray = new JsonArray();
        if (next == null) {
            List<Trigger> triggers = (List<Trigger>) scheduler.getTriggersOfJob(jobDetail.getKey());
            if (triggers != null && triggers.size() > 1) {
                triggers.forEach(trigger -> {
                    jsonArray.add(trigger.getNextFireTime().toInstant());
                });
                next = triggers.get(0).getNextFireTime();
            }
        }
        JsonObject jsonObject = new JsonObject()
                .put("id", jobDetail.getKey().getName())
                .put("groupName", jobDetail.getKey().getGroup())
                .put("next", next.toInstant());
        if (jsonArray.size() > 0) {
            jsonObject.put("dates", jsonArray);
        }
        return jsonObject;

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
        JobDataMap jobDataMap = null;
        if (id == null) {
            sendError(400, response);
        } else {
            try {
                jobDetail = this.scheduler.getJobDetail(new JobKey(id));
                jobDataMap = jobDetail.getJobDataMap();
            } catch (SchedulerException e) {
                e.printStackTrace();
            }
            if (jobDetail == null) {
                sendError(404, response);
            } else {
                JsonObject jsonObject = null;
                try {
                    List<Trigger> triggers = (List<Trigger>) scheduler.getTriggersOfJob(jobDetail.getKey());
                    Date nextFireTime = triggers.get(0).getNextFireTime();
                    jsonObject = new JsonObject()
                            .put("id", id)
                            .put("groupName", jobDetail.getKey().getGroup())
                            .put("next", nextFireTime.toInstant())
                            .put("host", jobDataMap.get("host"))
                            .put("port", jobDataMap.get("port"))
                            .put("cron", jobDataMap.get("cron"))
                            .put("description", jobDataMap.get("description"))
                            .put("path", jobDataMap.get("path"))
                            .put("method", jobDataMap.get("method"))
                            .put("username", jobDataMap.get("username"))
                            .put("password", jobDataMap.get("password"))
                            .put("jsonObject", jobDataMap.get("jsonObject"));

                    logger.info("[jobName] : " + id + " - " + nextFireTime);

                } catch (Exception e) {
                    sendError(500, response);
                }
                response
                        .putHeader("content-type", "application/json")
                        .setStatusCode(200)
                        .end(jsonObject.encodePrettily());
            }
        }
    }

    private void delete(RoutingContext routingContext) {
        String id = routingContext.request().getParam("id");
        HttpServerResponse response = routingContext.response();
        if (id == null) {
            sendError(400, response);
        } else {
            JobKey jobKey = new JobKey(id);
            try {
                boolean exist = this.scheduler.checkExists(jobKey);
                if (exist) {
                    this.scheduler.deleteJob(jobKey);
                    response.setStatusCode(204).end();
                    return;
                } else {
                    sendError(404, response);
                }
            } catch (SchedulerException e) {
                e.printStackTrace();
                sendError(404, response);
            }

        }
    }

    private void update(RoutingContext routingContext) {
        String id = routingContext.request().getParam("id");
        HttpServerResponse response = routingContext.response();
        JobDetail jobDetail = null;
        if (id == null) {
            sendError(400, response);
        } else {
            JobKey jobKey = new JobKey(id);
            try {
                boolean exist = this.scheduler.checkExists(jobKey);
                if (exist) {
                    this.scheduler.deleteJob(jobKey);
                    create(routingContext);
                    return;
                } else {
                    sendError(404, response);
                    return;
                }

            } catch (Exception e) {
                e.printStackTrace();
                sendError(500, response);
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
            jobDataMap.put("description", vertxJobDetail.getDescription());
            jobDataMap.put("path", vertxJobDetail.getPath());
            jobDataMap.put("method", vertxJobDetail.getMethod());
            jobDataMap.put("username", vertxJobDetail.getUsername());
            jobDataMap.put("password", vertxJobDetail.getPassword());
            jobDataMap.put("jsonObject", vertxJobDetail.getJsonObject());
            jobDataMap.put("httpClient", httpClient);
            jobDetail.setJobDataMap(jobDataMap);

            CronTriggerImpl trigger = new CronTriggerImpl();
            trigger.setName(uuid);
            Date date = null;
            try {
                trigger.setCronExpression(vertxJobDetail.getCron());
            } catch (ParseException e) {
                e.printStackTrace();
            }
            JsonObject jsonObject = null;
            try {
                date = this.scheduler.scheduleJob(jobDetail, trigger);
                logger.info("scheduling: " + uuid + ", date: " + date + ", detail: " + vertxJobDetail.toString());
                if (date == null) {
                    sendError(400, response);
                    return;
                } else {
                    jsonObject = toJson(jobDetail, this.scheduler, date);
                    response.setStatusCode(201)
                            .putHeader("content-type", "application/json; charset=utf-8")
                            .end(jsonObject.encodePrettily());
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
                sendError(500, response);
                return;
            }

        }
    }

    private void list(RoutingContext routingContext) {
        HttpServerResponse response = routingContext.response();
        JsonArray arr = new JsonArray();
        try {
            for (String groupName : scheduler.getJobGroupNames()) {
                for (JobKey jobKey : scheduler.getJobKeys(GroupMatcher.jobGroupEquals(groupName))) {
                    String id = jobKey.getName();
                    //get job's trigger
                    List<Trigger> triggers = (List<Trigger>) scheduler.getTriggersOfJob(jobKey);
                    Date nextFireTime = triggers.get(0).getNextFireTime();
                    JsonObject jsonObject = new JsonObject()
                            .put("id", id)
                            .put("groupName", groupName)
                            .put("next", nextFireTime.toInstant());
                    logger.info("[jobName] : " + id + " - " + nextFireTime);
                    arr.add(jsonObject);
                }

            }
        } catch (SchedulerException e) {
            e.printStackTrace();
            sendError(500, response);
            return;
        }
        response.putHeader("content-type", "application/json").end(arr.encodePrettily());
    }

    private void sendError(int statusCode, HttpServerResponse response) {
        response.setStatusCode(statusCode).end();
    }

}
