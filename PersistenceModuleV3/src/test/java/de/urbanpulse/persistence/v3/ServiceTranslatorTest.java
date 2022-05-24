package de.urbanpulse.persistence.v3;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class ServiceTranslatorTest {

    public ServiceTranslatorTest() {
    }



    @Test(expected = IllegalArgumentException.class)
    public void testConvertForOtherImplementations() {
        String otherImplementation = "otherImplementation";
        ServiceTranslator instance = new ServiceTranslator();

        String result = instance.convert(otherImplementation);
        assertEquals(otherImplementation, result);

    }

}
