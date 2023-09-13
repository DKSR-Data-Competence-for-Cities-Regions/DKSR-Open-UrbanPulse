package de.urbanpulse.dist.jee.upsecurityrealm.oidc.helpers;

import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import de.urbanpulse.dist.jee.upsecurityrealm.hmac.HmacToken;
import de.urbanpulse.dist.jee.upsecurityrealm.oidc.OidcToken;
import io.vertx.core.json.JsonObject;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.pac4j.core.credentials.TokenCredentials;
import org.pac4j.oidc.authorization.generator.KeycloakRolesAuthorizationGenerator;
import org.pac4j.oidc.credentials.authenticator.UserInfoOidcAuthenticator;
import org.pac4j.oidc.profile.keycloak.KeycloakOidcProfile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 *
 * @author Iliya Bahchevanski <iliya.bahchevanski@the-urban-institute.de>
 */
public class Pac4jSecurityHelper {
    private static final Logger LOGGER = Logger.getLogger(Pac4jSecurityHelper.class.getName());

    private CloseableHttpClient httpClient;

    public Pac4jSecurityHelper(){
        httpClient = HttpClients.createDefault();
    }

    public Set<String> createOidcProfileAndExtractRolesFromToken(TokenCredentials tokenCredentials) {
        Set<String> userRoles = new HashSet<>();

        //if we would need the roles that are also only specific for this client
        //we would not need to supply the clientId in here
        KeycloakRolesAuthorizationGenerator keycloakRolesAuthorizationGenerator
                = new KeycloakRolesAuthorizationGenerator();

        KeycloakOidcProfile keycloakOidcProfile = new KeycloakOidcProfile();
        AccessToken accessToken1 = new BearerAccessToken(tokenCredentials.getToken());
        keycloakOidcProfile.setAccessToken(accessToken1);

        keycloakRolesAuthorizationGenerator.generate(null, keycloakOidcProfile)
                .ifPresent(userProfile -> userRoles.addAll(userProfile.getRoles()));

        return userRoles;
    }

    public TokenCredentials createTokenCredentialsFromShiroAuthToken(AuthenticationToken token, String url) {
        OidcToken oidcToken = null;
        TokenCredentials tokenCredentials = null;

        if (token instanceof UsernamePasswordToken) {
            //Unfortunately we cant use userInfoOidcAuthenticator because it does not have a way to
            //create an oidc token from a username and password. So we use our internal logic (/login)
            //to create an access_token using the username and password.
            UsernamePasswordToken userPassToken = (UsernamePasswordToken) token;
            String username = userPassToken.getUsername();
            char[] password = userPassToken.getPassword();

            if ((username != null && !username.isEmpty()) || (password != null && password.length > 0)) {
                oidcToken = createOidcTokenForUser(username,new String(password), url);
            } else {
                LOGGER.warning("Username or password is null!");
                return null;
            }
        } else {
            oidcToken = (OidcToken) token;
        }

        if (oidcToken != null) {
            String accessToken = (String) oidcToken.getCredentials();
            tokenCredentials = new TokenCredentials(accessToken);
        }

        return tokenCredentials;
    }

    public OidcToken createOidcTokenForUser(String username, String password, String url) {
        OidcToken oidcToken = null;
        String strUsernameColonPw = username + ":" + password;
        String authorizationHeader = "Basic " + new String(Base64.encodeBase64(strUsernameColonPw.getBytes()));

        HttpGet request = new HttpGet(url);
        request.addHeader("Authorization", authorizationHeader);

        oidcToken = executeTokenRequest(oidcToken,request);

        return oidcToken;
    }

    public OidcToken createOidcTokenWithClientSecret(String clientId, String clientSecret, String url) throws IOException {
        OidcToken oidcToken = null;
        HttpPost httpPost = new HttpPost(url);

        List<NameValuePair> params = new ArrayList<>(3);
        params.add(new BasicNameValuePair("client_id", clientId));
        params.add(new BasicNameValuePair("client_secret", clientSecret));
        params.add(new BasicNameValuePair("grant_type", "client_credentials"));
        httpPost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
        httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded");

        oidcToken = executeTokenRequest(oidcToken, httpPost);
        return oidcToken;
    }


    public boolean validateToken(String url, String token){
        HttpGet httpGet = new HttpGet(url);
        httpGet.addHeader("Authorization","Bearer " + token);
        try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
            return response.getStatusLine().getStatusCode() == 200;
        } catch (IOException e) {
            LOGGER.fine(e.getMessage());
            return false;
        }
    }

    public SimpleAuthenticationInfo createAuthInfoAndValidateToken(TokenCredentials tokenCredentials,
            String realmName,UserInfoOidcAuthenticator userInfoOidcAuthenticator) {

        if (tokenCredentials == null){
            return null;
        }

        SimpleAuthenticationInfo simpleAuth = new SimpleAuthenticationInfo();
        try {
            userInfoOidcAuthenticator.validate(tokenCredentials, null);

            List<Object> principalsList = new ArrayList<>();
            //this will be used later in doGetAuthorizationInfo to extract the roles out of the token
            principalsList.add(tokenCredentials);
            //this will be used to get the roles of the user from shiro based on the username
            principalsList.add(tokenCredentials.getUserProfile().getAttribute("preferred_username"));

            SimplePrincipalCollection simplePrincipalCollection = new SimplePrincipalCollection(principalsList, realmName);
            simpleAuth.setPrincipals(simplePrincipalCollection);
            simpleAuth.setCredentials(tokenCredentials.getToken());
        } catch (Exception e) {
            throw new IncorrectCredentialsException(e.getMessage());
        }

        return simpleAuth;
    }

    public boolean isTokenSupported(AuthenticationToken token){
        //HmacToken is of type UsernamePasswordToken
       return ((token instanceof OidcToken) || (token instanceof UsernamePasswordToken) && !(token instanceof HmacToken));
    }

    private OidcToken executeTokenRequest(OidcToken oidcToken, HttpRequestBase request) {
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            if (response.getStatusLine().getStatusCode() == 200) {
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    String reader = EntityUtils.toString(entity);
                    JsonObject responseObject = new JsonObject(reader);
                    oidcToken = new OidcToken(responseObject.getString("access_token"));
                }
            }
        } catch (IOException e) {
            LOGGER.fine(e.getMessage());
        }
        return oidcToken;
    }
}
