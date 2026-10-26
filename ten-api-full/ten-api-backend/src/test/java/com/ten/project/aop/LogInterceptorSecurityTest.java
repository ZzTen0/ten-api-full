package com.ten.project.aop;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.ten.project.model.dto.user.UserLoginRequest;
import com.ten.project.model.dto.user.UserRegisterRequest;
import com.ten.project.model.dto.interfaceinfo.InterfaceInfoInvokeRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LogInterceptorSecurityTest {
    @Test void neverStringifiesArgumentsAndLogsCompletionOnFailure() throws Throwable {
        Logger logger = (Logger) LoggerFactory.getLogger(LogInterceptor.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/user/login");
            RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
            ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
            when(joinPoint.proceed()).thenThrow(new IllegalStateException("failed"));
            assertThrows(IllegalStateException.class, () -> new LogInterceptor().doInterceptor(joinPoint));
            verify(joinPoint, never()).getArgs();
            assertTrue(appender.list.stream().anyMatch(event -> event.getFormattedMessage().contains("request end")));
            assertTrue(appender.list.stream().noneMatch(event -> event.getFormattedMessage().contains("params:")));
        } finally {
            logger.detachAppender(appender);
            RequestContextHolder.resetRequestAttributes();
        }
    }

    @Test void credentialsAreExcludedFromDtoToString() {
        UserLoginRequest login = new UserLoginRequest();
        login.setUserPassword("sensitive-login-password");
        UserRegisterRequest register = new UserRegisterRequest();
        register.setUserPassword("sensitive-register-password");
        register.setCheckPassword("sensitive-confirm-password");
        assertFalse(login.toString().contains("sensitive"));
        assertFalse(register.toString().contains("sensitive"));
        InterfaceInfoInvokeRequest invocation = new InterfaceInfoInvokeRequest();
        invocation.setUserRequestParams("sensitive-tool-input");
        assertFalse(invocation.toString().contains("sensitive"));
    }
}
