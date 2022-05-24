package de.urbanpulse.urbanpulsecontroller.admin.virtualsensors;

/**
 *
 * This enum defines the Operations, that can be performed by the predefined Virtual Sensors
 *
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public enum Operations {
    SUM,AVG,MIN,MAX;

    public static Operations toOperation(final String string) {

        for(Operations s : Operations.values()) {
            if(s.toString().equals(string)) {
                return s;
            }

        }
        return null;
    }
}
