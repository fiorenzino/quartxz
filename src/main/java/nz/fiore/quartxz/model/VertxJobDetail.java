package nz.fiore.quartxz.model;

import io.vertx.core.json.JsonObject;

import java.io.Serializable;

/**
 * Created by fiorenzo on 21/08/16.
 */
public class VertxJobDetail implements Serializable {

    private String uuid;
    private Integer port;
    private boolean ssl;
    private String host;
    private String path;
    private String method;
    private String username;
    private String password;
    private String cron;
    private JsonObject jsonObject;

    public VertxJobDetail() {
    }

    public VertxJobDetail(JsonObject json) {
        this.uuid = json.getString("uuid");
        this.ssl = json.getBoolean("ssl");
        this.port = json.getInteger("port");
        this.host = json.getString("host");
        this.path = json.getString("path");
        this.method = json.getString("method");
        this.username = json.getString("username");
        this.password = json.getString("password");
        this.cron = json.getString("cron");
        this.jsonObject = json.getJsonObject("jsonObject");
    }

    public JsonObject toJson() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.put("uuid", this.uuid)
                .put("port", this.port)
                .put("ssl", this.ssl)
                .put("host", this.host)
                .put("path", this.path)
                .put("method", this.method)
                .put("username", this.username)
                .put("password", this.password)
                .put("cron", this.cron)
                .put("jsonObject", this.jsonObject);
        return jsonObject;
    }

    @Override
    public String toString() {
        return "VertxJobDetail{" +
                "port=" + port +
                ", uuid='" + uuid + '\'' +
                ", host='" + host + '\'' +
                ", ssl='" + ssl + '\'' +
                ", path='" + path + '\'' +
                ", method='" + method + '\'' +
                ", username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", cron='" + cron + '\'' +
                ", jsonObject=" + jsonObject +
                '}';
    }

    public String getUuid() {
        return uuid;
    }

    public VertxJobDetail setUuid(String uuid) {
        this.uuid = uuid;
        return this;
    }

    public Integer getPort() {
        return port;
    }

    public VertxJobDetail setPort(Integer port) {
        this.port = port;
        return this;
    }

    public String getHost() {
        return host;
    }

    public VertxJobDetail setHost(String host) {
        this.host = host;
        return this;
    }

    public String getPath() {
        return path;
    }

    public VertxJobDetail setPath(String path) {
        this.path = path;
        return this;
    }

    public String getMethod() {
        return method;
    }

    public VertxJobDetail setMethod(String method) {
        this.method = method;
        return this;
    }

    public String getUsername() {
        return username;
    }

    public VertxJobDetail setUsername(String username) {
        this.username = username;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public VertxJobDetail setPassword(String password) {
        this.password = password;
        return this;
    }

    public JsonObject getJsonObject() {
        return jsonObject;
    }

    public VertxJobDetail setJsonObject(JsonObject jsonObject) {
        this.jsonObject = jsonObject;
        return this;
    }

    public String getCron() {
        return cron;
    }

    public VertxJobDetail setCron(String cron) {
        this.cron = cron;
        return this;
    }

    public boolean isSsl() {
        return ssl;
    }

    public VertxJobDetail setSsl(boolean ssl) {
        this.ssl = ssl;
        return this;
    }
}
