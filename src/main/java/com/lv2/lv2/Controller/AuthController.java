package com.lv2.lv2.Controller;


import com.lv2.lv2.Service.AuthService;
import com.lv2.lv2.Service.UserService;
import com.lv2.lv2.Util.SessionManager;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.ui.Model;



@Controller
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    @Autowired
    public AuthController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }


    @GetMapping("/site/register")
    public String showRegisterPage() {
        return "register";
    }

    @PostMapping("/site/register")
    public String doRegisterStep1(@RequestParam String username,
                                  @RequestParam String email,
                                  @RequestParam String password) throws Exception {
        if (authService.registerStep1(username, email, password)) {
            return "redirect:/site/register2?email=" + email;
        }
        return "fail";
    }

    @GetMapping("/site/register2")
    public String showRegisterStep2Page(@RequestParam String email,
                                        @RequestParam(required = false) String error,
                                        Model model) {
        model.addAttribute("email", email);
        model.addAttribute("error", error);
        return "register2";
    }

    @PostMapping("/site/register2")
    public String doRegisterStep2(@RequestParam String email,
                                  @RequestParam("code") String code) throws Exception {
        boolean success = authService.registerStep2(email, code);
        System.out.println("Регистрација Step2: email=" + email + ", code=" + code + ", success=" + success);
        if (success) {
            return "redirect:/site/login";
        }
        return "fail";
    }

    @GetMapping("/site/login")
    public String showLoginPage() {
        return "login";
    }

    @PostMapping("/site/login")
    public String doLoginStep1(@RequestParam String username,
                               @RequestParam String password) throws Exception {
        if (authService.loginStep1(username, password)) {
            authService.generate2FACode(username);
            return "redirect:/site/login2?username=" + username;
        }
        return "fail";
    }

    @GetMapping("/site/login2")
    public String showLoginStep2Page(@RequestParam String username,
                                     @RequestParam(required = false) String error,
                                     Model model) {
        model.addAttribute("username", username);
        model.addAttribute("error", error);
        return "login2";
    }

    @PostMapping("/site/login2")
    public String doLoginStep2(@RequestParam("username") String username,
                               @RequestParam("code") String code,
                               HttpServletResponse response) {
        if (authService.verify2FACode(username, code)) {
            String token = SessionManager.createSession(username);
            Cookie cookie = new Cookie("session", token);
            cookie.setHttpOnly(true);
            cookie.setPath("/");
            response.addCookie(cookie);
            return "redirect:/site/dashboard";
        }
        return "fail";
    }

    @GetMapping("/site/dashboard")
    public String dashboard(@CookieValue(value = "session", required = false) String token) {
        if (token != null && SessionManager.isValid(token)) {
            return "dashboard";
        }
        return "redirect:/site/login";
    }

    @GetMapping("/site/logout")
    public String logout(@CookieValue(value = "session", required = false) String token,
                         HttpServletResponse response) {
        if (token != null) {
            SessionManager.removeSession(token);
            Cookie cookie = new Cookie("session", null);
            cookie.setMaxAge(0);
            cookie.setPath("/");
            response.addCookie(cookie);
        }
        return "redirect:/site/login";
    }



}
