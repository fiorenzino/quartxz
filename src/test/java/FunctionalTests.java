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
    public void test30Second() {
        VertxJobDetail vertxJobDetail = new VertxJobDetail();
        vertxJobDetail.setHost("localhost")
                .setPort(8080)
                .setCron("0/30 * * * * ?")
                .setMethod("GET")
                .setSsl(false)
                .setPath("/test");
        JsonObject jsonObject = new JsonObject().put("name", "fiorenzo").put("surname", "pizza");
        vertxJobDetail.setJsonObject(jsonObject);
        String result = given().
                contentType(ContentType.JSON).
                body(Json.encode(vertxJobDetail)).
                when().post(MainVerticle.JOBS_PATH).
                then().assertThat()
                .statusCode(201).
                        extract()
                .path("uuid");
        System.out.println(result);
        Assert.assertNotNull(result);
    }

    @Test
    public void test1Second() {
        VertxJobDetail vertxJobDetail = new VertxJobDetail();
        vertxJobDetail.setHost("localhost")
                .setPort(8080)
                .setCron("0/1 * * * * ?")
                .setMethod("GET")
                .setSsl(false)
                .setPath("/test");
        JsonObject jsonObject = new JsonObject().put("name", "fiorenzo").put("surname", "pizza");
        vertxJobDetail.setJsonObject(jsonObject);
        String result = given().
                contentType(ContentType.JSON).
                body(Json.encode(vertxJobDetail)).
                when().post(MainVerticle.JOBS_PATH).
                then().assertThat()
                .statusCode(201).
                        extract()
                .path("uuid");
        System.out.println(result);
        Assert.assertNotNull(result);
    }
}
