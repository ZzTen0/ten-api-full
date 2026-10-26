package com.ten.project.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ten.project.annotation.AuthCheck;
import com.ten.project.common.BaseResponse;
import com.ten.project.common.DeleteRequest;
import com.ten.project.constant.UserConstant;
import com.ten.project.model.dto.user.UserAddRequest;
import com.ten.project.model.dto.user.UserLoginRequest;
import com.ten.project.model.dto.user.UserQueryRequest;
import com.ten.project.model.dto.user.UserUpdateRequest;
import com.ten.project.model.entity.User;
import com.ten.project.model.vo.LoginUserVO;
import com.ten.project.model.vo.UserVO;
import com.ten.project.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void loginReturnsOwnAccessKeyWithoutSecrets() {
        User user = createUser();
        UserLoginRequest loginRequest = new UserLoginRequest();
        loginRequest.setUserAccount("test-user");
        loginRequest.setUserPassword("password123");
        when(userService.userLogin(eq("test-user"), eq("password123"), any())).thenReturn(user);

        BaseResponse<LoginUserVO> response =
                userController.userLogin(loginRequest, new MockHttpServletRequest());
        JsonNode data = objectMapper.valueToTree(response.getData());

        assertEquals(0, response.getCode());
        assertEquals("test-ak", response.getData().getAccessKey());
        assertTrue(data.has("accessKey"));
        assertFalse(data.has("secretKey"));
        assertFalse(data.has("userPassword"));
    }

    @Test
    void generalUserViewDoesNotExposeAccessKey() {
        when(userService.getById(1)).thenReturn(createUser());

        BaseResponse<UserVO> response =
                userController.getUserById(1, new MockHttpServletRequest());
        JsonNode data = objectMapper.valueToTree(response.getData());

        assertEquals(0, response.getCode());
        assertFalse(data.has("accessKey"));
        assertFalse(data.has("secretKey"));
        assertFalse(data.has("userPassword"));
    }

    @Test
    void userManagementEndpointsRequireAdminRole() throws NoSuchMethodException {
        assertAdmin("addUser", UserAddRequest.class, HttpServletRequest.class);
        assertAdmin("deleteUser", DeleteRequest.class, HttpServletRequest.class);
        assertAdmin("updateUser", UserUpdateRequest.class, HttpServletRequest.class);
        assertAdmin("getUserById", int.class, HttpServletRequest.class);
        assertAdmin("listUser", UserQueryRequest.class, HttpServletRequest.class);
        assertAdmin("listUserByPage", UserQueryRequest.class, HttpServletRequest.class);
    }

    private void assertAdmin(String methodName, Class<?>... parameterTypes) throws NoSuchMethodException {
        Method method = UserController.class.getMethod(methodName, parameterTypes);
        AuthCheck authCheck = method.getAnnotation(AuthCheck.class);
        assertNotNull(authCheck);
        assertEquals(UserConstant.ADMIN_ROLE, authCheck.mustRole());
    }

    private User createUser() {
        User user = new User();
        user.setId(1L);
        user.setUserAccount("test-user");
        user.setUserName("Test User");
        user.setUserRole("user");
        user.setAccessKey("test-ak");
        user.setSecretKey("test-sk");
        user.setUserPassword("hashed-password");
        return user;
    }
}
