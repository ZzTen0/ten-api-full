package com.ten.tenapiinterface.service.impl;

import com.ten.apiclientsdk.model.Username;
import com.ten.tenapiinterface.service.NameService;
import org.springframework.stereotype.Service;
import javax.servlet.http.HttpServletRequest;

@Service
public class NameServiceImpl implements NameService {
    @Override
    public String getName(Username username, HttpServletRequest request) {
        // GatewayAuthenticationFilter authenticates every interface endpoint before controller execution.
        return "POST 你的名字是" + username.getUsername();
    }
}
