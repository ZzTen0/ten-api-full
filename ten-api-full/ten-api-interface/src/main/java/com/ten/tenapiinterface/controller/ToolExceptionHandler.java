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

@RestControllerAdvice(assignableTypes = ToolController.class)
public class ToolExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ToolResponse<Void>> invalid(IllegalArgumentException exception) {
        return error(exception.getMessage());
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ToolResponse<Void>> validation(MethodArgumentNotValidException exception) {
        String field = exception.getBindingResult().getFieldErrors().isEmpty() ? "请求"
                : exception.getBindingResult().getFieldErrors().get(0).getField();
        return error(field + " 参数无效，请检查必填项、长度或取值范围");
    }
    @ExceptionHandler({DateTimeException.class, ArithmeticException.class})
    public ResponseEntity<ToolResponse<Void>> date(Exception exception) {
        return error("日期、时间戳或时区无效，或计算结果超出支持范围");
    }
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class})
    public ResponseEntity<ToolResponse<Void>> binding(Exception exception) {
        return error("请求格式或参数类型无效，请检查必填参数及 JSON 格式");
    }
    private ResponseEntity<ToolResponse<Void>> error(String message) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ToolResponse<>(40000, message, null));
    }
}
