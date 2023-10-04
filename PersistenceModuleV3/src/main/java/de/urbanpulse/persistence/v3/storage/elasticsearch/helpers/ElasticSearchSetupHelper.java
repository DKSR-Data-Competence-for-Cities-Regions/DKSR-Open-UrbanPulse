package de.urbanpulse.persistence.v3.storage.elasticsearch.helpers;

import de.urbanpulse.persistence.v3.storage.ElasticSearchSecondLevelStorageServiceImpl;
import de.urbanpulse.persistence.v3.storage.elasticsearch.clients.ElasticSearchRestHighLevelClientWrapper;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import java.util.Arrays;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.client.indices.IndexTemplatesExistRequest;
import org.elasticsearch.client.indices.PutIndexTemplateRequest;
import org.elasticsearch.common.xcontent.XContentType;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class ElasticSearchSetupHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(ElasticSearchSetupHelper.class);

    protected ElasticSearchRestHighLevelClientWrapper client;
    private final String mappingIndexName;
    private final String indexTemplateName;
    private final String upIndexPrefix;

    public ElasticSearchSetupHelper(ElasticSearchRestHighLevelClientWrapper client, String mappingIndexName,
                                    String indexTemplateName, String upIndexPrefix){
        this.client = client;
        this.mappingIndexName = mappingIndexName;
        this.indexTemplateName = indexTemplateName;
        this.upIndexPrefix = upIndexPrefix;

    }

    public Future<Void> checkForAndCreateEventTypeSIDIndex(Context context) {
        Future<Void> voidFuture = Future.future();
        Future<Boolean> indexExistsFuture = client.checkIfIndexExists(new GetIndexRequest(mappingIndexName), context);

        indexExistsFuture.setHandler(hndlr -> {
            if (hndlr.succeeded()) {
                if (Boolean.FALSE.equals(hndlr.result())) { //does the index exist or not
                    LOGGER.info("Mapping index not existing! Creating...");
                    createIndex(mappingIndexName, context).setHandler(createIndexHndlr -> {
                        if (createIndexHndlr.succeeded()) {
                            voidFuture.complete();
                        } else {
                            voidFuture.fail(createIndexHndlr.cause());
                        }
                    });
                } else {
                    voidFuture.complete();
                }
            } else {
                LOGGER.error("Could not check if mapping index exists!", hndlr.cause());
                voidFuture.fail(hndlr.cause());
            }
        });

        return voidFuture;
    }

    public Future<Void> checkForAndCreateIndexTemplate(Context context){
        Future<Void> voidFuture = Future.future();
        Future<Boolean> templateExistsFuture = client.checkIfTemplateAlreadyExists(new IndexTemplatesExistRequest(indexTemplateName),context);

        templateExistsFuture.setHandler(hndlr -> {
            if (hndlr.succeeded()){
                if (Boolean.FALSE.equals(hndlr.result())){ //is the template there or not
                    LOGGER.info("Creating index template...");
                    PutIndexTemplateRequest putIndexTemplateRequest = new PutIndexTemplateRequest(indexTemplateName);
                    putIndexTemplateRequest.mapping(" {\n" +
                            "      \"properties\": {\n" +
                            "        \""+ ElasticSearchSecondLevelStorageServiceImpl.UP_GEO_LOCATION_FIELD+"\": {\n" +
                            "          \"type\": \"geo_point\"\n" +
                            "        }\n" +
                            "      }\n }" , XContentType.JSON);
                    putIndexTemplateRequest.patterns(Arrays.asList(upIndexPrefix + "*"));
                    client.createTemplate(putIndexTemplateRequest,context).setHandler(voidFuture);
                } else {
                    voidFuture.complete();
                }
            } else {
                LOGGER.error("Could not check if template exists!", hndlr.cause());
                voidFuture.fail(hndlr.cause());
            }
        });

        return voidFuture;
    }

    protected Future<Void> createIndex(String indexName, Context context) {
        Future<Void> voidFuture = Future.future();

        client.createIndex(new CreateIndexRequest(indexName), context).setHandler(hndlr -> {
            if (hndlr.succeeded()) {
                voidFuture.complete();
            } else {
                LOGGER.error("Was unable to create index " + indexName, hndlr.cause());
                voidFuture.fail(hndlr.cause());
            }
        });

        return voidFuture;
    }
}
