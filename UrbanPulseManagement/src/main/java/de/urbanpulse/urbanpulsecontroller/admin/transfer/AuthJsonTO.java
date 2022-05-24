package de.urbanpulse.urbanpulsecontroller.admin.transfer;

import io.vertx.core.json.JsonObject;
import java.io.Serializable;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class AuthJsonTO implements Serializable {

    private String authMethod;
    private String user;
    private String password;

    public AuthJsonTO() {

    }

    public AuthJsonTO(String jsonString) {
        String json = (jsonString == null) ? "{}" : jsonString;
        JsonObject authJson = new JsonObject(json);
        this.authMethod = authJson.getString("authMethod");
        this.user = authJson.getString("user");
        this.password = authJson.getString("password");
    }

    public AuthJsonTO(AuthJsonTO other) {
        this.authMethod = other.getAuthMethod();
        this.user = other.getUser();
        this.password = other.getPassword();
    }

    public String getAuthMethod() {
        return authMethod;
    }

    public void setAuthMethod(String authMethod) {
        this.authMethod = authMethod;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String toString() {
        JsonObject authJsonObj = new JsonObject();
        if (authMethod != null) {
            authJsonObj.put("authMethod", authMethod);
        }
        if (user != null) {
            authJsonObj.put("user", user);
        }
        if (password != null) {
            authJsonObj.put("password", password);
        }
        return authJsonObj.encode();
    }

}
