package de.urbanpulse.persistence.v3.storage.elasticsearch.actionlisteners;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.update.UpdateResponse;

import io.vertx.core.Context;
import io.vertx.core.Promise;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class UpdateResponseActionListener implements ActionListener<UpdateResponse> {

    private Context context;
    private Promise<Void> voidPromise;

    public UpdateResponseActionListener(Context context, Promise<Void> voidPromise) {

        this.context = context;
        this.voidPromise = voidPromise;
    }

    @Override
    public void onResponse(UpdateResponse updateResponse) {
        context.runOnContext(contextHndlr -> {
            if (updateResponse.status().getStatus() < 200 || updateResponse.status().getStatus() >= 400) {
                voidPromise.fail("Was unable to save document, received status code " + updateResponse.status().getStatus());
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
