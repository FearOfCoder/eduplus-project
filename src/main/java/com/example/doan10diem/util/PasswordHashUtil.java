package com.example.doan10diem.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Tiện ích hash mật khẩu bằng SHA-256.
 * Lưu ý: chỉ dùng cho mục đích demo đồ án, không nên dùng trực tiếp cho hệ thống production.
 */
public final class PasswordHashUtil {

    private PasswordHashUtil() {
    }

    public static String sha256(String rawPassword) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            // Fallback: trả về mật khẩu gốc trong trường hợp rất hiếm không có SHA-256.
            return rawPassword;
        }
    }
}

