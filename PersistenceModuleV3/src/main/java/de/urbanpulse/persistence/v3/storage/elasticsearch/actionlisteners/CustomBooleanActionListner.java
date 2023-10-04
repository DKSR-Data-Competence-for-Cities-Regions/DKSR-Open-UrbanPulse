package de.urbanpulse.persistence.v3.storage.elasticsearch.actionlisteners;

import org.elasticsearch.action.ActionListener;

import io.vertx.core.Context;
import io.vertx.core.Promise;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class CustomBooleanActionListner implements ActionListener<Boolean> {
    private Context context;
    private Promise<Boolean> booleanPromise;


    public CustomBooleanActionListner(Context context, Promise<Boolean> booleanPromise) {
        this.context = context;
        this.booleanPromise = booleanPromise;
    }

    @Override
    public void onResponse(Boolean aBoolean) {
        context.runOnContext(hndlr -> booleanPromise.complete(aBoolean));
    }

    @Override
    public void onFailure(Exception e) {
        context.runOnContext(hndlr -> booleanPromise.fail(e));
    }
}
