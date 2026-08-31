package com.ten.project.model.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 当前登录用户视图。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class LoginUserVO extends UserVO {

    /**
     * 当前用户自己的 AccessKey。AccessKey 用于标识调用方，不属于签名密钥。
     */
    private String accessKey;

    private static final long serialVersionUID = 1L;
}
