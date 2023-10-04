package de.urbanpulse.persistence.v3.storage.elasticsearch.actionlisteners;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.index.IndexResponse;

import io.vertx.core.Context;
import io.vertx.core.Promise;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class IndexResponseActionListener implements ActionListener<IndexResponse> {

    private Context context;
    private Promise<Void> voidPromise;

    public IndexResponseActionListener(Context context, Promise<Void> voidPromise) {
        this.context = context;
        this.voidPromise = voidPromise;
    }

    @Override
    public void onResponse(IndexResponse indexResponse) {
        context.runOnContext(hndlr -> {
            if (indexResponse.status().getStatus() < 200 || indexResponse.status().getStatus() >= 400) {
                voidPromise.fail("Something went wrong saving a document in ElasticSearch, received status "
                        + indexResponse.status().getStatus());
            } else {
                voidPromise.complete();
            }
        });
    }

    @Override
    public void onFailure(Exception e) {
        context.runOnContext(hndlr -> voidPromise.fail(e));
    }
}
