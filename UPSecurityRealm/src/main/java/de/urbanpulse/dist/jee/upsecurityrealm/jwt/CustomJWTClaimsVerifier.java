package de.urbanpulse.dist.jee.upsecurityrealm.jwt;

import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.BadJWTException;
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier;
import java.util.List;

/**
 * adds audience verification to the {@link DefaultJWTClaimsVerifier} of nimbus-jose-jwt
 *
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class CustomJWTClaimsVerifier extends DefaultJWTClaimsVerifier<SecurityContext> {

    private final String expectedAudience;

    public CustomJWTClaimsVerifier(String expectedAudience) {
        this.expectedAudience = expectedAudience;
    }

    @Override
    public void verify(final JWTClaimsSet claimsSet, final SecurityContext context) throws BadJWTException {
        super.verify(claimsSet, context);
        verifyAudience(claimsSet);
    }

    private void verifyAudience(final JWTClaimsSet claimsSet) throws BadJWTException {
        // verify audience as well as the default "not before" and "expiration" checks
        List<String> audiences = claimsSet.getAudience();
        boolean audienceOk = false;
        for (String audience : audiences) {
            if (expectedAudience.equals(audience)) {
                audienceOk = true;
                break;
            }
        }
        if (!audienceOk) {
            throw new BadJWTException("invalid audience");
        }
    }

}
