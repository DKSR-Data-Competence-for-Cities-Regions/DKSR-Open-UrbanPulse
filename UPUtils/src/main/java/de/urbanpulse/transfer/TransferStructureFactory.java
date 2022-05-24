package de.urbanpulse.transfer;

import de.urbanpulse.util.status.UPModuleState;
import io.vertx.core.json.JsonObject;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * creates Json structure used by the transport layer protocol for
 * sending/receiving messages
 *
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class TransferStructureFactory {

    private static final Logger LOG = java.util.logging.Logger.getLogger(TransferStructureFactory.class.getName());

    public static final String TAG_HEADER = "header";
    public static final String TAG_HEADER_SENDERID = "senderId";
    public static final String TAG_HEADER_RECEIVERID = "receiverId";
    public static final String TAG_HEADER_STATE = "state";
    public static final String TAG_BODY = "body";
    public static final String TAG_BODY_METHOD = "method";
    public static final String TAG_BODY_ARGS = "args";
    public static final String COMMAND_HEARTBEAT = "heartbeat";
    public static final String COMMAND_RESET_CONNECTION = "resetConnection";
    public static final String COMMAND_EXIT_PROCESS = "exitProcess";

    /**
     * @param senderId vert.x address of the sender
     * @param receiverId vert.x address of the receiver
     * @return json-object of the form:
     * <pre>
     * {
     *   "senderId": &lt;senderId&gt;,
     *   "messageSN": &lt;nextSerialNumberForReceiver&gt;
     * }
     * </pre>
     */
    private JsonObject createHeader(String senderId, String receiverId) {
        return new JsonObject()
                .put(TAG_HEADER_SENDERID, senderId)
                .put(TAG_HEADER_RECEIVERID, receiverId);
    }

    /**
     * @param method the command method to invoke
     * @param args arguments for the method
     * @return json-object of the form:
     * <pre>
     * {
     *   "method": &lt;method&gt;,
     *   "args"  : &lt;args&gt;
     * }
     * </pre>
     */
    private JsonObject createBody(String method, Map<String, Object> args) {
        return new JsonObject().put(TAG_BODY_METHOD, method).put(TAG_BODY_ARGS, new JsonObject(args));
    }

    /**
     * @param senderId the id of the sender this heartbeat belongs to
     * @param state the self-desired state of the sender
     * @return body containing heartbeat info:
     * <pre>
     * {
     *   "heartbeat": &lt;senderId&gt;
     * }
     * </pre>
     */
    public JsonObject createHeartbeat(String senderId, UPModuleState state) {
        return new JsonObject().put(COMMAND_HEARTBEAT, new JsonObject().put(TAG_HEADER_SENDERID, senderId).put(TAG_HEADER_STATE, state));
    }

    public JsonObject createResetConnection(String senderId) {
        return new JsonObject().put(COMMAND_RESET_CONNECTION, senderId);
    }

    public JsonObject createExitProcess(String senderId) {
        return new JsonObject().put(COMMAND_EXIT_PROCESS, senderId);
    }

    /**
     * @param senderId the id of the sender this heartbeat belongs to
     * @return body containing heartbeat command
     */
    public JsonObject createExitProcessCommand(String senderId) {
        Map<String, Object> args = new HashMap<>();
        args.put("id", senderId);
        return createBody(COMMAND_EXIT_PROCESS, args);
    }

    /**
     * @param body the body of the heartbeat command, containing the id of the sender this heartbeat belongs to (as "senderId") and
     * the state of the module (as "state")
     * @return body containing heartbeat command
     */
    public JsonObject createHeartbeatCommand(JsonObject body) {
        Map<String, Object> args = new HashMap<>();
        args.put("id", body.getString(TAG_HEADER_SENDERID));
        args.put(TAG_HEADER_STATE, body.getString(TAG_HEADER_STATE));
        return createBody(COMMAND_HEARTBEAT, args);
    }

    /**
     * @param senderId the id of the sender this heartbeat belongs to
     * @return body containing resetConnection command
     */
    public JsonObject createResetConnectionCommand(String senderId) {
        Map<String, Object> args = new HashMap<>();
        args.put("id", senderId);
        return createBody(COMMAND_RESET_CONNECTION, args);
    }

    /**
     * @param senderId vert.x address of the sender
     * @param receiverId vert.x address of the receiver
     * @param method the command method to invoke
     * @param args arguments for the method
     * @return transfer structure containing "header"
     * } and "body" (via {@link #createBody(java.lang.String, java.util.Map) })
     */
    public JsonObject createTransferStructure(String senderId, String receiverId, String method, Map<String, Object> args) {
        return createTransferStructure(senderId, receiverId, createBody(method, args));
    }

    public JsonObject createTransferStructure(String senderId, String receiverId, JsonObject body) {
        return new JsonObject().put(TAG_HEADER, createHeader(senderId, receiverId)).put(TAG_BODY, body);
    }

    /**
     * @param message a JsonObject with this minimal format: {"header": { "senderId": "some_string"}}
     * @return the "senderID"
     */
    public static String getSender(JsonObject message) {
        JsonObject header = message.getJsonObject(TAG_HEADER);
        if (header == null) {
            return null;
        }
        return header.getString(TAG_HEADER_SENDERID);
    }

    JsonObject putTransferHeadersIntoMessage(String senderId, String receiverId, JsonObject message) {
        LOG.fine("Putting transfer headers to message");

        if (message.getJsonObject(TAG_HEADER) == null) {
            JsonObject header = new JsonObject();
            header.put(TAG_HEADER_SENDERID, senderId);
            header.put(TAG_HEADER_RECEIVERID, receiverId);
            message.put(TAG_HEADER, header);
        } else {
            message.getJsonObject(TAG_HEADER)
                    .put(TAG_HEADER_SENDERID, senderId)
                    .put(TAG_HEADER_RECEIVERID, receiverId);
        }
        LOG.log(Level.FINE, "Message with transfer headers: {0}", message);
        return message;

    }

}
