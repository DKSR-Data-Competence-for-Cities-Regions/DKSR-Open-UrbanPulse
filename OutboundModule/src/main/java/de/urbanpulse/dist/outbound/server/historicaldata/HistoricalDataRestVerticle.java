package de.urbanpulse.dist.outbound.server.historicaldata;

import de.urbanpulse.upservice.UPService;
import de.urbanpulse.upservice.UPServiceVerticle;
import de.urbanpulse.util.server.HttpServerFactory;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import city.ui.shared.commons.time.UPDateTimeFormat;
import de.urbanpulse.dist.outbound.server.auth.ShiroAuthHandler;
import de.urbanpulse.outbound.PersistenceQueryServiceProxyFactory;
import de.urbanpulse.outbound.PersistenceQueryServiceVertxEBProxy;
import de.urbanpulse.outbound.QueryConfig;
import io.vertx.core.*;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.serviceproxy.ServiceProxyBuilder;

/**
 * Verticle providing the REST interface for accessing historical data.
 * <p>
 * Internally uses the generated {@link PersistenceQueryServiceVertxEBProxy} to query the
 * PersistenceModuleV3.
 * <p>
 *
 * In the historicalDataRestConfig we have "rules" and "role_to_rules" configurations The rules
 * defines the default filters on the historic data for example: how big time interval of the data
 * can be accessed by a user Withe the role_to_rules configuration we can assign role to these
 * rules. We will use always the default rule a role doesn't has any assigned rule.
 *
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class HistoricalDataRestVerticle extends AbstractVerticle {

    private static final String SEND_NEXT_BATCH = "sendNextBatch";
    private static final String SEND_DONE = "done";

    private static final String HELP_TEXT_FILE = "historic_help_text.html";

    private final CSVRowWriter csvRowWriter = new CSVRowWriter();

    private static final Logger LOGGER =
            LoggerFactory.getLogger(HistoricalDataRestVerticle.class.getName());

    private final ResponseDefFactory responseDefFactory = new ResponseDefFactory();
    private Map<String, MessageConsumer<JsonObject>> openConsumers;
    private PersistenceQueryServiceVertxEBProxy persistenceQueryProxy;

    private UPService upService;

    private String helpText;

    private AuthChecker authChecker;

    @Override
    public void start(Promise<Void> startPromise) {
        String persistenceQueryAddress =
                config().getString("persistenceQueryAddress", "thePersistenceQuery");
        persistenceQueryProxy =
                PersistenceQueryServiceProxyFactory.createProxy(vertx, persistenceQueryAddress);
        openConsumers = new HashMap<>();

        upService = new ServiceProxyBuilder(vertx).setAddress(UPServiceVerticle.SERVICE_ADDRESS)
                .build(UPService.class);

        createAuthChecker();

        vertx.fileSystem().readFile(HELP_TEXT_FILE, fileResult -> {
            if (fileResult.succeeded()) {
                helpText = fileResult.result().toString();

                registerUnregistrationConsumer();

                Router router = Router.router(vertx);
                registerCorsHandler(router);
                setupSecuredRouter(router);
                registerApiRouteAndHandler(router);
                createHttpServer(router, startPromise);
            } else {
                startPromise.fail(fileResult.cause());
            }
        });
    }

    private void registerCorsHandler(Router router) {
        CorsHandler corsHandler = CorsHandler.create("*");
        corsHandler.allowedMethod(HttpMethod.GET).allowedMethod(HttpMethod.OPTIONS);
        corsHandler.allowedHeader("Content-Type").allowedHeader("Accept").allowedHeader("Authorization");
        router.route().handler(corsHandler);
    }

    private void createAuthChecker() {
        JsonObject rulesConfigurations = config().getJsonObject("rules");
        JsonObject rolesToRulesConfigurations = config().getJsonObject("roles_to_rules");
        authChecker = new AuthChecker(rulesConfigurations, rolesToRulesConfigurations);
    }

    private Router setupSecuredRouter(Router router) {
        ShiroAuthHandler authenticator = new ShiroAuthHandler(vertx);
        router.route("/UrbanPulseData/*").handler(authenticator::authenticate);
        return router;
    }

    private void registerUnregistrationConsumer() {
        vertx.eventBus().consumer("unregisterConsumer", (Message<String> event) -> {
            String responseAddress = event.body();

            MessageConsumer<JsonObject> responseConsumerToClose =
                    openConsumers.get(responseAddress);
            if (responseConsumerToClose != null) {
                responseConsumerToClose.unregister(res -> {
                    if (res.succeeded()) {
                        openConsumers.remove(responseAddress);
                    } else {
                        LOGGER.warn("Unregistration of response consumer failed!");
                    }
                });
            }
        });
    }

    private void registerApiRouteAndHandler(Router router) {
        router.get("/UrbanPulseData/historic/sensordata")
                .handler((RoutingContext routingContext) -> {
                    LOGGER.info("Handling incoming API request...");

                    HttpServerRequest req = routingContext.request();

                    String acceptedContentType = req.getHeader("Accept");
                    OutputFormat outputFormat;
                    if (OutputFormat.CSV.matches(acceptedContentType)) {
                        outputFormat = OutputFormat.CSV;
                    } else {
                        outputFormat = OutputFormat.JSON;
                    }

                    HttpServerResponse response = routingContext.response();

                    User user = routingContext.user();
                    String username = user.principal().getString("username");

                    authChecker.checkRole(user)
                            .onComplete(filterSelection -> respond(req, filterSelection.result(), username, response, outputFormat, routingContext));
                });
    }

    private void respond(HttpServerRequest request,
            JsonObject roleConfiguration, String username, HttpServerResponse response,
            OutputFormat outputFormat, RoutingContext routingContext) {

        boolean logAPIUsage = roleConfiguration.getBoolean("logAPIUsage", true);
        if (logAPIUsage) {
            LOGGER.info("\n=== Logging request for user with logging enabled ==="
                            + "\nUsername: {0}\nTime: {1}\nParameters: {2}\n"
                            + "Client IP address: {3}" + "\n=== ===",
                    username, ZonedDateTime.now().format(UPDateTimeFormat.getFormatterWithZoneZ()),
                    request.params(), request.remoteAddress());
        }

        if (hasMultipleOrNoSensorIdentifyingParameters(request)) {
            writeResponseDef(response, responseDefFactory
                    .createBadRequestResponseObject(helpText));
            return;
        }

        Set<String> includeOnly = null;
        if (request.params().contains("includeOnly")) {
            String includeOnlyCommaSeparated = request.getParam("includeOnly");
            String[] fields = includeOnlyCommaSeparated.split(",");
            includeOnly = new HashSet<>(Arrays.asList(fields));
        }

        String since = request.getParam("since");
        String until = request.getParam("until");
        String sid = request.getParam("SID");
        String eventTypeId = request.getParam("eventtype");
        String eventTypeName = request.getParam("eventtypeName");
        Params params = new Params().setIncludeOnly(includeOnly).setRoleConfiguration(roleConfiguration)
                .setSince(since).setUntil(until).setResponse(response).setOutputFormat(outputFormat)
                .setRoutingContext(routingContext);
        if (eventTypeId != null) {
            queryByEventTypeId(eventTypeId, params);
        } else if (eventTypeName != null) {
            queryByEventTypeName(eventTypeName, params);
        } else {

            //First check, if the user has permission for the requested SID
            authChecker.filterPermittedSIDs(Collections.singletonList(sid), params.routingContext, result -> {
                if (result.succeeded()) {
                    List<String> permittedSIDs = result.result();
                    // Note: Can be empty. In this case, persistence will return an empty array
                    queryBySids(permittedSIDs, params);
                } else {
                    writeResponseDef(params.response, responseDefFactory.createNoPermissionForSidResponseObject(result.cause().getMessage()));
                }
            });
        }
    }

    private boolean hasMultipleOrNoSensorIdentifyingParameters(HttpServerRequest request) {
        return Stream.of("SID", "eventType", "eventTypeName")
                .filter(param -> request.getParam(param) != null)
                .count() != 1;
    }

    private void queryByEventTypeId(String eventTypeId, Params params) {
        upService.eventTypeIdExists(eventTypeId, exists -> {
            if (exists.succeeded() && exists.result()) {
                upService.getEventTypeNameForEventTypeId(eventTypeId, eventTypeNameRes -> {
                    if (eventTypeNameRes.succeeded()) {
                        upService.getSIDsForEventTypeId(eventTypeId,
                                res -> handleQueryByEventTypeResult(res, params, eventTypeNameRes.result()));
                    } else {
                        writeResponseDef(params.response, responseDefFactory.createNotFoundResponseObject("Unknown event type: " + eventTypeId));
                    }
                });
            } else if (exists.succeeded() && !exists.result()) {
                writeResponseDef(params.response, responseDefFactory.createNotFoundResponseObject("Unknown event type: " + eventTypeId));
            } else {
                writeResponseDef(params.response, responseDefFactory.createInternalServerErrorResponseObject("Unable to query event type: " + eventTypeId));
            }
        });
    }

    private void queryByEventTypeName(String eventTypeName, Params params) {
        upService.eventTypeNameExists(eventTypeName, exists -> {
            if (exists.succeeded() && exists.result()) {
                upService.getSIDsForEventTypeName(eventTypeName,
                        res -> handleQueryByEventTypeResult(res, params, eventTypeName));
            } else if (exists.succeeded() && !exists.result()) {
                writeResponseDef(params.response, responseDefFactory.createNotFoundResponseObject("Unknown event type: " + eventTypeName));
            } else {
                writeResponseDef(params.response, responseDefFactory.createInternalServerErrorResponseObject("Unable to query event type: " + eventTypeName));
            }
        });
    }

    private void handleQueryByEventTypeResult(AsyncResult<List<String>> res, Params params, String eventtype) {
        if (res.succeeded()) {
            List<String> requestedSIDs = res.result();
            authChecker.filterPermittedSIDsAndEventType(requestedSIDs, params.routingContext, eventtype, result -> {
                if (result.succeeded()) {
                    List<String> permittedSIDs = result.result();
                    //Can be empty. In this case, persistence will return an empty array
                    queryBySids(permittedSIDs, params);
                } else {
                    writeResponseDef(params.response, responseDefFactory.createInternalServerErrorResponseObject(res.cause().getMessage()));
                }
            });
        } else {
            writeResponseDef(params.response, responseDefFactory
                    .createInternalServerErrorResponseObject(res.cause().getMessage()));
        }
    }

    private void queryBySids(List<String> requestedSIDs, Params params) {
        QueryFiltering queryFiltering = new QueryFiltering(params.roleConfiguration, requestedSIDs);
        List<String> validSIDs = queryFiltering.filter(params.since, params.until);
        if (validSIDs.isEmpty() && !requestedSIDs.isEmpty()) {
            handleInvalidRequest(params.response);
        } else {
            // Request may be empty - in this case, it's up to the persistence to return an empty result
            LOGGER.info("request valid, processing query");
            String uniqueRequestHandle = createHandlerAndRegisterResponseConsumer(params.response,
                    params.outputFormat, params.includeOnly, queryFiltering);
            LOGGER.info("Created unique request handle id: " + uniqueRequestHandle);
            triggerPersistenceQuery(requestedSIDs, params, uniqueRequestHandle);
        }
    }

    private String createHandlerAndRegisterResponseConsumer(HttpServerResponse response,
                                                            OutputFormat outputFormat, Set<String> includeOnly, QueryFiltering queryFiltering) {
        String uniqueRequestHandle = UUID.randomUUID().toString();
        registerResponseConsumer(response, uniqueRequestHandle, queryFiltering, outputFormat,
                includeOnly);
        return uniqueRequestHandle;
    }

    private void handleInvalidRequest(HttpServerResponse response) {
        LOGGER.info("since/until invalid or out of range limitation");
        writeResponseDef(response,
                responseDefFactory.createBadRequestResponseObject(helpText));
    }

    private void handleEventQueryResult(String uniqueRequestHandle, HttpServerResponse response,
                                        AsyncResult<Void> queryStartResult, OutputFormat outputFormat) {
        if (queryStartResult.failed()) {
            LOGGER.error("Query to persistence failed.", queryStartResult.cause());
            writeResponseDef(response,
                    responseDefFactory.createPersistenceNotAvailableResponseObject());
            openConsumers.get(uniqueRequestHandle).unregister();
            openConsumers.remove(uniqueRequestHandle);
        } else {
            response.setChunked(true);
            response.putHeader("content-type", outputFormat.getContentType());
            response.setStatusCode(200);

            if (OutputFormat.JSON.equals(outputFormat)) {
                response.write("{\"sensordata\":[");
            }
        }
    }

    private void triggerPersistenceQuery(List<String> sids, Params params,
                                         String uniqueRequestHandle) {
        QueryConfig queryConfig =
                new QueryConfig().setSids(sids).setSince(params.since).setUntil(params.until);

        persistenceQueryProxy.query(queryConfig, uniqueRequestHandle,
                queryStartResult -> handleEventQueryResult(uniqueRequestHandle, params.response,
                        queryStartResult, params.outputFormat));
    }

    private void registerResponseConsumer(HttpServerResponse response, String uniqueRequestHandle,
                                          QueryFiltering queryFiltering, OutputFormat outputFormat, Set<String> includeOnly) {
        AtomicBoolean isFirst = new AtomicBoolean(true);
        CSVHeaderSet headerFields = new CSVHeaderSet();

        MessageConsumer<JsonObject> responseConsumer =
                vertx.eventBus().consumer(uniqueRequestHandle, (Message<JsonObject> msg) -> {
                    JsonObject body = msg.body();
                    if (body.containsKey("abortingException")) {
                        LOGGER.error("persistence query aborted with exception: "
                                + body.getString("abortingException"));
                        // query aborted with error
                        openConsumers.get(uniqueRequestHandle).unregister();
                        openConsumers.remove(uniqueRequestHandle);
                        response.end();
                    } else if (body.getBoolean("isLast", Boolean.FALSE)) {
                        LOGGER.info("query successfully completed for : " + uniqueRequestHandle);
                        openConsumers.get(uniqueRequestHandle).unregister();
                        openConsumers.remove(uniqueRequestHandle);

                        writeBatch(body, queryFiltering, response, isFirst, outputFormat,
                                headerFields, includeOnly);
                        msg.reply(SEND_DONE);
                        if (OutputFormat.JSON.equals(outputFormat)) {
                            response.end("]}");
                        } else {
                            response.end();
                        }
                    } else {
                        writeBatch(body, queryFiltering, response, isFirst, outputFormat,
                                headerFields, includeOnly);
                        LOGGER.info("Requesting next batch for : " + uniqueRequestHandle);
                        if (!response.writeQueueFull()) {
                            msg.reply(SEND_NEXT_BATCH);
                        } else {
                            response.drainHandler(h -> {
                                LOGGER.info("Drain handler called for : " + uniqueRequestHandle);
                                msg.reply(SEND_NEXT_BATCH);
                            });
                        }
                    }
                });
        openConsumers.put(uniqueRequestHandle, responseConsumer);
    }

    private void writeBatch(JsonObject body, QueryFiltering queryFiltering,
                            HttpServerResponse response, AtomicBoolean isFirst, OutputFormat outputFormat,
                            CSVHeaderSet headerFields, Set<String> includeOnly) {
        JsonArray batch = body.getJsonArray("batch", new JsonArray());
        for (Object obj : batch) {
            JsonObject event = (JsonObject) obj;
            queryFiltering.applyEventFilter(event, includeOnly);

            if (OutputFormat.CSV.equals(outputFormat)) {
                csvRowWriter.writeCsvEvent(isFirst, response, event, headerFields);
            } else {
                writeJsonEvent(isFirst, response, event);
            }
        }
    }

    private void writeJsonEvent(AtomicBoolean isFirst, HttpServerResponse response,
                                JsonObject event) {
        if (!isFirst.compareAndSet(true, false)) {
            response.write(",");
        }

        response.write(event.encode());
    }

    private void writeResponseDef(HttpServerResponse response, JsonObject responseDef) {
        int statusCode = responseDef.getInteger("statusCode");
        String contentType = responseDef.getString("contentType");
        String body = responseDef.getString("body");

        response.putHeader("content-type", contentType).setStatusCode(statusCode).end(body);
    }

    private void createHttpServer(Router router, Promise<Void> startPromise) {
        LOGGER.info("Creating HttpServer...");
        HttpServerFactory.createHttpServer(vertx, config()).requestHandler(router)
                .listen(config().getInteger("port", 4443), result -> {
                    if (result.succeeded()) {
                        startPromise.complete();
                    } else {
                        startPromise.fail(result.cause());
                    }
                });
    }

}
