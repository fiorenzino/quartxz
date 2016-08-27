import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import nz.fiore.quartxz.model.VertxJobDetail;
import nz.fiore.quartxz.verticle.MainVerticle;
import org.junit.Assert;
import org.junit.Test;

import static com.jayway.restassured.RestAssured.given;

/**
 * Created by fiorenzo on 21/08/16.
 */
public class FunctionalTests {


    @Test
    public void test16_30_51Second() {
        create("0 53 16 * * ?", "fiorenzo", "pizza");
    }


    @Test
    public void test30Second() {
        create("0/30 * * * * ?", "fiorenzo", "pizza");
    }

    @Test
    public void test1Second() {
        create("0/1 * * * * ?", "fiorenzo", "pizza");
    }


    @Test
    public void testDelete() {
        String id = "faabf4ad-24ed-4b5b-b758-1e1847525e65";
        RestAssured.baseURI = "http://149.202.178.101/";
        RestAssured.port = 80;
        given().
                contentType(ContentType.JSON).
                when().delete(MainVerticle.JOBS_PATH + "/" + id).
                then().assertThat()
                .statusCode(204);
    }

    @Test
    public void testUpdate() {
        String id = "";
        RestAssured.baseURI = "http://149.202.178.101/";
        RestAssured.port = 80;
        VertxJobDetail vertxJobDetail = new VertxJobDetail();
        vertxJobDetail.setHost("localhost")
                .setPort(8080)
                .setCron("0/1 * * * * ?")
                .setMethod("GET")
                .setSsl(false)
                .setPath(MainVerticle.TEST_PATH);
        JsonObject jsonObject = new JsonObject().put("name", "fiorenzo2").put("surname", "pizza2");
        vertxJobDetail.setJsonObject(jsonObject);
        String result = given().
                contentType(ContentType.JSON).
                body(Json.encode(vertxJobDetail)).
                when().put(MainVerticle.JOBS_PATH + "/" + id).
                then().assertThat()
                .statusCode(201).
                        extract()
                .path("id");
        System.out.println(result);
        Assert.assertNotNull(result);
    }

    private void create(String when, String name, String surname) {
        VertxJobDetail vertxJobDetail = new VertxJobDetail();
        vertxJobDetail.setHost("149.202.178.101")
                .setPort(80)
                .setCron(when)
                .setMethod("GET")
                .setSsl(false)
                .setPath(MainVerticle.TEST_PATH);
        JsonObject jsonObject = new JsonObject().put("name", name).put("surname", surname);
        vertxJobDetail.setJsonObject(jsonObject);
        RestAssured.baseURI = "http://149.202.178.101/";
        RestAssured.port = 80;
        String result = given().
                contentType(ContentType.JSON).
                body(Json.encode(vertxJobDetail)).
                when().post(MainVerticle.JOBS_PATH).
                then().assertThat()
                .statusCode(201).
                        extract()
                .path("id");
        System.out.println(result);
        Assert.assertNotNull(result);
    }
}
