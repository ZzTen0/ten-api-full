package com.ten.tenapiinterface;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@MapperScan("com.ten.tenapiinterface.mapper")// 指定 UserMapper 所在的包路径
@ComponentScan(basePackages = {"com.ten.tenapiinterface", "com.ten.apiclientsdk"})
public class TenApiInterfaceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TenApiInterfaceApplication.class, args);
    }

}
