package com.ten.common.security;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 开放接口 v2 签名协议工具。
 *
 * <p>由 SDK、网关和接口服务共同使用，统一绑定原始编码路径、查询串和真实请求体字节，
 * 避免不同模块对签名内容的理解不一致。</p>
 */
public final class RequestSignature {
    /**
     * 对外请求签名版本。
     */
    public static final String VERSION = "2";

    /**
     * 参与验签的请求体最大字节数，限制为 1 MiB。
     */
    public static final int MAX_BODY_BYTES = 1024 * 1024;

    /**
     * 客户端时间戳相对于服务器时间允许的最大偏移秒数。
     */
    public static final long CLOCK_SKEW_SECONDS = 300;

    /**
     * 防重放记录有效秒数。
     *
     * <p>时间戳可向未来偏移 300 秒，其完整有效窗口最长为 600 秒，因此记录保留 601 秒。</p>
     */
    public static final long NONCE_TTL_SECONDS = 2 * CLOCK_SKEW_SECONDS + 1;

    /**
     * nonce 的字符集合与长度约束。
     */
    private static final Pattern NONCE = Pattern.compile("[A-Za-z0-9_-]{16,64}");

    /**
     * 签名必须为 64 位小写十六进制字符串。
     */
    private static final Pattern HEX_SIGNATURE = Pattern.compile("[0-9a-f]{64}");

    /**
     * 工具类不允许实例化。
     */
    private RequestSignature() {
    }

    /**
     * 提取参与签名的原始路径和查询串，不解码、不重排查询参数。
     *
     * @param uri 原始请求 URI
     * @return 原始编码路径及可选查询串，空路径按根路径处理
     */
    public static String target(URI uri) {
        String path = uri.getRawPath();
        if (path == null || path.isEmpty()) path = "/";
        return path + (uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery());
    }

    /**
     * 校验 nonce 的字符及长度。
     *
     * @param nonce 本次请求使用的随机串
     * @return 是否为 16～64 位字母、数字、下划线或连字符
     */
    public static boolean validNonce(String nonce) {
        return nonce != null && NONCE.matcher(nonce).matches();
    }

    /**
     * 校验秒级时间戳是否落在允许偏移窗口内。
     *
     * @param timestamp 十进制 UNIX 秒级时间戳
     * @param now 服务器当前 UNIX 秒级时间戳
     * @return 时间戳格式及有效窗口是否均满足要求
     */
    public static boolean validTimestamp(String timestamp, long now) {
        if (timestamp == null || !timestamp.matches("[0-9]{1,12}")) return false;
        try {
            long value = Long.parseLong(timestamp);
            return value >= now - CLOCK_SKEW_SECONDS && value <= now + CLOCK_SKEW_SECONDS;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * 计算绑定完整请求的 HMAC-SHA256 签名。
     *
     * @param method HTTP 方法
     * @param target 原始编码路径及查询串
     * @param identity 对外签名使用 AK，内部签名使用网关确认的用户 ID
     * @param timestamp UNIX 秒级时间戳字符串
     * @param nonce 本次请求随机串
     * @param body 实际传输的请求体字节，无请求体时传空字节数组
     * @param secret 用户 SK 或网关内部密钥
     * @return 64 位小写十六进制签名
     * @throws IllegalArgumentException 密钥为空或签名字段包含换行
     * @throws IllegalStateException 运行环境缺少所需的密码学算法
     */
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

    /**
     * 校验签名格式并使用常量时间比较函数核对内容。
     *
     * @param expected 服务端计算的签名
     * @param supplied 请求提供的签名
     * @return 提供的签名格式合法且与服务端签名一致时返回 true
     */
    public static boolean matches(String expected, String supplied) {
        return supplied != null && HEX_SIGNATURE.matcher(supplied).matches()
                && MessageDigest.isEqual(expected.getBytes(StandardCharsets.US_ASCII),
                supplied.getBytes(StandardCharsets.US_ASCII));
    }

    /**
     * 校验内部密钥的最低配置要求。
     *
     * @param secret 内部鉴权密钥
     * @return 密钥至少 32 个字符且不使用示例占位值时返回 true
     */
    public static boolean validInternalSecret(String secret) {
        return secret != null && secret.length() >= 32 && !secret.startsWith("replace_with_");
    }

    /**
     * 拒绝规范字符串字段中的换行，避免字段边界被注入。
     *
     * @param value 待校验的签名字段
     * @throws IllegalArgumentException 字段为 null 或包含回车、换行
     */
    private static void requireSingleLine(String value) {
        if (value == null || value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0) {
            throw new IllegalArgumentException("Invalid signature field");
        }
    }

    /**
     * 将摘要或签名字节转换为小写十六进制文本。
     *
     * @param bytes 摘要或签名字节
     * @return 小写十六进制字符串
     */
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
