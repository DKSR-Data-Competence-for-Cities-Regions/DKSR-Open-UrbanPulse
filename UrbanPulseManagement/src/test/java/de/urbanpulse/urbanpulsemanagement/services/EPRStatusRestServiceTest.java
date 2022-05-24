/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.urbanpulse.urbanpulsemanagement.services;

import de.urbanpulse.urbanpulsemanagement.services.wrapper.EventProcessorWrapper;
import java.net.URI;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.mockito.BDDMockito.given;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@RunWith(MockitoJUnitRunner.class)
public class EPRStatusRestServiceTest {

    private static final String AUTH_HEADER = "someValidAuthHeader";
    private static final URI ABSOLUTE_PATH = URI.create("https://foo.bar/some/absolute/path/with/https/");
    private static final String RELATIVE_PATH_FOR_ROOT = "/users";

    private static final String KEY_EVENTS_PROCESSED = "events_processed";

    @Mock(name = "eventProcessor")
    EventProcessorWrapper eventProcessorMock;

    @Mock(name = "context")
    protected UriInfo contextMock;


    public EPRStatusRestServiceTest() {
    }

    @After
    public void tearDown() {
    }




}
