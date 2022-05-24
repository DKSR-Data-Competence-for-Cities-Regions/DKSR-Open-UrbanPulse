package de.urbanpulse.dist.jee.upsecurityrealm.jwt;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.RemoteJWKSet;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.util.DefaultResourceRetriever;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import io.vertx.core.json.JsonObject;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * validates OAuth2 access tokens in JWT format (self-contained tokens)
 * <P>
 * the following is checked: token signature, expiration date, "not before" date and audience
 * <p>
 * supports these JNDI config vars:
 * <li>
 * oauthDemo/keyUrl => URL from which to retrieve the public keys of the auth server, defaults to https://login.microsoftonline.com/oauthtest.onmicrosoft.com/discovery/v2.0/keys
 * </li>
 * <li>
 * oauthDemo/expectedAudience => ID URI of the resource app, defaults to https://oauthtest.onmicrosoft.com/RscApi
 * </li>
 *
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class AccessTokenValidator {

    private static final Logger LOGGER = Logger.getLogger(AccessTokenValidator.class.getName());
    private static final int KEY_RETRIEVAL_TIMEOUT = 3000;
    private final ConfigurableJWTProcessor jwtProcessor;
    private final JWKSource keySource;
    private boolean verboseLogging;

    public AccessTokenValidator() {
        try {
            verboseLogging = InitialContext.doLookup("keycloak/verboseLogging");
        } catch (NamingException ex) {
            verboseLogging = false;
        }

        if (verboseLogging) {
            LOGGER.warning("verbose logging for OAuth enabled - decoded tokens will be logged!");
        }

        String keyUrl;
        try {
            keyUrl = InitialContext.doLookup("keycloak/keyUrl2");
        } catch (NamingException ex) {
            keyUrl = "https://login.microsoftonline.com/oauthtest.onmicrosoft.com/discovery/v2.0/keys";
        }

        String expectedAudience;
        try {
            expectedAudience = InitialContext.doLookup("keycloak/expectedAudience2");
        } catch (NamingException ex) {
            expectedAudience = "https://oauthtest.onmicrosoft.com/InnerRsc";
        }

        jwtProcessor = new DefaultJWTProcessor();
        try {
            DefaultResourceRetriever resourceRetriever = new DefaultResourceRetriever(KEY_RETRIEVAL_TIMEOUT, KEY_RETRIEVAL_TIMEOUT);
            keySource = new RemoteJWKSet(new URL(keyUrl), resourceRetriever);
        } catch (MalformedURLException ex) {
            throw new IllegalStateException(ex);
        }
        JWSAlgorithm expectedJWSAlg = JWSAlgorithm.RS256;
        JWSKeySelector keySelector = new JWSVerificationKeySelector(expectedJWSAlg, keySource);
        jwtProcessor.setJWSKeySelector(keySelector);
        jwtProcessor.setJWTClaimsSetVerifier(new CustomJWTClaimsVerifier(expectedAudience));
    }

    public JWTClaimsSet verifyAndReturnToken(String token) {
        if (token == null) {
            return null;
        }

        try {
            JWTClaimsSet claimsSet = jwtProcessor.process(token, null);

            if (verboseLogging) {
                JsonObject decocdedToken = new JsonObject(claimsSet.toJSONObject().toString());
                LOGGER.log(Level.INFO, "OAuth ok: {0}", decocdedToken.encodePrettily());
            } else {
                LOGGER.info("OAuth ok");
            }

            return claimsSet;
        } catch (RuntimeException | ParseException | BadJOSEException | JOSEException ex) {
            if (verboseLogging) {
                LOGGER.log(Level.SEVERE, "OAuth failed: {0}", ex.getMessage());
            } else {
                LOGGER.log(Level.SEVERE, "OAuth failed", ex);
            }
            return null;
        }
    }

}
