package com.lv2.lv2.Service;

import com.lv2.lv2.Model.OrgRole;
import com.lv2.lv2.Model.ResourceRole;
import com.lv2.lv2.Model.User;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class UserService {

    private final Map<String, Map<ResourceRole, Long>> expirationByUser = new HashMap<>();
    private final Map<String, List<ResourceRole>> rolesByUser = new HashMap<>();

    private final Map<String, OrgRole> tempOrgRoles = new HashMap<>();
    private final Map<String, Long> tempOrgRoleExpiry = new HashMap<>();


    public void addRole(User user, ResourceRole role) {
        rolesByUser.computeIfAbsent(user.getUsername(), k -> new ArrayList<>());
        List<ResourceRole> roles = rolesByUser.get(user.getUsername());
        if (!roles.contains(role)) roles.add(role);
    }

    public void addRoleWithExpiry(User user, ResourceRole role, int seconds) {
        addRole(user, role);
        expirationByUser.computeIfAbsent(user.getUsername(), k -> new HashMap<>());
        expirationByUser.get(user.getUsername())
                .put(role, System.currentTimeMillis() + seconds * 1000L);
    }

    public void removeRole(User user, ResourceRole role) {
        List<ResourceRole> roles = rolesByUser.get(user.getUsername());
        if (roles != null) roles.remove(role);
    }

    public void removeExpiredRoles(User user) {
        Map<ResourceRole, Long> expirations = expirationByUser.get(user.getUsername());
        if (expirations == null) return;

        long now = System.currentTimeMillis();
        Iterator<Map.Entry<ResourceRole, Long>> it = expirations.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<ResourceRole, Long> entry = it.next();
            if (entry.getValue() <= now) {
                removeRole(user, entry.getKey());
                it.remove();
            }
        }
    }

    public boolean hasRole(User user, ResourceRole role) {
        removeExpiredRoles(user);
        Map<ResourceRole, Long> expirations = expirationByUser.get(user.getUsername());
        if (expirations != null && expirations.containsKey(role)) return true;
        List<ResourceRole> roles = rolesByUser.get(user.getUsername());
        return roles != null && roles.contains(role);
    }

    public List<ResourceRole> getRoles(User user) {
        removeExpiredRoles(user);
        List<ResourceRole> userRoles = rolesByUser.getOrDefault(user.getUsername(), new ArrayList<>());
        List<ResourceRole> activeRoles = new ArrayList<>();
        for (ResourceRole r : userRoles) if (hasRole(user, r)) activeRoles.add(r);
        return activeRoles;
    }

    public List<ResourceRole> getAllAvailableResourceRoles() {
        return Arrays.asList(ResourceRole.values());
    }


    public void addOrgRoleWithExpiry(User user, OrgRole role, int seconds) {
        tempOrgRoles.put(user.getUsername(), role);
        tempOrgRoleExpiry.put(user.getUsername(), System.currentTimeMillis() + seconds * 1000L);
    }

    public void removeExpiredOrgRoles(User user) {
        Long expiry = tempOrgRoleExpiry.get(user.getUsername());
        if (expiry != null && System.currentTimeMillis() > expiry) {
            tempOrgRoles.remove(user.getUsername());
            tempOrgRoleExpiry.remove(user.getUsername());
            user.setOrgRole(OrgRole.INTERN);
        }
    }

    public OrgRole getCurrentOrgRole(User user) {
        removeExpiredOrgRoles(user);
        OrgRole tempRole = tempOrgRoles.get(user.getUsername());
        return tempRole != null ? tempRole : user.getOrgRole();
    }

    public boolean hasOrgRole(User user, OrgRole role) {
        return getCurrentOrgRole(user) == role;
    }

    public void clearTempOrgRoles(User user) {
        tempOrgRoles.remove(user.getUsername());
        tempOrgRoleExpiry.remove(user.getUsername());
    }

    public List<ResourceRole> getAllowedRolesByUser(User user) {
        List<ResourceRole> allowed = new ArrayList<>();
        OrgRole currentOrgRole = getCurrentOrgRole(user);
        switch (currentOrgRole) {
            case ADMIN -> allowed.addAll(List.of(ResourceRole.values()));
            case EMPLOYEE -> {
                allowed.add(ResourceRole.DB_READER);
                allowed.add(ResourceRole.DB_WRITER);
                allowed.add(ResourceRole.FILE_UPLOADER);
                allowed.add(ResourceRole.FILE_DOWNLOADER);
            }
            case INTERN -> {
                allowed.add(ResourceRole.DB_READER);
                allowed.add(ResourceRole.FILE_DOWNLOADER);
            }
        }
        return allowed;
    }
}
