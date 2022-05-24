/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.urbanpulse.urbanpulsecontroller.admin.modules;

import de.urbanpulse.urbanpulsecontroller.admin.StatementManagementDAO;
import de.urbanpulse.urbanpulsecontroller.admin.UpdateListenerDAO;
import de.urbanpulse.dist.jee.entities.StatementEntity;
import de.urbanpulse.urbanpulsecontroller.admin.transfer.AuthJsonTO;
import de.urbanpulse.urbanpulsecontroller.admin.transfer.UpdateListenerTO;
import de.urbanpulse.urbanpulsecontroller.modules.vertx.UPModuleType;
import io.vertx.core.json.JsonObject;
import java.util.logging.Logger;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.runner.RunWith;
import static org.mockito.BDDMockito.given;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import static org.mockito.ArgumentMatchers.any;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@RunWith(MockitoJUnitRunner.class)
public class OutboundSetupDAOTest {

    private static final Logger LOG = Logger.getLogger(OutboundSetupDAOTest.class.getName());

    @Mock
    private UpdateListenerDAO listenerDAO;

    @Mock
    private StatementManagementDAO statementDAOMock;

    @InjectMocks
    private OutboundSetupDAO outboundSetupDAOMock;

    public OutboundSetupDAOTest() {
    }

    /**
     * Test of createModuleSetup method, of class OutboundSetupDAO.
     * @throws Exception something went wrong by testing
     */
    @Test
    public void testCreateModuleSetup() throws Exception {
        StatementEntity testStatement = new StatementEntity();
        testStatement.setName("TEST_MOCK");

        given(statementDAOMock.queryById(any())).willReturn(testStatement);
        UpdateListenerTO updateListenerTO = new UpdateListenerTO();
        updateListenerTO.setAuthJson(new AuthJsonTO("{\n"
                + "\"authMethod\" : \"BASIC\",\n"
                + "\"user\":\"foo\",\n"
                + "\"password\":\"bar\"\n"
                + "}"));
        updateListenerTO.setId("1");
        updateListenerTO.setKey("HMAChmacHMAChmac");
        updateListenerTO.setStatementId("5");
        updateListenerTO.setTarget("https://dummy.de/dummy");
        JsonObject result = outboundSetupDAOMock.createOutboundListenerConfig(updateListenerTO);
        LOG.info(result.encodePrettily());

        JsonObject expectedResult = new JsonObject("{\n"
                + "  \"id\" : \"1\",\n"
                + "  \"statementName\" : \"TEST_MOCK\",\n"
                + "  \"target\" : \"https://dummy.de/dummy\",\n"
                + "  \"credentials\" : {\n"
                + "    \"authMethod\" : \"BASIC\",\n"
                + "    \"user\" : \"foo\",\n"
                + "    \"password\" : \"bar\",\n"
                + "    \"hmacKey\" : \"HMAChmacHMAChmac\"\n"
                + "  }\n"
                + "}");

        assertEquals(expectedResult, result);
    }

    /**
     * Test of getModuleType method, of class OutboundSetupDAO.
     *  @throws Exception something went wrong by testing
     */
    @Test
    public void testGetModuleType() throws Exception {
        assertEquals(UPModuleType.OutboundInterface, new OutboundSetupDAO().getModuleType());
    }

}
