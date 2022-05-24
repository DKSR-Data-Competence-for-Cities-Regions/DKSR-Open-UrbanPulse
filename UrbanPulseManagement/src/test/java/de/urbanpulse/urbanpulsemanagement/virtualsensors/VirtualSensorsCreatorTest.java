package de.urbanpulse.urbanpulsemanagement.virtualsensors;

import de.urbanpulse.urbanpulsecontroller.admin.virtualsensors.VirtualSensorConfiguration;
import de.urbanpulse.urbanpulsecontroller.admin.virtualsensors.VirtualSensorConfigurationException;
import de.urbanpulse.urbanpulsecontroller.admin.virtualsensors.VirtualSensorConfigurator;
import de.urbanpulse.urbanpulsemanagement.restfacades.AbstractRestFacade;
import javax.json.JsonObject;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.runner.RunWith;
import static org.mockito.BDDMockito.*;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@RunWith(MockitoJUnitRunner.class)
public class VirtualSensorsCreatorTest {

    @InjectMocks
    @Spy
    private VirtualSensorsCreator creator;

    @Mock
    private VirtualSensorConfigurator mockConfigurator;

    @Mock
    private UriInfo mockContext;

    @Mock
    private AbstractRestFacade mockFacade;

    @Mock
    private Response mockResponse;

    @Mock
    private JsonObject mockConfiguration;

    @Mock
    private VirtualSensorConfiguration mockVirtualSensorConfig;

    private static final String EXPECTED_CATEGORY_ID = "13";

    @Before
    public void setUp() {

        given(mockConfiguration.getString("category")).willReturn(EXPECTED_CATEGORY_ID);

        doReturn(mockResponse).when(creator).createVirtualSensorFromConfig(
                EXPECTED_CATEGORY_ID, mockVirtualSensorConfig, mockContext, mockFacade);
    }

    @Test
    public void createVirtualSensor_createsFromProperArgs() throws Exception {
        given(mockConfigurator.createVirtualSensorConfiguration(mockConfiguration)).willReturn(mockVirtualSensorConfig);

        Response response = creator.createVirtualSensor(mockConfiguration, mockContext, mockFacade);

        verify(mockConfigurator).createVirtualSensorConfiguration(mockConfiguration);

        assertSame(mockResponse, response);
    }



    @Test
    public void createVirtualSensor_returnsBadRequestOnConfigurationException() throws Exception {
        VirtualSensorConfigurationException exception = new VirtualSensorConfigurationException("something went wrong");

        given(mockConfigurator.createVirtualSensorConfiguration(mockConfiguration)).willThrow(exception);

        Response response = creator.createVirtualSensor(mockConfiguration, mockContext, mockFacade);

        assertEquals(exception.toString(), response.getEntity());
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }
}
