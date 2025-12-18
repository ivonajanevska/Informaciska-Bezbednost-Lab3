package com.lv2.lv2.Model;

public class User {
    private String username;
    private String email;
    private String passwordHash;
    private String salt;
    private OrgRole orgRole;

    public User(String username, String email, String passwordHash, String salt, String orgRole) {
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.salt = salt;
        this.orgRole = OrgRole.valueOf(orgRole);
    }

    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getSalt() { return salt; }

    public OrgRole getOrgRole() {
        return orgRole;
    }

    public void setOrgRole(OrgRole orgRole) {
        this.orgRole = orgRole;
    }
    @Override
    public String toString() {
        return username + ";" + email + ";" + passwordHash + ";" + salt + ";" + orgRole;
    }
}
