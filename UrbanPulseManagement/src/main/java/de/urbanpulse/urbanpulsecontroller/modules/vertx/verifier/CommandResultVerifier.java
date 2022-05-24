package de.urbanpulse.urbanpulsecontroller.modules.vertx.verifier;

import io.vertx.core.json.JsonObject;

/**
 *
 * checks whether the result object returned by a vert.x module indicates successful invocation or not
 *
 * in most cases {@link DefaultCommandResultVerifier} can be used, which merely checks for an "error" field, but some cases require
 * a more specialized implementation
 *
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public interface CommandResultVerifier {

    /**
     *
     * @param commandResult the command result to be verified
     * @return whether the commandResult represented a successful invocation of the command
     */
    public boolean verify(JsonObject commandResult);
}
