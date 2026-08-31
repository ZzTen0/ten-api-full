package com.ten.common.service;

import com.ten.common.model.vo.InvokeUserVO;

/**
 * 内部用户服务（Dubbo）
 * 提供给网关调用
 */
public interface InnerUserService {

    /**
     * 根据 accessKey 查询调用用户信息（包含 secretKey）
     *
     * @param accessKey accessKey
     * @return 用户信息，未找到返回 null
     */
    InvokeUserVO getInvokeUser(String accessKey);
}
