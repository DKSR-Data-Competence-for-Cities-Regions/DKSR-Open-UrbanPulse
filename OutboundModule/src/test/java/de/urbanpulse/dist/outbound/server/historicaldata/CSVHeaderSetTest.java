package de.urbanpulse.dist.outbound.server.historicaldata;

import java.util.LinkedList;
import java.util.List;
import java.util.SortedSet;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class CSVHeaderSetTest {

    @Test
    public void testSortedButSidFirstAndTimestampSecond() {
        SortedSet<String> set = new CSVHeaderSet();
        set.add("a");
        set.add("timestamp");
        set.add("b");
        set.add("SID");
        set.add("c");

        List<String> sorted = new LinkedList<>(set);
        assertEquals("SID", sorted.get(0));
        assertEquals("timestamp", sorted.get(1));
        assertEquals("a", sorted.get(2));
        assertEquals("b", sorted.get(3));
        assertEquals("c", sorted.get(4));
    }

}
