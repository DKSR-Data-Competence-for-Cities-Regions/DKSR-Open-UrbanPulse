package de.urbanpulse.backchannel.pojos;

/**
 *
 * @author Steffen Haertlein
 */
public class UsernamePasswordCredential extends Credential {

    private String username;
    private String password;

    public UsernamePasswordCredential(String user, String pw) {
        username = user;
        password = pw;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
