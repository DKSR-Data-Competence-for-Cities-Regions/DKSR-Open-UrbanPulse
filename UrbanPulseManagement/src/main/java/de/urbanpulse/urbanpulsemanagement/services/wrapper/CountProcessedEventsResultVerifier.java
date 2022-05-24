package de.urbanpulse.urbanpulsemanagement.services.wrapper;

import de.urbanpulse.urbanpulsecontroller.modules.vertx.verifier.CommandResultVerifier;
import io.vertx.core.json.JsonObject;

/**
 * verifier that checks if the commandResult contains a field called "processedEvents"
 *
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class CountProcessedEventsResultVerifier implements CommandResultVerifier {

    @Override
    public boolean verify(JsonObject commandResult) {
        if (commandResult == null) {
            return false;
        }

        if (commandResult.containsKey("body")) {
            return commandResult.getJsonObject("body").containsKey("processedEvents");
        }

        return false;
    }
}
