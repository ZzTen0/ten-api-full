package com.ten.common.service;

/**
 * 内部用户-接口调用关系服务（Dubbo）
 * 提供给网关调用
 */
public interface InnerUserInterfaceInfoService {

    /**
     * 调用接口后扣减次数
     *
     * @param userId      用户id
     * @param interfaceId 接口id
     * @return 是否扣减成功
     */
    boolean invokeCount(long userId, long interfaceId);

    /**
     * 校验用户是否有剩余调用次数
     *
     * @param userId      用户id
     * @param interfaceId 接口id
     * @return 是否有可用次数
     */
    boolean hasLeftNum(long userId, long interfaceId);
}
