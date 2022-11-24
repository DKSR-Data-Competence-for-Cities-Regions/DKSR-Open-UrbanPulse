package de.urbanpulse.backchannel.auth;

import de.urbanpulse.backchannel.pojos.UsernamePasswordCredential;
import de.urbanpulse.util.HashingUtils;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Steffen Haertlein
 */
public class BasicAuthorization extends Authorization<UsernamePasswordCredential> {
    public BasicAuthorization(List<UsernamePasswordCredential> validCredentialList) {
        super(validCredentialList);
    }

    public BasicAuthorization() {
        this(new ArrayList<>());
    }

    @Override
    public boolean isAuthorized(UsernamePasswordCredential credential) {
        return this.validCredentials.stream().anyMatch((cred) -> (cred.getUsername().equals(credential.getUsername())
                && HashingUtils.checkPassword(credential.getPassword(), cred.getPassword())));
    }

    @Override
    public boolean hasValidCredentialsFor(UsernamePasswordCredential credential) {
        return validCredentials.stream().anyMatch((cred) -> (credential.getUsername().equals(cred.getUsername())));
    }

    @Override
    protected void removeValidCredentialsFor(UsernamePasswordCredential credential) {
        for (UsernamePasswordCredential cred : this.validCredentials) {
            if (cred.getUsername().equals(credential.getUsername())) {
                this.validCredentials.remove(cred);
                return;
            }
        }
    }
}
