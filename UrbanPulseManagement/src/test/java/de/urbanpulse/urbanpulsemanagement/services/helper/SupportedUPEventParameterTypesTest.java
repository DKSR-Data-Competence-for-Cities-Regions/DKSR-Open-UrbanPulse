package de.urbanpulse.urbanpulsemanagement.services.helper;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class SupportedUPEventParameterTypesTest {

    public SupportedUPEventParameterTypesTest() {
    }

    /**
     * Test of contains method, of class SupportedUPEventParameterTypes.
     */
    @Test
    public void testContains() {

        SupportedUPEventParameterTypes instance = new SupportedUPEventParameterTypes();
        boolean result = instance.contains(SupportedUPEventParameterTypes.TYPE_MAP);
        assertEquals(true, result);
        result = instance.contains(SupportedUPEventParameterTypes.TYPE_DOUBLE);
        assertEquals(true, result);
        result = instance.contains(SupportedUPEventParameterTypes.TYPE_LIST);
        assertEquals(true, result);
        result = instance.contains(SupportedUPEventParameterTypes.TYPE_DATE);
        assertEquals(true, result);
        result = instance.contains(SupportedUPEventParameterTypes.TYPE_BOOLEAN);
        assertEquals(true, result);
        result = instance.contains(SupportedUPEventParameterTypes.TYPE_STRING);
        assertEquals(true, result);
    }

}
