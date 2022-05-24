package de.urbanpulse.urbanpulsecontroller.modules.vertx.master;

import de.urbanpulse.urbanpulsecontroller.admin.UserManagementDAO;
import de.urbanpulse.dist.jee.entities.UserEntity;
import io.vertx.core.json.JsonObject;
import java.util.List;
import javax.ejb.Stateless;
import javax.inject.Inject;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
@Stateless
public class UserAuthCreator {

    @Inject
    private UserManagementDAO userManagementDAO;

    /**
     *
     * @return userAuth json object containing usernames mapped to objects containing hmacKey and passwordHash for admin and appUser groups ONLY!
     */
    public JsonObject createUserAuth() {
        JsonObject userAuth = new JsonObject();
        List<UserEntity> users = userManagementDAO.queryAll();
        for (UserEntity user : users) {
            String userName = user.getName();
            String passwordHash = user.getPasswordHash();
            String hmacKey = user.getKey();

            JsonObject userCredential = new JsonObject();
            userCredential.put("passwordHash", passwordHash);
            userCredential.put("hmacKey", hmacKey);
            userAuth.put(userName, userCredential);
        }
        return userAuth;
    }
}
