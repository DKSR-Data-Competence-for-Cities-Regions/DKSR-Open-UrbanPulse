package de.urbanpulse.urbanpulsecontroller.util;

import java.util.List;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class JSONHelper {

    public static JsonArray listToJsonArray(List<String> list) {
        JsonArrayBuilder b = Json.createArrayBuilder();
        for(String s :list) {
            b.add(s);
        }
        return b.build();
    }

    private JSONHelper() {
        //SQ
    }
}
