package com.ten.apiclientsdk.utils;

import cn.hutool.core.util.IdUtil;
import cn.hutool.crypto.digest.DigestAlgorithm;
import cn.hutool.crypto.digest.Digester;

public class SignUtils {

    public String generateAccessKey(){
        return IdUtil.simpleUUID();
    }

    public String generateSecretKey(){
        return IdUtil.simpleUUID();
    }

    public static String getSign(String body,String secretKey){
        Digester digester = new Digester(DigestAlgorithm.SHA256);
        String content = body + "." + secretKey;
        return digester.digestHex(content);
    }


}
