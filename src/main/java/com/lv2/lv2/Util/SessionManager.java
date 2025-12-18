package com.lv2.lv2.Util;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SessionManager {
    private static Map<String, String> sessions = new HashMap<>();

    public static String createSession(String username) {
        String token = UUID.randomUUID().toString();
        sessions.put(token, username);
        return token;
    }

    public static boolean isValid(String token) {
        return token != null && sessions.containsKey(token);
    }

    public static void removeSession(String token) {
        sessions.remove(token);
    }

    public static String getUsername(String token) {
        return sessions.get(token);
    }
}