package com.ten.tenapiinterface.aop;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * 接口调用统计 AOP
 * 记录调用用户、耗时等日志
 */
@Aspect
@Component
@Slf4j
public class InvokeCountAOP {

    @Around("execution(* com.ten.tenapiinterface.controller.*.*(..))")
    public Object doInterceptor(ProceedingJoinPoint point) throws Throwable {
        long start = System.currentTimeMillis();
        HttpServletRequest request = null;
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                request = attributes.getRequest();
            }
        } catch (Exception e) {
            log.warn("获取HttpServletRequest失败", e);
        }

        String userId = "-";
        String method = "-";
        String path = "-";
        if (request != null) {
            // 网关透传下来的 userId
            userId = request.getHeader("X-User-Id");
            if (StringUtils.isBlank(userId)) {
                userId = request.getRemoteAddr();
            }
            method = request.getMethod();
            path = request.getRequestURI();
        }
        log.info("[INTERFACE-IN ] user={} {} {}", userId, method, path);

        Object result = point.proceed();

        long cost = System.currentTimeMillis() - start;
        log.info("[INTERFACE-OUT] user={} {} {} cost={}ms", userId, method, path, cost);
        return result;
    }
}
