package de.urbanpulse.persistence.v3.storage.elasticsearch.actionlisteners;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.client.indices.CreateIndexResponse;

import io.vertx.core.Context;
import io.vertx.core.Promise;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class CreateIndexResponseActionListener implements ActionListener<CreateIndexResponse> {

    private Context context;
    private String indexName;
    private Promise<Void> voidPromise;

    public CreateIndexResponseActionListener(Context context, String indexName, Promise<Void> voidPromise) {
        this.context = context;
        this.indexName = indexName;
        this.voidPromise = voidPromise;
    }

    @Override
    public void onResponse(CreateIndexResponse createIndexResponse) {
        context.runOnContext(hndlr -> {
            if (createIndexResponse.index().equals(indexName)) {
                voidPromise.complete();
            } else {
                voidPromise.fail("Something went wrong when creating the index " + indexName);
            }
        });
    }

    @Override
    public void onFailure(Exception e) {
        context.runOnContext(hndlr -> voidPromise.fail(e));
    }
}
