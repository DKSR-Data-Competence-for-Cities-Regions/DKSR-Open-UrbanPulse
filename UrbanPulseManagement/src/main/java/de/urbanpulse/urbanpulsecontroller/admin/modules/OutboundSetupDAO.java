package de.urbanpulse.urbanpulsecontroller.admin.modules;

import de.urbanpulse.urbanpulsecontroller.admin.StatementManagementDAO;
import de.urbanpulse.urbanpulsecontroller.admin.UpdateListenerDAO;
import de.urbanpulse.dist.jee.entities.StatementEntity;
import de.urbanpulse.urbanpulsecontroller.admin.entities.modules.UPModuleEntity;
import de.urbanpulse.urbanpulsecontroller.admin.transfer.UpdateListenerTO;
import de.urbanpulse.urbanpulsecontroller.modules.vertx.UPModuleType;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.List;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.inject.Inject;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@Stateless
@LocalBean
public class OutboundSetupDAO implements ModuleSetup {

    public static final String OUTBOUND_VERTX_ADDRESS = "theOutbound";

    @Inject
    private UpdateListenerDAO listenerDAO;

    @Inject
    private StatementManagementDAO statementDAO;

    @Override
    public JsonObject createModuleSetup(UPModuleEntity module, JsonObject setup) {

        JsonArray listeners = createOutboundListeners();
        setup.put("listeners", listeners);

        return setup;
    }

    /**
     * This method creates the json config object of an UpdateListerner for the
     * OutboundModules.
     *
     * @param to The transfer object of the UpdateListener
     * @return json config of an UpdateListener
     */
    public JsonObject createOutboundListenerConfig(UpdateListenerTO to) {
        JsonObject listener = new JsonObject();
        String id = "" + to.getId();
        String hmacKey = to.getKey();
        String target = to.getTarget();
        String statementId = to.getStatementId();
        StatementEntity statement = statementDAO.queryById(statementId);
        String statementName = statement.getName();

        listener.put("id", id);
        listener.put("statementName", statementName);
        listener.put("target", target);
        JsonObject credentials;

        if (to.getAuthJson() != null) {
            JsonObject authJson = new JsonObject(Json.encode(to.getAuthJson()));
            if (authJson.getString("authMethod") != null) {
                credentials = authJson;
            } else {
                credentials = new JsonObject();
            }
        } else {
            credentials = new JsonObject();
        }

        credentials.put("hmacKey", hmacKey); // The hmacKey will be provided in any case.
        listener.put(
                "credentials", credentials);
        return listener;
    }

    private JsonArray createOutboundListeners() {
        JsonArray listeners = new JsonArray();
        List<UpdateListenerTO> listenerEntities = listenerDAO.getAll();
        listenerEntities.stream().map(this::createOutboundListenerConfig).forEach(listeners::add);
        return listeners;
    }

    @Override
    public UPModuleType getModuleType() {
        return UPModuleType.OutboundInterface;
    }

}
