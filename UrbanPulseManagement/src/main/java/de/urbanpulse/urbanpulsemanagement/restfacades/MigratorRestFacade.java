package de.urbanpulse.urbanpulsemanagement.restfacades;

import de.urbanpulse.dist.jee.entities.SensorEntity;
import de.urbanpulse.urbanpulsecontroller.admin.SensorManagementDAO;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresRoles;

import javax.ejb.EJB;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static de.urbanpulse.urbanpulsecontroller.config.UPDefaultRoles.ADMIN;
import static de.urbanpulse.urbanpulsecontroller.config.UPDefaultRoles.CONNECTOR_MANAGER;
import static de.urbanpulse.urbanpulsemanagement.restfacades.MigratorRestFacade.ROOT_PATH;

/**
 * REST Web Service migrating certain entities
 *
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@Path(ROOT_PATH)
public class MigratorRestFacade extends AbstractRestFacade {

    static final String ROOT_PATH = "migrator";

    @EJB
    private SensorManagementDAO sensorDao;

    @RequiresRoles(value = {ADMIN, CONNECTOR_MANAGER}, logical = Logical.OR)
    @POST
    @Consumes("application/json")
    @Path("/sensors/descripton/reference")
    public Response addReferenceToSensorDescriptions(String json) {
        JsonArray orderedReferenceFieldNames = new JsonObject(json).getJsonArray("params");

        List<SensorEntity> sensors = sensorDao.queryAll();
        long count = 0;
        for (SensorEntity sensor : sensors) {
            JsonObject descrption = new JsonObject(sensor.getDescription());
            if (descrption.containsKey("reference")) {
                // already migrated
                continue;
            }

            List orderedReferenceValues = getReferenceValues(orderedReferenceFieldNames, descrption);
            if (orderedReferenceValues.size() != orderedReferenceFieldNames.size()) {
                // reference values do not match
                continue;
            }

            JsonObject newDescription = getNewDescription(descrption, orderedReferenceValues, orderedReferenceFieldNames);

            sensor.setDescription(newDescription.encode());
            sensorDao.merge(sensor);
            count++;
        }

        final String message = "added [" + count + "] sensor references for " + orderedReferenceFieldNames;
        Logger.getLogger(this.getClass().getName()).log(Level.INFO, message);
        return Response.ok(message).build();
    }

    private List getReferenceValues(JsonArray orderedReferenceFieldNames, JsonObject descrption) {
        List orderedReferenceValues = new LinkedList();
        for (Object obj : orderedReferenceFieldNames) {
            String fieldName = (String) obj;
            if (descrption.containsKey(fieldName)) {
                orderedReferenceValues.add(descrption.getValue(fieldName));
            }
        }
        return orderedReferenceValues;
    }

    private JsonObject getNewDescription(JsonObject descrption, List referenceValues, JsonArray orderedReferenceFieldNames) {
        JsonObject newDescription = descrption.copy();
        JsonObject reference = new JsonObject();
        for (int i = 0; i < referenceValues.size(); i++) {
            String key = orderedReferenceFieldNames.getString(i);
            Object value = referenceValues.get(i);
            reference.put(key, value);
        }
        newDescription.put("reference", reference);
        return newDescription;
    }

}
