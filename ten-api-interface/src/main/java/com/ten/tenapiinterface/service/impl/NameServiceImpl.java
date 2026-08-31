package com.ten.tenapiinterface.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ten.apiclientsdk.model.Username;
import com.ten.apiclientsdk.utils.SignUtils;
import com.ten.tenapiinterface.entity.User;
import com.ten.tenapiinterface.mapper.UserMapper;
import com.ten.tenapiinterface.service.NameService;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.regex.Pattern;

@Service
@Slf4j
public class NameServiceImpl implements NameService {

    private static final Pattern NONCE_PATTERN = Pattern.compile("[A-Za-z0-9_-]{16,64}");

    @Resource
    private UserMapper userMapper;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public String getName(Username username, HttpServletRequest request) {
        // 如果请求是从网关转发过来的（已经完成签名校验和调用次数扣减），直接放行
        String userIdHeader = request.getHeader("X-User-Id");
        if (StringUtils.isNotBlank(userIdHeader)) {
            log.debug("来自网关的请求，userId={}，跳过重复鉴权", userIdHeader);
            return "POST 你的名字是" + username.getUsername();
        }
        // 否则（直连调试场景）执行完整的签名校验逻辑
        String accessKey = request.getHeader("accessKey");
        String nonce = request.getHeader("nonce");
        String timestamp = request.getHeader("timestamp");
        String sign = request.getHeader("sign");
        String body = request.getHeader("body");

        if (StringUtils.isAnyBlank(accessKey, nonce, timestamp, sign)) {
            throw new RuntimeException("缺少鉴权参数");
        }
        // 去数据库查是否已分配给用户
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("access_key", accessKey);
        User user = userMapper.selectOne(wrapper);
        if (user == null) {
            throw new RuntimeException("未查询到用户信息");
        }
        String secretKey = user.getSecretKey();
        // 随机数不重复 - 修复：nonce key和SDK保持一致
        String nonceKey = "nonce:" + accessKey + ":" + nonce;
        String redisNonce = (String) redisTemplate.opsForValue().get(nonceKey);
        if (!NONCE_PATTERN.matcher(nonce).matches()) {
            throw new RuntimeException("随机数格式错误");
        }
        if (!nonce.equals(redisNonce)) {
            throw new RuntimeException("随机数不符或已使用");
        }
        // 时间和当前时间不能超过5分钟
        Long currentTime = System.currentTimeMillis() / 1000;
        if (currentTime - Long.parseLong(timestamp) > 300) {
            throw new RuntimeException("已超时");
        }
        String serverSign = SignUtils.getSign(body, secretKey);
        if (!sign.equals(serverSign)) {
            throw new RuntimeException("无权限");
        }
        // 验证通过，删除 nonce，防重放
        redisTemplate.delete(nonceKey);
        return "POST 你的名字是" + username.getUsername();
    }
}
