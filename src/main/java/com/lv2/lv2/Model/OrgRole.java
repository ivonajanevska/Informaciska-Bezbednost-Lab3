package com.lv2.lv2.Model;

public enum OrgRole {
    INTERN,
    EMPLOYEE,
    ADMIN;

    public boolean canOrgAccess(OrgRole orgRole)
    {
        return this.ordinal()>= orgRole.ordinal();
    }

}
