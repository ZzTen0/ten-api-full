package com.ten.common.security;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.regex.Pattern;

/** Shared v2 protocol: raw encoded request target and exact body bytes. */
public final class RequestSignature {
    public static final String VERSION = "2";
    public static final int MAX_BODY_BYTES = 1024 * 1024;
    public static final long CLOCK_SKEW_SECONDS = 300;
    // A future-dated request remains valid for twice the clock skew.
    public static final long NONCE_TTL_SECONDS = 2 * CLOCK_SKEW_SECONDS + 1;
    private static final Pattern NONCE = Pattern.compile("[A-Za-z0-9_-]{16,64}");
    private static final Pattern HEX_SIGNATURE = Pattern.compile("[0-9a-f]{64}");

    private RequestSignature() { }

    public static String target(URI uri) {
        String path = uri.getRawPath();
        if (path == null || path.isEmpty()) path = "/";
        return path + (uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery());
    }

    public static boolean validNonce(String nonce) {
        return nonce != null && NONCE.matcher(nonce).matches();
    }

    public static boolean validTimestamp(String timestamp, long now) {
        if (timestamp == null || !timestamp.matches("[0-9]{1,12}")) return false;
        try {
            long value = Long.parseLong(timestamp);
            return value >= now - CLOCK_SKEW_SECONDS && value <= now + CLOCK_SKEW_SECONDS;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static String sign(String method, String target, String identity, String timestamp,
                              String nonce, byte[] body, String secret) {
        if (secret == null || secret.isEmpty()) throw new IllegalArgumentException("Missing signing secret");
        requireSingleLine(method);
        requireSingleLine(target);
        requireSingleLine(identity);
        requireSingleLine(timestamp);
        requireSingleLine(nonce);
        try {
            String canonical = "TEN-API-V2\n" + method.toUpperCase(Locale.ROOT) + "\n" + target
                    + "\n" + identity + "\n" + timestamp + "\n" + nonce + "\n"
                    + hex(MessageDigest.getInstance("SHA-256").digest(body));
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return hex(mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Signing algorithm unavailable", e);
        }
    }

    public static boolean matches(String expected, String supplied) {
        return supplied != null && HEX_SIGNATURE.matcher(supplied).matches()
                && MessageDigest.isEqual(expected.getBytes(StandardCharsets.US_ASCII),
                supplied.getBytes(StandardCharsets.US_ASCII));
    }

    public static boolean validInternalSecret(String secret) {
        return secret != null && secret.length() >= 32 && !secret.startsWith("replace_with_");
    }

    private static void requireSingleLine(String value) {
        if (value == null || value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0) {
            throw new IllegalArgumentException("Invalid signature field");
        }
    }

    private static String hex(byte[] bytes) {
        char[] digits = "0123456789abcdef".toCharArray();
        char[] output = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            output[i * 2] = digits[(bytes[i] & 0xff) >>> 4];
            output[i * 2 + 1] = digits[bytes[i] & 15];
        }
        return new String(output);
    }
}
