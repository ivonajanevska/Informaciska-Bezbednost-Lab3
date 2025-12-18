package com.lv2.lv2.Controller;

import com.lv2.lv2.Model.OrgRole;
import com.lv2.lv2.Model.ResourceRole;
import com.lv2.lv2.Model.User;
import com.lv2.lv2.Service.AuthService;
import com.lv2.lv2.Service.UserService;
import com.lv2.lv2.Util.SessionManager;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

@Controller
public class UserController {

    private final AuthService authService;
    private final UserService userService;

    public UserController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    @GetMapping("/site/admin/users")
    public String viewUsers(@CookieValue(value = "session", required = false) String token, Model model) throws Exception {

        if (token == null || !SessionManager.isValid(token)) {
            return "redirect:/site/login";
        }

        String username = SessionManager.getUsername(token);
        User currentUser = authService.getUserByUsername(username);


        if (currentUser.getOrgRole() != OrgRole.ADMIN) {
            return "fail";
        }

        List<User> users = authService.loadUsers();
        model.addAttribute("users", users);

        return "admin_users";
    }

    @PostMapping("/site/admin/changeRole")
    public String changeRole(@CookieValue(value="session", required=false) String token,
                             @RequestParam String username,
                             @RequestParam String newRole) throws Exception {

        if (token == null || !SessionManager.isValid(token)) {
            return "redirect:/site/login";
        }

        User currentUser = authService.getUserByUsername(SessionManager.getUsername(token));

        if (currentUser.getOrgRole() != OrgRole.ADMIN) {
            return "fail";
        }

        User userToUpdate = authService.getUserByUsername(username);
        if (userToUpdate != null) {
            userToUpdate.setOrgRole(OrgRole.valueOf(newRole));
            authService.updateUserRole(userToUpdate);
        }

        return "redirect:/site/admin/users";
    }


    @GetMapping("/site/resources")
    public String showResources(@CookieValue(value = "session", required = false) String token,
                                Model model) throws Exception {
        if (token == null || !SessionManager.isValid(token)) {
            return "redirect:/site/login";
        }

        String username = SessionManager.getUsername(token);
        User currentUser = authService.getUserByUsername(username);
        model.addAttribute("currentUser", currentUser);

        userService.removeExpiredRoles(currentUser);
        userService.removeExpiredRoles(currentUser);

        OrgRole currentOrgRole = userService.getCurrentOrgRole(currentUser);
        model.addAttribute("currentOrgRole", currentOrgRole);

        model.addAttribute("availableResourceRoles", userService.getAllAvailableResourceRoles());

        List<ResourceRole> activeRoles = new ArrayList<>();
        for (ResourceRole r : userService.getRoles(currentUser)) {
            if (userService.hasRole(currentUser, r)) {
                activeRoles.add(r);
            }
        }
        model.addAttribute("userResourceRoles", activeRoles);

        return "resources";
    }

    @PostMapping("/site/requestResource")
    public String requestResourceAccess(@CookieValue(value = "session", required = false) String token,
                                        @RequestParam String role,
                                        RedirectAttributes redirectAttributes) throws Exception {
        if (token == null || !SessionManager.isValid(token)) {
            return "redirect:/site/login";
        }


        String username = SessionManager.getUsername(token);
        User currentUser = authService.getUserByUsername(username);

        try {
            ResourceRole requestedRole = ResourceRole.valueOf(role);
            List<ResourceRole> allowedRoles = userService.getAllowedRolesByUser(currentUser);
            if (!allowedRoles.contains(requestedRole)) {
                redirectAttributes.addFlashAttribute(
                        "errorMessage",
                        "You are not allowed to request this role: " + requestedRole
                );
                return "redirect:/site/resources";
            }



            userService.addRoleWithExpiry(currentUser, requestedRole, 30); // 30 секунди

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "You now have access to " + requestedRole + " for 30 seconds!"
            );

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Invalid resource role requested!"
            );
        }

        return "redirect:/site/resources";
    }

    @PostMapping("/site/admin/changeOrgRole")
    public String changeOrgRole(@CookieValue(value="session", required=false) String token,
                                @RequestParam String username,
                                @RequestParam String newRole,
                                @RequestParam(defaultValue = "0") int durationSeconds,
                                RedirectAttributes redirectAttributes) throws Exception {

        if (token == null || !SessionManager.isValid(token)) return "redirect:/site/login";

        User currentUser = authService.getUserByUsername(SessionManager.getUsername(token));
        if (currentUser.getOrgRole() != OrgRole.ADMIN) return "fail";

        User userToUpdate = authService.getUserByUsername(username);
        if (userToUpdate != null) {
            OrgRole role = OrgRole.valueOf(newRole.toUpperCase());

            userService.clearTempOrgRoles(userToUpdate);

            if (durationSeconds > 0) {
                userService.addOrgRoleWithExpiry(userToUpdate, role, durationSeconds);
            } else {

                userToUpdate.setOrgRole(role);
                authService.updateUserRole(userToUpdate);
            }

            redirectAttributes.addFlashAttribute("successMessage",
                    "User " + username + " now has " +
                            (durationSeconds > 0 ? "temporary" : "permanent") +
                            " role " + role + " for " + durationSeconds + " seconds!");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "User " + username + " not found!");
        }

        return "redirect:/site/admin/users";
    }
}
