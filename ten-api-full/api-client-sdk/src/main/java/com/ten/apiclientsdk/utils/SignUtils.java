package com.ten.apiclientsdk.utils;

import cn.hutool.core.util.IdUtil;
import com.ten.common.security.RequestSignature;

public class SignUtils {

    public String generateAccessKey(){
        return IdUtil.simpleUUID();
    }

    public String generateSecretKey(){
        return IdUtil.simpleUUID();
    }

    public static String getSign(String method, String target, String accessKey, String timestamp,
                                 String nonce, byte[] body, String secretKey) {
        return RequestSignature.sign(method, target, accessKey, timestamp, nonce, body, secretKey);
    }


}
