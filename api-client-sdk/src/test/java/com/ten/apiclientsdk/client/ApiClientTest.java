package com.ten.apiclientsdk.client;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import javax.annotation.Resource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ApiClientTest {

    @Resource
    private RedisTemplate<String,Object> redisTemplate;

    @Test
    void test(){
        redisTemplate.opsForValue().set("test","test");
        String test = (String) redisTemplate.opsForValue().get("test");
        System.out.println(test);
    }

}