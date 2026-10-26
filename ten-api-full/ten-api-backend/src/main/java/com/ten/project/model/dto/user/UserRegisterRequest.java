package com.ten.project.model.dto.user;

import lombok.Data;
import lombok.ToString;

import java.io.Serializable;

/**
 * 用户注册请求体
 *
 * @author ten
 */
@Data
public class UserRegisterRequest implements Serializable {

    private static final long serialVersionUID = 3191241716373120793L;

    private String userAccount;

    @ToString.Exclude
    private String userPassword;

    @ToString.Exclude
    private String checkPassword;
}
