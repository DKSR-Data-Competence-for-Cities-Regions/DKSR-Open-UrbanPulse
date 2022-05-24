package de.urbanpulse.transfer;

import java.util.HashMap;
import java.util.Map;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class Command {

    protected final String methodName;
    protected final Map<String, Object> args;

    public Command(String methodName, Map<String, Object> args) {
        this.methodName = methodName;
        this.args = args;
    }

    public String getMethodName() {
        return methodName;
    }

    public Map<String, Object> getArgs() {
        return new HashMap<>(args);
    }

}
