package com.ten.tenapiinterface.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 工具接口统一响应。
 *
 * @param <T> 业务数据类型
 */
@Getter
@AllArgsConstructor
public class ToolResponse<T> {

    /**
     * 业务状态码，0 表示成功，40000 表示请求参数错误。
     */
    private final int code;

    /**
     * 状态说明或面向调用方的错误提示。
     */
    private final String message;

    /**
     * 业务数据，失败时可为 null。
     */
    private final T data;

    /**
     * 封装成功响应。
     *
     * @param data 业务数据
     * @param <T> 业务数据类型
     * @return 状态码为 0 的工具接口响应
     */
    public static <T> ToolResponse<T> success(T data) {
        return new ToolResponse<>(0, "success", data);
    }
}
