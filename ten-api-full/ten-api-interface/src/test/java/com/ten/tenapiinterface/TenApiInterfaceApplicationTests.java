package com.ten.tenapiinterface;

import com.ten.apiclientsdk.client.ApiClient;
import com.ten.apiclientsdk.model.Username;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest
class TenApiInterfaceApplicationTests {

    @Resource
    private ApiClient apiClient;

    @Test
    void contextLoads() {
        String nameByGET = apiClient.getNameByGET("abc");
        Username username = new Username();
        username.setUsername("efg");
        String nameByPOSTJson = apiClient.getNameByPOSTJson(username);
        System.out.println(nameByGET);
        System.out.println(nameByPOSTJson);

    }

}
