package com.ev.charging.tool.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * MD5 工具类（短信签名生成）。
 */
public class Md5Util {

    private Md5Util() {
    }

    public static String md5Encode(String plainText) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(plainText.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) sb.append('0');
                sb.append(hex);
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 algorithm not available", e);
        }
    }

    /**
     * 生成短信签名：sign = md5(md5(phone + salt1) + salt2)
     */
    public static String generateSmsSign(String phone, String salt1, String salt2) {
        return md5Encode(md5Encode(phone + salt1) + salt2);
    }
}
