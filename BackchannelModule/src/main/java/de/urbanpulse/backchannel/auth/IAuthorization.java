package de.urbanpulse.backchannel.auth;

import de.urbanpulse.backchannel.pojos.Credential;

/**
 *
 * @author Steffen Haertlein
 */
interface IAuthorization<T extends Credential> {
    public boolean isAuthorized(T credential);

    public boolean hasValidCredentialsFor(T credential);
}
