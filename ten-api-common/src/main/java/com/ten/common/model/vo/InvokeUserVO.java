package com.ten.common.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 调用方用户信息（网关使用）
 */
@Data
public class InvokeUserVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户id
     */
    private Long id;

    /**
     * accessKey
     */
    private String accessKey;

    /**
     * secretKey
     */
    private String secretKey;

    /**
     * 账号
     */
    private String userAccount;

    /**
     * 用户角色: user, admin
     */
    private String userRole;
}
