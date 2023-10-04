package de.urbanpulse.persistence.v3.storage.elasticsearch.clients;

import static de.urbanpulse.persistence.v3.storage.ElasticSearchSecondLevelStorageServiceImpl.INNER_HIT_PROPERTY_NAME;
import java.io.IOException;

import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.core.CountRequest;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.client.indices.IndexTemplatesExistRequest;
import org.elasticsearch.client.indices.PutIndexTemplateRequest;

import de.urbanpulse.persistence.v3.storage.elasticsearch.actionlisteners.*;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import java.util.List;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.core.CountResponse;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class ElasticSearchRestHighLevelClientWrapper {

    protected RestHighLevelClient client;

    public ElasticSearchRestHighLevelClientWrapper(RestHighLevelClient client) {
        this.client = client;
    }

    public void close() throws IOException {
        client.close();
    }

    public RestHighLevelClient getClient() {
        return client;
    }

    public Future<Boolean> checkIfIndexExists(GetIndexRequest getIndexRequest, Context context) {
        Promise<Boolean> indexExistsPromise = Promise.promise();
        CustomBooleanActionListner customBooleanActionListner = new CustomBooleanActionListner(context, indexExistsPromise);
        performClientIndicesExistAsync(getIndexRequest, customBooleanActionListner);
        return indexExistsPromise.future();
    }

    protected void performClientIndicesExistAsync(GetIndexRequest getIndexRequest, CustomBooleanActionListner listener) {
        client.indices().existsAsync(getIndexRequest, RequestOptions.DEFAULT, listener);
    }

    public Future<Void> createIndex(CreateIndexRequest request, Context context) {
        Promise<Void> voidPromise = Promise.promise();
        CreateIndexResponseActionListener actionListener = new CreateIndexResponseActionListener(context, request.index(), voidPromise);
        performClientIndicesCreateAsync(request, actionListener);

        return voidPromise.future();
    }

    protected void performClientIndicesCreateAsync(CreateIndexRequest request, CreateIndexResponseActionListener listener) {
        client.indices().createAsync(request, RequestOptions.DEFAULT, listener);
    }

    public Future<Void> updateAsync(UpdateRequest updateRequest, Context context) {
        Promise<Void> voidPromise = Promise.promise();
        UpdateResponseActionListener actionListener = new UpdateResponseActionListener(context, voidPromise);
        performClientUpdateAsync(updateRequest, actionListener);

        return voidPromise.future();
    }

    protected void performClientUpdateAsync(UpdateRequest request, UpdateResponseActionListener listener) {
        client.updateAsync(request, RequestOptions.DEFAULT, listener);
    }

    public Future<Void> bulkAsync(BulkRequest bulkRequest, Context context) {
        Promise<Void> voidPromise = Promise.promise();
        BulkResponseActionListener actionListener = new BulkResponseActionListener(context, voidPromise);
        performClientBulkAsync(bulkRequest, actionListener);

        return voidPromise.future();
    }

    protected void performClientBulkAsync(BulkRequest request, BulkResponseActionListener listener) {
        client.bulkAsync(request, RequestOptions.DEFAULT, listener);
    }

    public Future<String> searchForEventTypeAsync(SearchRequest searchRequest, Context context) {
        Promise<String> responsePromise = Promise.promise();
        SearchResponseActionListener actionListener = new SearchResponseActionListener(context, responsePromise);
        performClientSearchAsync(searchRequest, actionListener);

        return responsePromise.future();
    }

    protected void performClientSearchAsync(SearchRequest request, ActionListener<SearchResponse> listener) {
        client.searchAsync(request, RequestOptions.DEFAULT, listener);
    }

    public Future<Long> countAsync(CountRequest countRequest, Context context) {
        Promise<Long> responsePromise = Promise.promise();
        CountResponseActionListener actionListener = new CountResponseActionListener(context, responsePromise);
        performClientCountAsync(countRequest, actionListener);

        return responsePromise.future();
    }

    protected void performClientCountAsync(CountRequest request, ActionListener<CountResponse> listener) {
        client.countAsync(request, RequestOptions.DEFAULT, listener);
    }

    public Future<JsonObject> searchAfterAsync(SearchRequest searchRequest, Context context) {
        Promise<JsonObject> responsePromise = Promise.promise();
        SearchAfterSearchResponseActionListener actionListener = new SearchAfterSearchResponseActionListener(context, responsePromise);
        performClientSearchAsync(searchRequest, actionListener);

        return responsePromise.future();
    }

    public Future<Void> createTemplate(PutIndexTemplateRequest putIndexTemplateRequest, Context context) {
        Promise<Void> voidPromise = Promise.promise();
        PutIndexTemplateAcknowledgeResponseListener acknowledgResponseListener = new PutIndexTemplateAcknowledgeResponseListener(voidPromise, context);
        performClientIndicesPutTemplateAsync(putIndexTemplateRequest, acknowledgResponseListener);

        return voidPromise.future();
    }

    protected void performClientIndicesPutTemplateAsync(PutIndexTemplateRequest request, ActionListener<AcknowledgedResponse> listener) {
        client.indices().putTemplateAsync(request, RequestOptions.DEFAULT, listener);
    }

    public Future<Boolean> checkIfTemplateAlreadyExists(IndexTemplatesExistRequest indexTemplatesExistRequest, Context context) {
        Promise<Boolean> templateExistsPromise = Promise.promise();
        CustomBooleanActionListner customBooleanActionListner = new CustomBooleanActionListner(context, templateExistsPromise);
        performClientIndicesExistsTemplateAsync(indexTemplatesExistRequest, customBooleanActionListner);

        return templateExistsPromise.future();
    }

    protected void performClientIndicesExistsTemplateAsync(IndexTemplatesExistRequest request, CustomBooleanActionListner listener) {
        client.indices().existsTemplateAsync(request, RequestOptions.DEFAULT, listener);
    }

    public Future<List<JsonObject>> searchLatestDocumentsAsync(SearchRequest searchRequest, Context context) {
        Promise<List<JsonObject>> responsePromise = Promise.promise();
        InnerHitsActionListener actionListener = new InnerHitsActionListener(context, INNER_HIT_PROPERTY_NAME, responsePromise);
        performClientSearchAsync(searchRequest, actionListener);
        return responsePromise.future();
    }
}
