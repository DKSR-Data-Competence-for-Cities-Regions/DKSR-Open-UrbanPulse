package de.urbanpulse.urbanpulsecontroller.modules.vertx.verifier;

import io.vertx.core.json.JsonObject;

/**
 * simple verifier that merely checks for the absence of a field named "error" in the body
 *
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class DefaultCommandResultVerifier implements CommandResultVerifier {

    @Override
    public boolean verify(JsonObject commandResult) {
        return !commandResult.getJsonObject("body").containsKey("error");
    }
}
