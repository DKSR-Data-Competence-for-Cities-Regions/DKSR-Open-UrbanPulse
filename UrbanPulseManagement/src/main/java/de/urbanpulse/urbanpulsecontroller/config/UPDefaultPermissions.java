package de.urbanpulse.urbanpulsecontroller.config;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class UPDefaultPermissions {

    private UPDefaultPermissions(){
    }

    public static final String HISTORIC_DATA_OPERATOR_FOR_ALL_SENSORS = "sensor:*:historicdata:*";

    public static final String HISTORIC_DATA_READER_FOR_ALL_SENSORS = "sensor:*:historicdata:read";

    public static final String LIVE_DATA_READER_FOR_ALL_SENSORS = "sensor:*:livedata:read";

    public static final String PERMISSION_OPERATOR_FOR_ALL_SENSORS = "sensor:*:permission:*";

}
