package nz.fiore.quartxz.model;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.quartz.Job;
import org.quartz.JobExecutionContext;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Created by fiorenzo on 21/08/16.
 */
public class VertxJob implements Job {

    private final static Logger logger = LoggerFactory.getLogger(VertxJob.class);
    private HttpClient httpClient = null;
    private Vertx vertx = null;

    public VertxJob() {
    }

    @Override
    public void execute(JobExecutionContext context) {
        try {
            vertx = Vertx.vertx();

            logger.info("VertxJob start" + new Date() + ", " + context.getJobDetail().getKey().getName());
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
            String jsonObjectString = (String) context.getMergedJobDataMap().get("jsonObject");
            JsonObject jsonObject = new JsonObject(jsonObjectString);
            if (jsonObject == null) {
                logger.info("jsonObject is null");
            } else {
                logger.info(jsonObject.toString());
                logger.info(jsonObject.toString().length());
            }
            boolean ssl = false;
            if (context.getMergedJobDataMap().containsKey("ssl")) {
                ssl = (Boolean) context.getMergedJobDataMap().get("ssl");
            }

            if (ssl) {
                httpClient = vertx.createHttpClient(new HttpClientOptions().setSsl(true).setTrustAll(true));
            } else {
                httpClient = vertx.createHttpClient();
            }
            if (httpClient == null) {
                logger.info("httpClient is null:STOP JOB");
                return;
            }
            List<CompletableFuture<String>> callList = new ArrayList<>();
            CompletableFuture<String> cf = new CompletableFuture<>();
            callList.add(cf);
            HttpClientRequest req = null;
            switch (method.toUpperCase()) {
                case "GET":
                    req = httpClient
                            .get(port, host, path, response -> {
                                logger.info("GET  RESPONSE CODE: " + response.statusCode());
                                response.bodyHandler(body -> {
                                    System.out.println("Got data :" + body.toString());
                                    cf.complete("ok");
                                });
                            });
                    break;
                case "PUT":
                    req = httpClient
                            .put(port, host, path, response -> {
                                logger.info("PUT RESPONSE CODE: " + response.statusCode());
                                response.bodyHandler(body -> {
                                    System.out.println("Got data :" + body.toString());
                                    cf.complete("ok");
                                });
                            });
                    break;
                case "DELETE":
                    req = httpClient
                            .delete(port, host, path, response -> {
                                logger.info(" DELETE RESPONSE CODE: " + response.statusCode());
                                response.bodyHandler(body -> {
                                    System.out.println("Got data :" + body.toString());
                                    cf.complete("ok");

                                });
                            });
                    break;
                case "POST":
                default:
                    req = httpClient
                            .post(port, host, path, response -> {
                                logger.info("Received response with status code " + response.statusCode());
                                response.bodyHandler(body -> {
                                    System.out.println("Got data :" + body.toString());
                                    cf.complete("ok");
                                });
                            });

            }
            req.putHeader("Content-Type", "application/json")
                    .putHeader("Content-Length", "" + jsonObject.toString().length());
//
            req.exceptionHandler(err -> {
                logger.info("ERROR: " + err.getMessage());
                err.printStackTrace();
            });

            if (username != null) {
                req.putHeader("Authorization", "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes()));
            }
            req.write(jsonObject.toString())
                    .end();
            CompletableFuture.allOf(callList.toArray(new CompletableFuture[callList.size()]))
                    .thenAccept(v -> {
                        System.out.println("FINITO");
                        if (this.httpClient != null) {
                            this.httpClient.close();
                            this.httpClient = null;
                        }
                        if (this.vertx != null) {
                            this.vertx.close();
                            this.vertx = null;
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}