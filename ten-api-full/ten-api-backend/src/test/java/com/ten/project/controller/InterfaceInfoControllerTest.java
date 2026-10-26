package com.ten.project.controller;

import com.ten.project.annotation.AuthCheck;
import com.ten.project.common.DeleteRequest;
import com.ten.project.common.IdRequest;
import com.ten.project.constant.UserConstant;
import com.ten.project.model.dto.interfaceinfo.InterfaceInfoAddRequest;
import com.ten.project.model.dto.interfaceinfo.InterfaceInfoQueryRequest;
import com.ten.project.model.dto.interfaceinfo.InterfaceInfoUpdateRequest;
import com.ten.project.service.InterfaceInfoService;
import org.junit.jupiter.api.Test;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InterfaceInfoControllerTest {

    @Test
    void marketOnlyReturnsOnlineInterfaces() {
        InterfaceInfoService service = mock(InterfaceInfoService.class);
        InterfaceInfoController controller = new InterfaceInfoController();
        org.springframework.test.util.ReflectionTestUtils.setField(controller, "interfaceinfoService", service);
        when(service.listOnlineInterfaceInfo()).thenReturn(Collections.emptyList());

        assertEquals(0, controller.listOnlineInterfaceInfo().getCode());
        verify(service).listOnlineInterfaceInfo();
    }

    @Test
    void managementEndpointsRequireAdminRole() throws NoSuchMethodException {
        assertAdmin("addInterfaceInfo", InterfaceInfoAddRequest.class, HttpServletRequest.class);
        assertAdmin("deleteInterfaceInfo", DeleteRequest.class, HttpServletRequest.class);
        assertAdmin("updateInterfaceInfo", InterfaceInfoUpdateRequest.class, HttpServletRequest.class);
        assertAdmin("getInterfaceInfoById", long.class);
        assertAdmin("listInterfaceInfo", InterfaceInfoQueryRequest.class);
        assertAdmin("listInterfaceInfoByPage", InterfaceInfoQueryRequest.class, HttpServletRequest.class);
        assertAdmin("onlineInterfaceInfo", IdRequest.class, HttpServletRequest.class);
        assertAdmin("offlineInterfaceInfo", IdRequest.class, HttpServletRequest.class);
    }

    private void assertAdmin(String methodName, Class<?>... parameterTypes) throws NoSuchMethodException {
        Method method = InterfaceInfoController.class.getMethod(methodName, parameterTypes);
        AuthCheck authCheck = method.getAnnotation(AuthCheck.class);
        assertNotNull(authCheck);
        assertEquals(UserConstant.ADMIN_ROLE, authCheck.mustRole());
    }
}
