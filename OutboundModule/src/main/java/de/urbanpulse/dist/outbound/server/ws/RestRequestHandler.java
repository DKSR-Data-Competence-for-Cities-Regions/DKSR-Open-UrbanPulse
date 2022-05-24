package de.urbanpulse.dist.outbound.server.ws;

import de.urbanpulse.dist.util.UpdateListenerConfig;
import de.urbanpulse.util.AccessLogger;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class RestRequestHandler {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss zzz");
    private static final Logger LOGGER = LoggerFactory.getLogger(RestRequestHandler.class);
    private static final AccessLogger ACCESS_LOGGER = AccessLogger.newInstance(RestRequestHandler.class);
    private static final int ONE_WEEK = 60 * 60 * 24 * 7;

    private final WsServerTargetMatcher matcher;
    private final URI restBaseUrl;
    private final Map<String, Set<UpdateListenerConfig>> statementToListenerMap;

    public RestRequestHandler(WsServerTargetMatcher matcher,
            URI restBaseUrl,
            Map<String, Set<UpdateListenerConfig>> statementToListenerMap) {
        this.matcher = matcher;
        this.restBaseUrl = restBaseUrl;
        this.statementToListenerMap = statementToListenerMap;
    }

    public void handle(RoutingContext routingContext) {
        HttpServerRequest request = routingContext.request();
        HttpServerResponse response = request.response();
        try {

            String requestPath = request.path();
            HttpMethod method = request.method();

            ACCESS_LOGGER.log(request);

            if (!method.equals(HttpMethod.GET)) {
                response.setStatusCode(405).end("405 METHOD_NOT_ALLOWED");
            } else {
                String statement = matcher.extractStatement(requestPath, restBaseUrl.getPath());
                boolean haveMatchingTargetForStatement = (statement != null) && this.statementToListenerMap.containsKey(statement);
                if (haveMatchingTargetForStatement) {
                    String filename = calculateFilename(request);
                    Buffer responseBody = readStaticFileFromResource(filename);
                    long datetime = getClass().getClassLoader().getResource(filename).openConnection().getLastModified();
                    ZonedDateTime lastModifiedDate = ZonedDateTime.ofInstant(Instant.ofEpochMilli(datetime), ZoneId.of("UTC"));
                    String cacheControl = "private, max-age=" + ONE_WEEK;
                    response.putHeader("Cache-Control", cacheControl);
                    response.putHeader("Last-Modified", FMT.format(lastModifiedDate));
                    response.setStatusCode(200);
                    response.end(responseBody);
                } else {
                    response.setStatusCode(404).end("404 NOT_FOUND");
                }
            }
        } catch (Exception ex) {
            LOGGER.error("can't handle request {0}.", ex, request.absoluteURI());
            response.setStatusCode(500).end("500 INTERNAL_SERVER_ERROR");
        }
    }

    private String calculateFilename(HttpServerRequest request) {
        String filename = "ws.html";
        String acceptLanguage = request.getHeader("Accept-Language");
        if ((acceptLanguage != null) && !acceptLanguage.trim().isEmpty()) {
            Locale.LanguageRange selectedLanguageRange = new Locale.LanguageRange("en", 0.0);
            for (Locale.LanguageRange range : Locale.LanguageRange.parse(acceptLanguage)) {
                if (range.getWeight() > selectedLanguageRange.getWeight()) {
                    selectedLanguageRange = range;
                }
            }
            if (selectedLanguageRange.getRange().startsWith("de")) {
                filename = "ws_de.html";
            }
        }
        return filename;
    }

    private Buffer readStaticFileFromResource(String filename) {
        InputStream fileIn = getClass().getClassLoader().getResourceAsStream(filename);
        Buffer b = Buffer.buffer();
        BufferedReader br = new BufferedReader(new InputStreamReader(fileIn));
        br.lines().forEach(b::appendString);
        return b;
    }

}
