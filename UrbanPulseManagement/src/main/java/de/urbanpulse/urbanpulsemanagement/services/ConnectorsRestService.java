package de.urbanpulse.urbanpulsemanagement.services;

import de.urbanpulse.urbanpulsecontroller.admin.ConnectorManagementDAO;
import de.urbanpulse.urbanpulsecontroller.admin.SensorManagementDAO;
import de.urbanpulse.urbanpulsecontroller.admin.TransferObjectFactory;
import de.urbanpulse.dist.jee.entities.ConnectorEntity;
import de.urbanpulse.dist.jee.entities.SensorEntity;
import de.urbanpulse.urbanpulsecontroller.admin.transfer.ConnectorTO;
import de.urbanpulse.urbanpulsecontroller.admin.transfer.SensorTO;
import de.urbanpulse.urbanpulsemanagement.restfacades.AbstractRestFacade;
import de.urbanpulse.urbanpulsemanagement.services.factories.ErrorResponseFactory;
import de.urbanpulse.urbanpulsemanagement.services.wrapper.SensorModuleUpdateWrapper;
import de.urbanpulse.urbanpulsemanagement.services.wrapper.SensorModuleUpdateWrapperException;
import de.urbanpulse.urbanpulsemanagement.transfer.ConnectorsWrapperTO;
import de.urbanpulse.urbanpulsemanagement.transfer.SensorsWrapperTO;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.naming.OperationNotSupportedException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 *
 *
 * REST web service for registering connectors and accessing their keys
 *
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@Stateless
public class ConnectorsRestService extends AbstractRestService {

    @Inject
    private ConnectorManagementDAO connectorDao;

    @Inject
    private SensorManagementDAO sensorDao;

    @Inject
    private SensorModuleUpdateWrapper sensorModuleUpdateWrapper;

    @Inject
    private TransferObjectFactory transferObjectFactory;

    /**
     * retrieve all registered connectors
     *
     * @return connectors wrapped in JSON object
     */
    public Response getAllConnectors() {
        List<ConnectorTO> connectors = connectorDao.getAll();
        ConnectorsWrapperTO wrapper = new ConnectorsWrapperTO(connectors);
        return Response.ok(wrapper.toJson()).build();
    }

    /**
     * retrieve a registered connector
     *
     * @param id connector ID
     * @return connector
     */
    public Response getConnector(String id) {
        ConnectorTO connector = connectorDao.getById(id);

        if (connector == null) {
            return ErrorResponseFactory.notFound("connector with id [" + id + "] does not exist");
        }

        return Response.ok(connector.toJson()).build();
    }

    /**
     * delete a registered connector via its ID
     *
     * @param id connector ID
     * @return 204 NO CONTENT or 202 Accepted (if failed to unregister in Inbound)
     */
    public Response deleteConnector(String id) {
        boolean usedBySensor = false;
        for (SensorTO sensor : sensorDao.getAll()) {
            if (id.equals(sensor.getSenderid())) {
                usedBySensor = true;
                break;
            }
        }

        if (usedBySensor) {
            return ErrorResponseFactory.conflict("connector with id[" + id + "] still used by a sensor");
        }

        String deletedId = connectorDao.deleteById(id);

        if (deletedId.equals(id)) {
            try {
                sensorModuleUpdateWrapper.unregisterConnector(id);
            } catch (SensorModuleUpdateWrapperException ex) {
                Logger.getLogger(ConnectorsRestService.class.getName()).log(Level.SEVERE, null, ex);
                return ErrorResponseFactory.fromStatus(Response.Status.ACCEPTED, ex.getMessage());
            }
        }

        return Response.noContent().build();
    }

    /**
     * create a new connector registration
     *
     * @param jsonString JSON string with description field
     * @param context used to get base builder for the URI
     * @param facade the REST facade
     * @return 201 created response with location header set appropriately or 202 Accepted (if failed to register in Inbound)
     */
    public Response createConnector(String jsonString, UriInfo context, AbstractRestFacade facade) {
        JsonReader jsonReader = Json.createReader(new StringReader(jsonString));
        JsonObject jsonObject = jsonReader.readObject();
        if (!jsonObject.containsKey("description")) {
            return ErrorResponseFactory.unprocessibleEntity("connector description missing");
        }

        JsonObject description = jsonObject.getJsonObject("description");
        JsonString backchannelEndpointJson = null;
        if (jsonObject.containsKey("backchannelEndpoint") && !jsonObject.isNull("backchannelEndpoint")) {
            backchannelEndpointJson = jsonObject.getJsonString("backchannelEndpoint");
        }

        Optional<Response> validationResponse = validateNoDuplicateConnectorName(null, jsonObject);
        if (validationResponse.isPresent()) {
            return validationResponse.get();
        }

        ConnectorTO createdConnector;

        if (backchannelEndpointJson == null) {
            createdConnector = connectorDao.createConnector(description.toString());
        } else {
            try {
                URI backchannelEndpoint = new URI(backchannelEndpointJson.getString());
                createdConnector = connectorDao.createConnector(description.toString(), backchannelEndpoint);
            } catch (NullPointerException | URISyntaxException ex) {
                return ErrorResponseFactory.badRequest("Backchannel URI malformed");
            }
        }

        if (null == createdConnector) {
            return ErrorResponseFactory.internalServerError("failed to create connector");
        }

        URI connectorUri = getItemUri(context, facade, createdConnector.getId());

        try {
            sensorModuleUpdateWrapper.registerConnector(createdConnector.getId(), createdConnector.getKey(),
                    createdConnector.getBackchannelKey(), createdConnector.getBackchannelEndpoint());
        } catch (SensorModuleUpdateWrapperException ex) {
            Logger.getLogger(ConnectorsRestService.class.getName()).log(Level.SEVERE, null, ex);
            return ErrorResponseFactory.fromStatus(Response.Status.ACCEPTED, ex.getMessage());
        }

        return Response.created(connectorUri).build();
    }

