package de.urbanpulse.transfer;

import java.util.HashMap;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class CommandArgsFactory {

    public HashMap<String, Object> buildArgs(String... data) {
        if (data.length % 2 != 0) {
            throw new IllegalArgumentException("Odd number of arguments");
        }
        HashMap<String, Object> result = new HashMap<>();
        String key = null;
        Integer step = -1;
        for (String value : data) {
            step++;
            switch (step % 2) {
                case 0:
                    if (value == null) {
                        throw new IllegalArgumentException("Null key value");
                    }
                    key = value;
                    continue;
                case 1:
                    result.put(key, value);
                    break;
            }
        }
        return result;
    }
}
