package de.urbanpulse.backchannel.auth;

import de.urbanpulse.backchannel.pojos.Credential;
import java.util.List;

/**
 *
 * @author Steffen Haertlein
 *
 */
public abstract class Authorization<T extends Credential> implements IAuthorization<T> {
    protected List<T> validCredentials;

    /*
     * @param validCredentails list of the valid credentials
     * */
    public Authorization(List<T> validCredentails) {
        this.validCredentials = validCredentails;
    }

    public void addValidCredential(T validCredential) {
        if (this.hasValidCredentialsFor(validCredential)) {
            this.removeValidCredentialsFor(validCredential);
        }
        this.validCredentials.add(validCredential);
    }

    public void setValidCredentials(List<T> validCredentials) {
        this.validCredentials = validCredentials;
    }

    protected abstract void removeValidCredentialsFor(T credential);

}
