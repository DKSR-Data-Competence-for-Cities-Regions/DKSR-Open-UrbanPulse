package de.urbanpulse.urbanpulsecontroller.admin.virtualsensors;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class TestData {

    public static final String VIRTUAL_SENSOR_REQUEST = "{\n"
            + "	\"statements\": [\n"
            + "		{\n"
            + "			\"name\": \"TestEventTypeStatement\",\n"
            + "			\"query\": \"insert into TestEventType select temp from OpenWeatherMapEventType\"\n"
            + "		}\n"
            + "	],\n"
            + "	\"eventTypes\": [],\n"
            + "	\"resultEventType\": {\n"
            + "		\"name\": \"TestEventType\",\n"
            + "		\"config\": {\n"
            + "			\"SID\":\"string\",\n"
            + "			\"timestamp\":\"java.util.Date\",\n"
            + "			\"temp\": \"double\"\n"
            + "		},\n"
            + "		\"description\": {\n"
            + "		}\n"
            + "	},\n"
            + "	\"description\": {\n"
            + "		\"Virtual Sensor\": \"A Test\"\n"
            + "	},\n"
            + "	\"category\": \"819f45d2-0a45-41b4-8da7-b8f26d202fbc\",\n"
            + "	\"targets\": [\n"
            + "		\"thePersistence\"\n"
            + "	]\n"
            + "}";

}
