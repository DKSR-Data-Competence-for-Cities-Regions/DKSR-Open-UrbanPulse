package de.urbanpulse.persistence.v3.storage.elasticsearch.actionlisteners;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.support.master.AcknowledgedResponse;

import io.vertx.core.Context;
import io.vertx.core.Promise;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class PutIndexTemplateAcknowledgeResponseListener implements ActionListener<AcknowledgedResponse> {

    private final Context context;
    private final Promise<Void> voidPromise;

    public PutIndexTemplateAcknowledgeResponseListener(Promise<Void> voidPromise, Context context){
        this.context = context;
        this.voidPromise = voidPromise;
    }

    @Override
    public void onResponse(AcknowledgedResponse acknowledgedResponse) {
        context.runOnContext(hndlr -> {
            if (acknowledgedResponse.isAcknowledged()){
                voidPromise.complete();
            } else {
                voidPromise.fail("Could not create index template!");
            }
        });
    }

    @Override
    public void onFailure(Exception e) {
        context.runOnContext(hndlr -> voidPromise.fail(e));
    }
}
