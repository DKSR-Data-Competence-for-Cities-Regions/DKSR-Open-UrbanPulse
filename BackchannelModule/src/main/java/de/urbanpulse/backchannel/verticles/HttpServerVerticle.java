package de.urbanpulse.backchannel.verticles;


import de.urbanpulse.backchannel.auth.Authorization;
import de.urbanpulse.backchannel.auth.BasicAuthorization;
import de.urbanpulse.backchannel.pojos.Credential;
import de.urbanpulse.backchannel.pojos.UsernamePasswordCredential;

import de.urbanpulse.backchannel.utils.ElasticQueryHelper;
import de.urbanpulse.util.AccessLogger;
import io.vertx.core.*;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.*;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.serviceproxy.ServiceProxyBuilder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author Steffen Haertlein
 */
public class HttpServerVerticle extends AbstractVerticle {

    private final static Logger LOGGER = LoggerFactory.getLogger(HttpServerVerticle.class);
    private HttpServer httpServer;
    private Authorization auth;



    //private AuthChecker authChecker;

    private static final AccessLogger ACCESS_LOGGER = AccessLogger.newInstance(HttpServerVerticle.class);

    @Override
    public void start() {
        final JsonObject config = config();
        auth = new BasicAuthorization();
        initAuthorizedUsers(config.getJsonArray("authorizedUsers", new JsonArray()));
        Router router = Router.router(vertx);
        registerCorsHandler(router);
        //setupSecuredRouter(router);
        registerApiRouteAndHandler(router, config);
        httpServer = startHttpServer(router,config);
    }

    private void registerCorsHandler(Router router) {
        CorsHandler corsHandler = CorsHandler.create("*");
        corsHandler.allowedMethod(HttpMethod.GET).allowedMethod(HttpMethod.OPTIONS);
        corsHandler.allowedHeader("Content-Type").allowedHeader("Accept").allowedHeader("Authorization");
        router.route().handler(corsHandler);
    }

    private void registerApiRouteAndHandler(Router router, final JsonObject config) {

        router.post("/backchannel/queries")
                .handler((RoutingContext routingContext) -> {
                    LOGGER.info("Handling incoming send  API request...");
                    //router.

                    HttpServerRequest req = routingContext.request();
                    this.handleSearchAPI(req,config);
                });
    }

    private HttpServer startHttpServer(Router router, final JsonObject config) {
        String host = config.getString("host");
        int port = config.getInteger("port");
        boolean encrypt = config.getBoolean("encrypt", true);

        HttpServerOptions options = new HttpServerOptions();
        options.setTcpKeepAlive(true);
        options.setSsl(encrypt);

        if (encrypt) {
            String keystore = config.getString("keystore");
            String keystorePassword = config.getString("keystorePassword");
            JksOptions jksOptions = new JksOptions();
            jksOptions.setPath(keystore);
            jksOptions.setPassword(keystorePassword);
            options.setKeyStoreOptions(jksOptions);
            JsonArray cipherSuites = config.getJsonArray("cipherSuites", new JsonArray());
            for (Object suite : cipherSuites) {
                options.addEnabledCipherSuite((String) suite);
            }
        }

        HttpServer server = vertx.createHttpServer(options).requestHandler(router);

        server.listen(port, host, handler -> {
            if (handler.succeeded()) {
                LOGGER.info("Server running at " + host + ":" + server.actualPort());
            } else {
                LOGGER.fatal("Unable to start HTTPS Server: ", handler.cause());
            }
        });
        return server;
    }




