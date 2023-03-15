package de.urbanpulse.urbanpulsemanagement.services.helper;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class SupportedJsonSchemaTypesTest {
    
    public SupportedJsonSchemaTypesTest() {
    }

    /**
     * Test of contains method, of class SupportedJsonSchemaTypes.
     */
    @Test
    public void testContains() {
        SupportedJsonSchemaTypes instance = new SupportedJsonSchemaTypes();
        
        boolean result = instance.contains(SupportedJsonSchemaTypes.TYPE_ARRAY);
        assertEquals(true, result);
        result = instance.contains(SupportedJsonSchemaTypes.TYPE_INTEGER);
        assertEquals(true, result);
        result = instance.contains(SupportedJsonSchemaTypes.TYPE_INTEGER);
        assertEquals(true, result);
        result = instance.contains(SupportedJsonSchemaTypes.TYPE_NUMBER);
        assertEquals(true, result);
        result = instance.contains(SupportedJsonSchemaTypes.TYPE_OBJECT);
        assertEquals(true, result);
        result = instance.contains(SupportedJsonSchemaTypes.TYPE_STRING);
        assertEquals(true, result);
        
    }
    
}
