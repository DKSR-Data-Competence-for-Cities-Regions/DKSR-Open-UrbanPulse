package de.urbanpulse.urbanpulsemanagement.restfacades.dto;

import java.util.List;

/**
 *  DTO representing permissions with scopes and operations in an incoming HTTP messages
 *  operations can contain: "read", "write", "delete", "*".
 *  scopes can contain: "livedata", "historicdata", "model", "permission", "*".
 *
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class ScopesWithOperations {

    private List<String> scope;

    private List<String> operation;

    public List<String> getScope() {
        return scope;
    }

    public void setScope(List<String> scope) {
        this.scope = scope;
    }

    public List<String> getOperation() {
        return operation;
    }

    public void setOperation(List<String> operation) {
        this.operation = operation;
    }
}
