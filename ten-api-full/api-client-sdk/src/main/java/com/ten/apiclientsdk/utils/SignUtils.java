package com.ten.apiclientsdk.utils;

import cn.hutool.core.util.IdUtil;
import com.ten.common.security.RequestSignature;

/**
 * SDK 密钥生成及请求签名工具。
 *
 * <p>签名计算委托给公共模块，确保客户端与服务端使用同一份 v2 协议实现。</p>
 */
public class SignUtils {

    /**
     * 生成访问密钥 AK。
     *
     * @return 不带连字符的 UUID 字符串
     */
    public String generateAccessKey(){
        return IdUtil.simpleUUID();
    }

    /**
     * 生成签名密钥 SK。
     *
     * @return 不带连字符的 UUID 字符串
     */
    public String generateSecretKey(){
        return IdUtil.simpleUUID();
    }

    /**
     * 计算开放接口 v2 请求签名。
     *
     * @param method HTTP 方法
     * @param target 原始编码路径及查询串
     * @param accessKey 用户访问密钥
     * @param timestamp UNIX 秒级时间戳字符串
     * @param nonce 本次请求随机串
     * @param body 实际传输的请求体字节
     * @param secretKey 用户签名密钥
     * @return 小写十六进制 HMAC-SHA256 签名
     */
    public static String getSign(String method, String target, String accessKey, String timestamp,
                                 String nonce, byte[] body, String secretKey) {
        return RequestSignature.sign(method, target, accessKey, timestamp, nonce, body, secretKey);
    }


}
