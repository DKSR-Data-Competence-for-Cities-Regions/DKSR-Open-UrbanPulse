package de.urbanpulse.dist.outbound.server.historicaldata;

import java.util.Comparator;
import java.util.TreeSet;

/**
 * sorted string set which always places "SID" first and "timestamp" second, then all others in string compare order
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class CSVHeaderSet extends TreeSet<String> {

    private static class EventFieldComparator implements Comparator<String> {

        @Override
        public int compare(String first, String second) {
            if (first == null ? second == null : first.equals(second)) {
                return 0;
            }

            if ("SID".equals(first)) {
                return -1;
            }

            if ("SID".equals(second)) {
                return 1;
            }

            // now SID is always the first

            if ("timestamp".equals(first)) {
                return -1;
            }

            if ("timestamp".equals(second)) {
                return 1;
            }

            // and timestamp is second

            return first.compareToIgnoreCase(second);
        }

    }

    public CSVHeaderSet() {
        super(new EventFieldComparator());
    }
}
