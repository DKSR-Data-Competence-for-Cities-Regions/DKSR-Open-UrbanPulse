
package de.urbanpulse.upservice;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.ResultSet;
import io.vertx.serviceproxy.ServiceBinder;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This class is responsible for querying the UP database for information directly (which we don't
 * want to receive via the regular setup messages).
 *
 * So far, only resolving event types to SIDs is implemented.
 *
 *
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class UPServiceVerticle extends AbstractVerticle
        implements UPService {

    public static final String SERVICE_ADDRESS = "vertx://UrbanPulseService";

    static final String EVENT_TYPE_ID_TO_SIDS_QUERY =
            "SELECT id FROM up_sensors WHERE eventtype_id = ?";
    static final String EVENT_TYPE_NAME_TO_SIDS_QUERY =
            "SELECT sensor.id FROM up_sensors sensor INNER JOIN up_event_types et ON sensor.eventtype_id = et.id WHERE et.name = ?";
    static final String EVENT_TYPE_ID_EXISTS_QUERY = "SELECT count(*) as cnt from up_event_types where id = ?";
    static final String EVENT_TYPE_NAME_EXISTS_QUERY = "SELECT count(*) as cnt from up_event_types where name = ?";
    static final String EVENT_TYPE_NAME_FOR_EVENT_TYPE_ID_QUERY = "SELECT name from up_event_types where id = ?";

    private static final String DATASOURCE_NAME = "UrbanPulse";

    private JDBCClient client;

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        JsonObject jdbcConfig = config().getJsonObject("jdbc");
        client = JDBCClient.createShared(vertx, jdbcConfig, DATASOURCE_NAME);
        new ServiceBinder(vertx).setAddress(SERVICE_ADDRESS).register(UPService.class, this);
        super.start(startPromise);
    }

    @Override
    public void stop(Promise<Void> stopPromise) throws Exception {
        client.close(stopPromise);
    }

    @Override
    public void getSIDsForEventTypeId(String eventTypeId,
            Handler<AsyncResult<List<String>>> resultHandler) {
        queryForStringList(EVENT_TYPE_ID_TO_SIDS_QUERY, new JsonArray().add(eventTypeId), "id", resultHandler);
    }

    @Override
    public void getSIDsForEventTypeName(String eventTypeName,
            Handler<AsyncResult<List<String>>> resultHandler) {
        queryForStringList(EVENT_TYPE_NAME_TO_SIDS_QUERY, new JsonArray().add(eventTypeName), "id", resultHandler);
    }

    @Override
    public void getEventTypeNameForEventTypeId(String eventTypeId, Handler<AsyncResult<String>> resultHandler) {
        queryForStringList(EVENT_TYPE_NAME_FOR_EVENT_TYPE_ID_QUERY, new JsonArray().add(eventTypeId), "name", res -> {
            //if we have more than one event name for the same id something is broken
            if (res.succeeded() && res.result().size() == 1) {
                resultHandler.handle(Future.succeededFuture(res.result().get(0)));
            } else {
                resultHandler.handle(Future.failedFuture(res.cause()));
            }
        });
    }

    @Override
    public void eventTypeIdExists(String eventTypeId, Handler<AsyncResult<Boolean>> resultHandler) {
        queryForSingleInteger(EVENT_TYPE_ID_EXISTS_QUERY, new JsonArray().add(eventTypeId), "cnt", res -> {
            if (res.succeeded()) {
                resultHandler.handle(Future.succeededFuture(res.result() > 0));
            } else {
                resultHandler.handle(Future.failedFuture(res.cause()));
            }
        });
    }

    @Override
    public void eventTypeNameExists(String eventTypeName, Handler<AsyncResult<Boolean>> resultHandler) {
        queryForSingleInteger(EVENT_TYPE_NAME_EXISTS_QUERY, new JsonArray().add(eventTypeName), "cnt", res -> {
            if (res.succeeded()) {
                resultHandler.handle(Future.succeededFuture(res.result() > 0));
            } else {
                resultHandler.handle(Future.failedFuture(res.cause()));
            }
        });
    }

    private void queryForStringList(String query, JsonArray parameters, String columnName,
            Handler<AsyncResult<List<String>>> resultHandler) {
        client.queryWithParams(query, parameters, queryResult -> {
            if (queryResult.succeeded()) {
                ResultSet resultSet = queryResult.result();
                List<String> result = resultSet.getRows().stream().map(row -> row.getString(columnName))
                        .collect(Collectors.toList());
                resultHandler.handle(Future.succeededFuture(result));
            } else {
                resultHandler.handle(Future.failedFuture(queryResult.cause()));
            }
        });
    }

    private void queryForSingleInteger(String query, JsonArray parameters, String columnName, Handler<AsyncResult<Integer>> resultHandler) {
        client.queryWithParams(query, parameters, queryResult -> {
            if (queryResult.succeeded()) {
                ResultSet resultSet = queryResult.result();
                Integer result = resultSet.getRows().stream().map(row -> row.getInteger(columnName)).findAny().get();
                resultHandler.handle(Future.succeededFuture(result));
            } else {
                resultHandler.handle(Future.failedFuture(queryResult.cause()));
            }
        });
    }
}
