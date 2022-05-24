package de.urbanpulse.dist.outbound.server.ws;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class WsServerTargetMatcherTest {

    private WsServerTargetMatcher matcher;

    @Before
    public void setUp() {
        matcher = new WsServerTargetMatcher();
    }

    @Test(expected = IllegalArgumentException.class)
    public void matches_throwsForNullTarget() {
        matcher.matches("/the/path/lalastatement", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void matches_throwsForNullBase() {
        matcher.matches(null, "/the/path");
    }

    @Test(expected = IllegalArgumentException.class)
    public void matches_throwsForEmptyBase() {
        matcher.matches("/the/path/lalastatement", "");
    }

    @Test(expected = IllegalArgumentException.class)
    public void matches_throwsForTrailingSlashInBase() {
        matcher.matches("/foo", "/foo/");
    }

    @Test
    public void matches_falseForEmptyTarget() {
        assertFalse(matcher.matches("", "/the/path"));
    }

    @Test
    public void matches_trueForExactMatch() {
        assertTrue(matcher.matches("/the/path/lalastatement", "/the/path"));
    }

    @Test
    public void matches_falseForCaseDifferenceInBase() {
        assertFalse(matcher.matches("/THe/pAth/lalastatement", "/the/path"));
    }

    @Test
    public void matches_falseForNonTrimmed() {
        assertFalse(matcher.matches("/the/path/lalastatement ", "/the/path"));
    }

    @Test
    public void matches_falseForMissingPathElement() {
        assertFalse(matcher.matches("/the/path/missing/lalastatement", "/the/path"));
    }

    @Test
    public void matches_withRedundantSlashesPathElement() {
        assertTrue(matcher.matches("//OutboundInterfaces//outbound//WorkOrderStatement//", "/OutboundInterfaces/outbound"));
    }

    @Test
    public void extractStatement_extractsStatement() {
        assertEquals("lalastatement", matcher.extractStatement("/the/path/lalastatement", "/the/path"));
    }

    @Test
    public void extractStatement_extractsStatementEvenWithRedundantSlashes() {
        assertEquals("WorkOrderStatement", matcher.extractStatement("//OutboundInterfaces//outbound//WorkOrderStatement//",
                "/OutboundInterfaces/outbound"));
    }

    @Test
    public void extractStatement_nullForMismatch() {
        assertNull(matcher.extractStatement("/does/not/match/", "/the/path"));
    }
}
