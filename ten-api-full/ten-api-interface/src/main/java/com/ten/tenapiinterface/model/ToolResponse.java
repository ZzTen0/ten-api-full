package com.ten.tenapiinterface.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ToolResponse<T> {
    private final int code;
    private final String message;
    private final T data;

    public static <T> ToolResponse<T> success(T data) { return new ToolResponse<>(0, "success", data); }
}