    private void handleSearchAPI(HttpServerRequest request, final JsonObject configLocal) {
        HttpMethod method = request.method();
        HttpServerResponse response = request.response();

        ACCESS_LOGGER.log(request);
        if (!HttpMethod.POST.equals(method)) {
            respond(response, 405, "unsupported method " + method + ", use POST method");
            return;
        }


        String authorization = request.getHeader("Authorization");
        if (!isAuthorized(authorization, response)) {
            return;
        }

        try {



            request.bodyHandler((Buffer buffer) -> {
                String message = buffer.toString("UTF-8");

                JsonObject obj = new JsonObject(message);

                String indexName = "default-up-"+obj.getString("eventType").toLowerCase() + "*";

                //LOGGER.info("RECIEVED INPUT JSON : " + message);
                LOGGER.info("RECIEVED INDEX NAME : " + indexName);

                String temp = this.validateData(obj, configLocal);

                if(!temp.equalsIgnoreCase("success"))
                    respond(response, 400, temp);


                ElasticQueryHelper helper = new ElasticQueryHelper();


                JsonObject conditionObj = obj.getJsonObject("condition");

                if(conditionObj == null)
                {
                    conditionObj = new JsonObject();
                    conditionObj.put("NOCONDITION","NOCONDITION");
                }


                Iterator<String> condition = conditionObj.fieldNames().iterator();
                JsonObject req = null;
                while (condition.hasNext()) {

                    String field = condition.next();
                    if (field.equalsIgnoreCase("NOCONDITION")) {

                        req = helper.queryElasticAggregation(obj, configLocal.getJsonObject("operationConfig").getString(obj.getString("operationType")));
                    }
                    else {
                        req = helper.queryElasticReq(obj, field,
                                configLocal.getJsonObject("operationConfig").getString(obj.getString("operationType")));
                    }

                    LOGGER.info("REQUEST FORMED : " + req.encodePrettily());

                    /*Future<JsonObject> respJson = elasticClientQuery(configLocal.getJsonObject("elasticConfig").getString("elasticHost"),
                            configLocal.getJsonObject("elasticConfig").getInteger("elasticPort"),indexName,req);*/

                    WebClient clientWeb = WebClient.create(vertx);
                    clientWeb
                            .get(configLocal.getJsonObject("elasticConfig").getInteger("elasticPort"),
                                    configLocal.getJsonObject("elasticConfig").getString("elasticHost"), indexName+ "/_search?filter_path=aggregations.*")
                            .sendJsonObject(req, resp -> {
                                System.out.println("RESPONSE REST SERVICE BODY : " + resp.result().bodyAsString());
                                if (resp.succeeded()) {
                                    if(resp.result().bodyAsJsonObject().containsKey("aggregations")) {
                                        //respond(response, 200, helper.parseResultJSON(resp.result().bodyAsJsonObject(),field).encode());
                                        if (field.equalsIgnoreCase("NOCONDITION")) {
                                            //respond(response, 200, resp.result().bodyAsJsonObject().getJsonObject("aggregations").encode());
                                            response
                                                    .setStatusCode(200)
                                                    .putHeader("content-type", "application/json")
                                                    .end(resp.result().bodyAsJsonObject().getJsonObject("aggregations").encode());
                                            return;
                                        }
                                        response
                                                .setStatusCode(200)
                                                .putHeader("content-type", "application/json")
                                                .end(helper.parseResultJSON(resp.result().bodyAsJsonObject(), field).encode());
                                    }
                                    else
                                        respond(response, 404, "Result not found for the given Input");
                                }
                                else {
                                    System.out.println("Error Occured!!! " + resp.cause());
                                }

                                if (resp.failed())
                                    System.out.println("Error Occured!!! Failed!!!" + resp.cause());

                            });



                   /* response
                            .setStatusCode(200)
                            .putHeader("content-type", "application/json")
                            .end(helper.parseResultJSON(respJson.result()).encode());*/


                }

            });
        }
        catch (Exception exp)
        {
            exp.printStackTrace();
            respond(response, 503, exp.getMessage());
            return;
        }

    }





    private boolean isAuthorized(String authorization, HttpServerResponse response) {
        if (authorization == null || authorization.isEmpty()) {
            response.putHeader("WWW-Authenticate", "Basic");
            respond(response, 401, "Please use Basic Auth.");
            return false;
        }
        Credential credential;
        try {
            credential = Credential.fromAuthHeader(authorization);
        } catch (UnsupportedOperationException ex) {
            response.putHeader("WWW-Authenticate", "Basic");
            respond(response, 401, "Authorization used is currently not supported.");
            return false;
        }
        if (credential == null) {
            respond(response, 401, "Error reading Authorization Header.");
            return false;
        } else if (!auth.isAuthorized(credential)) {
            respond(response, 401, "Invalid Username or Password.");
            return false;
        }
        return true;
    }

    private void respond(HttpServerResponse response, Integer code, String message) {
        response.setStatusCode(code).end(message);
    }



    @Override
    public void stop() throws Exception {
        LOGGER.info("stopping https server");
        httpServer.close();
        super.stop();
    }

    private void initAuthorizedUsers(JsonArray authorizedUsers) {
        if (authorizedUsers.isEmpty()) {
            LOGGER.warn("No authorized users provided. All incoming requests to the HTTP(S) Server will be rejected.");
        }
        List<JsonObject> users = authorizedUsers.getList();
        users.forEach(user -> {
            String username = user.getString("username");
            String password = user.getString("password");
            if (username == null || username.trim().isEmpty() || password == null || password.isEmpty()) {
                return;
            }
            this.auth.addValidCredential(new UsernamePasswordCredential(username, password));
        });
    }


    private String validateData(JsonObject obj, JsonObject configLocal)
    {
        String retVal = "Success";
        try
        {
            JsonObject  pramObj = obj.getJsonObject("parameter");

            String since = pramObj.getString("since");
            String until = pramObj.getString("until");
            LocalDateTime sincedateTime = LocalDateTime.parse(since, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXX"));
            LocalDateTime untildateTime = LocalDateTime.parse(until, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXX"));

            //System.out.println("After parsing SINCE date : "  + sincedateTime.toString());
            //System.out.println("After parsing UNTIL  date : "  + untildateTime.toString());

            if(!untildateTime.isAfter(sincedateTime) && !untildateTime.equals(sincedateTime)) {
                retVal = "Either SINCE date is greater than UNTIL date (or) BOTH date are not Equal"; //Test the kibana for the equal range
            }

            if(!(pramObj.getJsonArray("fields") != null && pramObj.getJsonArray("fields").size() > 0))
                retVal="Please provide field list for which the value has to be calculated";

            if(obj.getString("eventType") == null)
                retVal="EventType value is missing";

            if(obj.getString("operationType") == null)
                retVal="operationType value is missing";

            if(!configLocal.getJsonObject("senderConfig").containsKey(obj.getString("senderID"))) {
                retVal = " Unauthorized senderID " + obj.getString("senderID") + ", use valid senderID";
            }

            if(!configLocal.getJsonObject("operationConfig").containsKey(obj.getString("operationType"))) {
                //retVal = " Invalid OperationType " + obj.getString("operationType") + ", use valid operationTypes (meanCalculation|totalCalculation|countCalculation)";
                retVal = " Invalid OperationType " + obj.getString("operationType") + ", use valid operationType meanCalculation";
            }


        }
        catch(Exception exp)
        {
            exp.printStackTrace();
        }
        return retVal;
    }

}

