package de.uniulm.omi.flexiant;

/**
 * Local object for the flexiant server object.
 *
 * @see net.flexiant.extility.Server
 */
public class Server {
    protected String publicIpAddress;
    protected String privateIpAddress;
    protected String serverName;
    protected String serverId;
    protected String initialPassword;
    protected String initialUser;

    public String getPublicIpAddress() {
        return publicIpAddress;
    }

    public void setPublicIpAddress(String publicIpAddress) {
        this.publicIpAddress = publicIpAddress;
    }

    public String getPrivateIpAddress() {
        return privateIpAddress;
    }

    public void setPrivateIpAddress(String privateIpAddress) {
        this.privateIpAddress = privateIpAddress;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public String getServerId() {
        return serverId;
    }

    public void setServerId(String serverId) {
        this.serverId = serverId;
    }

    public String getInitialPassword() {
        return initialPassword;
    }

    public void setInitialPassword(String initialPassword) {
        this.initialPassword = initialPassword;
    }

    public String getInitialUser() {
        return initialUser;
    }

    public void setInitialUser(String initialUser) {
        this.initialUser = initialUser;
    }
}
