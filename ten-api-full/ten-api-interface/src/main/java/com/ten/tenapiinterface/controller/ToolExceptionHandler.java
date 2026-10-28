package com.ten.tenapiinterface.controller;

import com.ten.tenapiinterface.model.ToolResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import java.time.DateTimeException;

/**
 * 工具接口异常处理器。
 *
 * <p>仅作用于 ToolController，将参数、绑定和日期计算错误统一返回为 HTTP 400，
 * 不向调用方回显原始请求文本或底层解析异常详情。</p>
 */
@RestControllerAdvice(assignableTypes = ToolController.class)
public class ToolExceptionHandler {
    /**
     * 处理工具服务主动抛出的参数校验异常。
     *
     * @param exception 含受控错误说明的参数异常
     * @return HTTP 400 及统一错误响应
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ToolResponse<Void>> invalid(IllegalArgumentException exception) {
        return error(exception.getMessage());
    }

    /**
     * 处理请求模型的 Bean Validation 校验失败。
     *
     * @param exception 请求参数校验异常
     * @return 提示首个错误字段的 HTTP 400 响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ToolResponse<Void>> validation(MethodArgumentNotValidException exception) {
        String field = exception.getBindingResult().getFieldErrors().isEmpty() ? "请求"
                : exception.getBindingResult().getFieldErrors().get(0).getField();
        return error(field + " 参数无效，请检查必填项、长度或取值范围");
    }

    /**
     * 处理无效日期、时区及计算溢出，统一隐藏底层异常细节。
     *
     * @param exception 日期或算术异常
     * @return HTTP 400 及时间计算错误说明
     */
    @ExceptionHandler({DateTimeException.class, ArithmeticException.class})
    public ResponseEntity<ToolResponse<Void>> date(Exception exception) {
        return error("日期、时间戳或时区无效，或计算结果超出支持范围");
    }

    /**
     * 处理请求体解析、查询参数类型及必填参数绑定错误。
     *
     * @param exception 请求绑定异常
     * @return HTTP 400 及通用格式错误说明
     */
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class})
    public ResponseEntity<ToolResponse<Void>> binding(Exception exception) {
        return error("请求格式或参数类型无效，请检查必填参数及 JSON 格式");
    }

    /**
     * 封装不包含业务数据的参数错误响应。
     *
     * @param message 面向调用方的错误说明
     * @return HTTP 400，业务码为 40000 的响应
     */
    private ResponseEntity<ToolResponse<Void>> error(String message) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ToolResponse<>(40000, message, null));
    }
}
