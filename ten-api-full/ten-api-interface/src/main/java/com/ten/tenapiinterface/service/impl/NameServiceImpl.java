package com.ten.tenapiinterface.service.impl;

import com.ten.apiclientsdk.model.Username;
import com.ten.tenapiinterface.service.NameService;
import org.springframework.stereotype.Service;
import javax.servlet.http.HttpServletRequest;

/**
 * 名字示例接口业务实现。
 *
 * <p>入口鉴权由统一过滤器完成，业务层只生成示例响应。</p>
 */
@Service
public class NameServiceImpl implements NameService {
    /**
     * 生成用户名示例响应。
     *
     * @param username 用户名请求对象
     * @param request 已通过网关身份过滤器校验的 HTTP 请求
     * @return 包含用户名的示例文本
     */
    @Override
    public String getName(Username username, HttpServletRequest request) {
        // GatewayAuthenticationFilter 已在控制器执行前统一验证业务入口的网关身份。
        return "POST 你的名字是" + username.getUsername();
    }
}
