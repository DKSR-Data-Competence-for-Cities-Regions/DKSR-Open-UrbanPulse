package de.urbanpulse.urbanpulsecontroller.admin.transfer;

import de.urbanpulse.dist.jee.entities.StatementEntity;
import de.urbanpulse.dist.jee.entities.UpdateListenerEntity;
import static org.junit.Assert.assertNotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.mockito.BDDMockito.given;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@RunWith(MockitoJUnitRunner.class)
public class UpdateListenerTOTest {

    @Mock
    private UpdateListenerEntity updateListenerEntityMock;

    @Mock
    private StatementEntity statementEntityMock;

    private final String authJsonTOMock = new AuthJsonTO("{\n"
                + "\"authMethod\" : \"PASCAL\",\n"
                + "\"user\": \"MaryPoppins\",\n"
                + "\"password\":\"Supercalifragilisticexpialidocious\"\n"
                + "}").toString();

    @Before
    public void setUp() {
        given(updateListenerEntityMock.getAuthJson()).willReturn(authJsonTOMock);
        given(updateListenerEntityMock.getStatement()).willReturn(statementEntityMock);
    }

    @Test
    public void testPasswordsAreNotCensoredInUpdateListenersFromEntities() {
        UpdateListenerTO updateListenerTO = new UpdateListenerTO(updateListenerEntityMock);
        assertNotNull(updateListenerTO.getAuthJson());
        assertNotNull(updateListenerTO.getAuthJson().getPassword());
    }
}
