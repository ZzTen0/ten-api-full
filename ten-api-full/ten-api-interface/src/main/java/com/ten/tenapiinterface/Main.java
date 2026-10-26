package com.ten.tenapiinterface;

import com.ten.apiclientsdk.client.ApiClient;
import com.ten.apiclientsdk.model.Username;

public class Main {
    public static void main(String[] args) {
        String accessKey = "abc";
        String secretKey = "abcdefgh";
        ApiClient apiClient = new ApiClient(accessKey, secretKey);
        String r1 = apiClient.getNameByGET("你好");
        String r2 = apiClient.getNameByPOSTPath("哈哈");
        Username username = new Username();
        username.setUsername("hahahaha");
        String r3 = apiClient.getNameByPOSTJson(username);
        System.out.println(r1);
        System.out.println(r2);
        System.out.println(r3);
//        SignUtils signUtils = new SignUtils();
//        String ak = signUtils.generateAccessKey();
//
//        System.out.println(ak);
//        System.out.println();

    }
}
