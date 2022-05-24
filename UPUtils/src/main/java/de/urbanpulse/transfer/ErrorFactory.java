package de.urbanpulse.transfer;

import io.vertx.core.json.JsonObject;

import static de.urbanpulse.transfer.TransferStructureFactory.TAG_BODY;
import static de.urbanpulse.transfer.TransferStructureFactory.TAG_HEADER;

/**
 * generate Json objects with certain error types
 *
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class ErrorFactory {

    public enum ErrorCode {

        INVALID_HEADER(ErrorFactory.INVALID_HEADER), //Errorcode == 0
        INVALID_MESSAGE(ErrorFactory.INVALID_MESSAGE),
        REPLY_TIMEOUT(ErrorFactory.REPLY_TIMEOUT),
        REPLY_NO_HANDLERS(ErrorFactory.REPLY_NO_HANDLERS),
        REPLY_RECIPIENT_FAILURE(ErrorFactory.REPLY_RECIPIENT_FAILURE),
        COMMAND_NOT_EXECUTED(ErrorFactory.COMMAND_NOT_EXECUTED),
        UNKNOWN_ERROR(ErrorFactory.UNKNOWN_ERROR); // Error code == 6

        private final String errorMessage;

        public String getErrorMessage() {
            return errorMessage;
        }

        ErrorCode(String errorMessage) {
            this.errorMessage = errorMessage;
        }

        public static ErrorCode fromErrorNumber(Integer number) {
            try {
                return ErrorCode.values()[number];
            } catch (ArrayIndexOutOfBoundsException ex) {
                return null;
            }
        }
    }

    public final static String ERROR_MESSAGE_TAG = "error";
    public final static String ERROR_CODE_TAG = "responseCode";
    public final static String ERROR_ORGINAL_MESSAGE = "originalMessage";
    public final static String COMMAND_NOT_EXECUTED = "command not executed";

    final static String INVALID_HEADER = "invalid header";
    final static String INVALID_MESSAGE = "invalid message";
    final static String REPLY_TIMEOUT = "reply timeout";
    final static String REPLY_NO_HANDLERS = "reply no handler";
    final static String REPLY_RECIPIENT_FAILURE = "reply recipient failure";
    final static String UNKNOWN_ERROR = "unknown error";

    public JsonObject createErrorMessage(String errorMsg) {
        ErrorCode errorCode;
        switch (errorMsg) {
            case INVALID_HEADER:
                errorCode = ErrorCode.INVALID_HEADER;
                break;
            case INVALID_MESSAGE:
                errorCode = ErrorCode.INVALID_MESSAGE;
                break;
            case REPLY_TIMEOUT:
                errorCode = ErrorCode.REPLY_TIMEOUT;
                break;
            case REPLY_NO_HANDLERS:
                errorCode = ErrorCode.REPLY_NO_HANDLERS;
                break;
            case REPLY_RECIPIENT_FAILURE:
                errorCode = ErrorCode.REPLY_RECIPIENT_FAILURE;
                break;
            case UNKNOWN_ERROR:
            default:
                errorCode = ErrorCode.UNKNOWN_ERROR;
                break;
        }

        JsonObject header = new JsonObject();
        header.put(ERROR_CODE_TAG, errorCode.ordinal());
        JsonObject body = new JsonObject();
        body.put(ERROR_MESSAGE_TAG, errorMsg);
        JsonObject errorObj = new JsonObject();
        errorObj.put(TAG_HEADER, header);
        errorObj.put(TAG_BODY, body);
        return errorObj;
    }

    public JsonObject createReplyNoHandler() {
        return createErrorMessage(REPLY_NO_HANDLERS);
    }

    public JsonObject createReplyRecipientFailure() {
        return createErrorMessage(REPLY_RECIPIENT_FAILURE);
    }

    public JsonObject createReplyTimeout() {
        return createErrorMessage(REPLY_TIMEOUT);
    }

    public JsonObject createHeaderError() {
        return createErrorMessage(INVALID_HEADER);
    }

    public JsonObject createBodyError() {
        return createErrorMessage(INVALID_MESSAGE);
    }

    public static boolean isHeaderError(JsonObject message) {
        if (!message.containsKey(TAG_HEADER)) {
            return false;
        }
        JsonObject header = message.getJsonObject(TAG_HEADER);
        if (!header.containsKey(ERROR_CODE_TAG)) {
            return false;
        }
        int errorCode = header.getInteger(ERROR_CODE_TAG);
        return errorCode == ErrorCode.INVALID_HEADER.ordinal();
    }
}
