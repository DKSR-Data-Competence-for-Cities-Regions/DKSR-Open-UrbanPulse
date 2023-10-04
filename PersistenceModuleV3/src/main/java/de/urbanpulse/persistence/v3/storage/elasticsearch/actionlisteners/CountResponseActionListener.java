package de.urbanpulse.persistence.v3.storage.elasticsearch.actionlisteners;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.client.core.CountResponse;

import io.vertx.core.Context;
import io.vertx.core.Promise;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class CountResponseActionListener implements ActionListener<CountResponse> {

    private Context context;
    private Promise<Long> longPromise;

    public CountResponseActionListener(Context context, Promise<Long> longPromise) {
        this.context = context;
        this.longPromise = longPromise;
    }

    @Override
    public void onResponse(CountResponse countResponse) {
        context.runOnContext(hndlr -> {
            if (countResponse.status().getStatus() == 200) {
                if (countResponse.getCount() > 0) {
                    longPromise.complete(countResponse.getCount());
                } else {
                    longPromise.fail("No count!");
                }
            } else {
                longPromise.fail("Was not able to get count of documents! Server responded with status "
                        + countResponse.status().getStatus());
            }
        });
    }

    @Override
    public void onFailure(Exception e) {
        context.runOnContext(hndlr -> longPromise.fail(e));
    }
}
