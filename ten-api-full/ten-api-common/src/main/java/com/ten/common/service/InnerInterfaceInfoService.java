package com.ten.common.service;

/**
 * 内部接口信息服务（Dubbo）。
 *
 * <p>供网关按请求方法和路径解析可调用的已上线接口。</p>
 */
public interface InnerInterfaceInfoService {
    /**
     * 解析已上线接口的标识。
     *
     * @param method HTTP 方法
     * @param path 不包含查询串的原始网关路径
     * @return 已上线接口 ID，未注册或已下线时返回 null
     */
    Long resolveOnlineInterfaceId(String method, String path);
}