    /**
     * update an existing connector
     *
     * @param id ID string of the connector to update
     * @param jsonString JSON string with description field
     * @return 204 NO CONTENT on success or 202 Accepted (if failed to update in Inbound)
     */
    public Response updateConnector(String id, String jsonString) {
        JsonReader jsonReader = Json.createReader(new StringReader(jsonString));
        JsonObject jsonObject = jsonReader.readObject();

        if (!jsonObject.containsKey("description")) {
            return ErrorResponseFactory.unprocessibleEntity("connector description missing");
        }

        JsonObject description = jsonObject.getJsonObject("description");
        JsonString backchannelEndpointJson = null;
        if (jsonObject.containsKey("backchannelEndpoint") && !jsonObject.isNull("backchannelEndpoint")) {
            backchannelEndpointJson = jsonObject.getJsonString("backchannelEndpoint");
        }

        Optional<Response> validationResponse = validateNoDuplicateConnectorName(id, jsonObject);
        if (validationResponse.isPresent()) {
            return validationResponse.get();
        }

        try {
            ConnectorTO updatedConnector;

            if (backchannelEndpointJson == null) {
                updatedConnector = connectorDao.updateConnector(id, description.toString());
            } else {
                try {
                    URI backchannelEndpoint = new URI(backchannelEndpointJson.getString());
                    updatedConnector = connectorDao.updateConnector(id, description.toString(), backchannelEndpoint);
                } catch (NullPointerException | URISyntaxException ex) {
                    return ErrorResponseFactory.badRequest("Backchannel URI malformed");
                }
            }

            if (null == updatedConnector) {
                return ErrorResponseFactory.internalServerError("failed to update connector with id[" + id + "]");
            }

            try {
                sensorModuleUpdateWrapper.updateConnector(updatedConnector.getId(), updatedConnector.getKey(),
                        updatedConnector.getBackchannelKey(), updatedConnector.getBackchannelEndpoint());
            } catch (SensorModuleUpdateWrapperException ex) {
                Logger.getLogger(ConnectorsRestService.class.getName()).log(Level.SEVERE, null, ex);
                return ErrorResponseFactory.fromStatus(Response.Status.ACCEPTED, ex.getMessage());
            }
        } catch (OperationNotSupportedException ex) {
            return ErrorResponseFactory.notFound("connector to update with id[" + id + "] not found");
        }

        return Response.noContent().build();
    }

    /**
     * retrieve registered all sensors for given connector
     *
     * @param connectorId connector ID string returned)
     * @return sensors wrapped in JSON object
     */
    public Response getSensorsForConnector(String connectorId) {
        ConnectorEntity connector = connectorDao.queryById(connectorId);
        if (connector == null) {
            return ErrorResponseFactory.notFound("connector with id [" + connectorId + "] does not exist");
        }
        List<SensorTO> sensors = sensorDao.getSensorsOfConnector(connectorId);
        SensorsWrapperTO wrapper = new SensorsWrapperTO(sensors);

        return Response.ok(wrapper.toJson().encodePrettily()).build();
    }

    private Optional<Response> validateNoDuplicateConnectorName(String connectorId, JsonObject connectorJson) {
        JsonObject description = connectorJson.getJsonObject("description");
        if (!description.containsKey("name")) {
            return Optional.of(ErrorResponseFactory.unprocessibleEntity("Connector description requires a name"));
        }
        String connectorName = description.getString("name");
        boolean conflict = connectorId != null ? connectorWithNameAndDifferentIdExists(connectorName, connectorId)
                : connectorWithNameExists(connectorName);
        if (conflict) {
            return Optional.of(ErrorResponseFactory.conflict("Connector with name [" + connectorName + "] already exists."));
        }
        // Else: no connector with that name
        return Optional.empty();
    }

    private boolean connectorWithNameExists(String connectorName) {
        return connectorDao
                .getAll()
                .stream()
                .map(this::getConnectorName)
                .anyMatch(connectorName::equals);
    }

    private boolean connectorWithNameAndDifferentIdExists(String connectorName, String id) {
        return connectorDao
                .getAll()
                .stream()
                .filter(connectorTO -> !id.equals(connectorTO.getId()))
                .map(this::getConnectorName)
                .anyMatch(connectorName::equals);
    }

    private String getConnectorName(ConnectorTO connectorTO) {
        JsonObject connectorJson = connectorTO.toJson();
        JsonObject description = connectorJson.getJsonObject("description");
        if (description == null) {
            return null;
        } else {
            return description.getString("name");
        }
    }
}
