package nz.fiore.quartxz.model;

import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.quartz.Job;
import org.quartz.JobExecutionContext;

import java.util.Base64;
import java.util.Date;

/**
 * Created by fiorenzo on 21/08/16.
 */
public class VertxJob implements Job {

    private final static Logger logger = LoggerFactory.getLogger(VertxJob.class);
    HttpClient httpClient = null;

    public VertxJob() {
    }

    @Override
    public void execute(JobExecutionContext context) {
        logger.info("VertxJob execute" + new Date() + ", " + context.getJobDetail().getKey().getName());
        try {

            Integer port = (Integer) context.getMergedJobDataMap().get("port");
            String cron = (String) context.getMergedJobDataMap().get("cron");
            logger.info("cron: " + cron);

            String host = (String) context.getMergedJobDataMap().get("host");
            if (host == null) {
                logger.info("host is null:STOP JOB");
                return;
            }

            String path = (String) context.getMergedJobDataMap().get("path");
            String method = (String) context.getMergedJobDataMap().get("method");
            if (method == null) {
                logger.info("method is null:STOP JOB");
                return;
            }

            String username = (String) context.getMergedJobDataMap().get("username");
            String password = (String) context.getMergedJobDataMap().get("password");
            if (username == null) {
                logger.info("username is null");
            }
            JsonObject jsonObject = (JsonObject) context.getMergedJobDataMap().get("jsonObject");
            if (jsonObject == null) {
                logger.info("jsonObject is null");
            } else {
                logger.info(jsonObject.toString());
                logger.info(jsonObject.toString().length());
            }
            httpClient = (HttpClient) context.getMergedJobDataMap().get("httpClient");
            if (httpClient == null) {
                logger.info("httpClient is null:STOP JOB");
                return;
            }


            HttpClientRequest req = null;
            switch (method.toUpperCase()) {
                case "GET":
                    req = httpClient
                            .get(port, host, path, response -> {
                                logger.info("Received response with status code " + response.statusCode());
                                response.bodyHandler(body -> System.out.println("Got data :" + body.toString()));
                            });
                    break;
                case "PUT":
                    req = httpClient
                            .put(port, host, path, response -> {
                                logger.info("Received response with status code " + response.statusCode());
                                response.bodyHandler(body -> System.out.println("Got data :" + body.toString()));
                            });
                    break;
                case "DELETE":
                    req = httpClient
                            .delete(port, host, path, response -> {
                                logger.info("Received response with status code " + response.statusCode());
                                response.bodyHandler(body -> System.out.println("Got data :" + body.toString()));
                            });
                    break;
                case "POST":
                default:
                    req = httpClient
                            .post(port, host, path, response -> {
                                logger.info("Received response with status code " + response.statusCode());
                                response.bodyHandler(body -> System.out.println("Got data :" + body.toString()));
                            });

            }

            httpClient
                    .post(port, host, path, response -> {
                        logger.info("Received response with status code " + response.statusCode());
                        response.bodyHandler(body -> System.out.println("Got data :" + body.toString()));
                    });
            req.exceptionHandler(err -> {
                logger.info("ERRORE: " + err.getMessage());
                err.printStackTrace();
            });
            req.putHeader("Content-Type", "application/json")
                    .putHeader("Content-Length", "" + jsonObject.toString().length());
            if (username != null) {
                req.putHeader("Authorization", "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes()));
            }
            req.write(jsonObject.toString())
                    .end();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
//            if (httpClient != null)
//                httpClient.close();
        }
    }
}