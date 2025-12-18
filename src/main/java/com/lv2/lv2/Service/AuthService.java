package com.lv2.lv2.Service;

import com.lv2.lv2.Model.OrgRole;
import com.lv2.lv2.Model.User;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.*;

@Service
public class AuthService {

    private static final String USER_FILE = "users.txt";
    private static final Map<String, String> verificationCodes = new HashMap<>();


    public AuthService() throws Exception {
        initializeDefaultAdmin();
    }

    public void updateUserRole(User user) throws Exception {
        List<User> users = loadUsers();
        try (FileWriter fw = new FileWriter(USER_FILE)) {
            for (User u : users) {
                if (u.getUsername().equals(user.getUsername())) {
                    fw.write(user.toString() + "\n");
                } else {
                    fw.write(u.toString() + "\n");
                }
            }
        }
    }
    public void initializeDefaultAdmin() throws Exception {
        List<User> users = loadUsers();


        boolean adminExists = users.stream()
                .anyMatch(u -> u.getUsername().equalsIgnoreCase("admin"));

        if (!adminExists) {
            String username = "admin";
            String email = "admin@example.com";
            String password = "admin123";
            String salt = generateSalt();
            String hashed = hashPassword(password, salt);
            User admin = new User(username, email, hashed, salt, OrgRole.ADMIN.name());

            try (FileWriter fw = new FileWriter(USER_FILE, true)) {
                fw.write(admin.toString() + "\n");
            }

            System.out.println("Default admin created: username=admin, password=admin123");
        }
    }
    public boolean isValidEmail(String email) {
        String regex = "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";
        System.out.println("validen");
        return email.matches(regex);
    }

    public boolean registerStep1(String username, String email, String password) throws Exception {
        if (!isValidEmail(email)) {
            System.out.println("Invalid email: " + email);
            return false;
        }
        if (userExists(username, email)) return false;
        String code = generateVerificationCode();
        verificationCodes.put(email, username + ";" + password + ";" + code);

        System.out.println("Verification code for " + email + ": " + code);
        return true;
    }


    public boolean registerStep2(String email, String code) throws Exception {
        if (!verificationCodes.containsKey(email)) return false;
        String[] parts = verificationCodes.get(email).split(";");
        String username = parts[0];
        String password = parts[1];
        String savedCode = parts[2];
        if (!savedCode.equals(code)) return false;

        String salt = generateSalt();
        String hashed = hashPassword(password, salt);
        try (FileWriter fw = new FileWriter(USER_FILE, true)) {
            fw.write(username + ";" + email + ";" + hashed + ";" + salt + ";" + OrgRole.INTERN.name() + "\n");
        }
        verificationCodes.remove(email);
        return true;
    }

    public boolean loginStep1(String username, String password) throws Exception {

        User user = getUserByUsername(username);
        if (user == null) return false;
        String hashed = hashPassword(password, user.getSalt());
        return user.getPasswordHash().equals(hashed);
    }

    public String generate2FACode(String username) {
        String code = String.format("%06d", new Random().nextInt(999999));
        verificationCodes.put(username, code);
        System.out.println("2FA code for " + username + ": " + code);
        return code;
    }

    public boolean verify2FACode(String username, String code) {
        return code.equals(verificationCodes.get(username));
    }

    public boolean userExists(String username, String email) throws Exception {
        return getUserByUsername(username) != null || getUserByEmail(email) != null;
    }

    public User getUserByUsername(String username) throws Exception {
        for (User u : loadUsers()) { // <-- loadUsers() е private
            if (u.getUsername().equals(username)) return u;
        }
        return null;
    }

    public User getUserByEmail(String email) throws Exception {
        for (User u : loadUsers()) {
            if (u.getEmail().equals(email)) return u;
        }
        return null;
    }

    public List<User> loadUsers() throws Exception {
        List<User> users = new ArrayList<>();
        File f = new File(USER_FILE);
        if (!f.exists()) return users;
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(";");
                if (parts.length == 5) users.add(new User(parts[0], parts[1], parts[2], parts[3], parts[4]));
            }
        }
        return users;
    }

    private String hashPassword(String password, String salt) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest((password + salt).getBytes("UTF-8"));
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    private String generateSalt() {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    private String generateVerificationCode() {
        return String.format("%06d", new Random().nextInt(999999));
    }
}
