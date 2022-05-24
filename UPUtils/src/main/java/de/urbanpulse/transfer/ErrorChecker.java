package de.urbanpulse.transfer;

import io.vertx.core.json.JsonObject;

import static de.urbanpulse.transfer.TransferStructureFactory.TAG_BODY;
import static de.urbanpulse.transfer.TransferStructureFactory.TAG_HEADER;


/**
 * checks the response Json received from a command invocation for certain error types
 *
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class ErrorChecker {

    public boolean isConnectionError(JsonObject msg) {
        if (msg.getJsonObject(TAG_BODY) == null) {
            return false;
        }
        JsonObject body = msg.getJsonObject(TAG_BODY);
        String errorMsg = body.getString(ErrorFactory.ERROR_MESSAGE_TAG);

        if (errorMsg == null) {
            return false;
        }
        switch (errorMsg) {
            case ErrorFactory.INVALID_HEADER:
            case ErrorFactory.INVALID_MESSAGE:
            case ErrorFactory.REPLY_TIMEOUT:
            case ErrorFactory.REPLY_NO_HANDLERS:
            case ErrorFactory.REPLY_RECIPIENT_FAILURE:
                return true;
        }
        return false;
    }

    public boolean isError(JsonObject msg) {
        return (msg.getJsonObject(TAG_HEADER) != null && msg.getJsonObject(TAG_HEADER).getInteger(ErrorFactory.ERROR_CODE_TAG) != null);
    }

    public ErrorFactory.ErrorCode getConnectionErrorCode(JsonObject msg) {
        Integer errorNumber = msg.getJsonObject(TAG_HEADER).getInteger(ErrorFactory.ERROR_CODE_TAG);
        return ErrorFactory.ErrorCode.fromErrorNumber(errorNumber);
    }
}
