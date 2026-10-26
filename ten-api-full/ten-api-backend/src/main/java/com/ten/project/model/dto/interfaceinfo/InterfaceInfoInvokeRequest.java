package com.ten.project.model.dto.interfaceinfo;

import lombok.Data;
import lombok.ToString;

import java.io.Serializable;

/**
 * 更新请求
 *
 * @TableName product
 */
@Data
public class InterfaceInfoInvokeRequest implements Serializable {

    /**
     * 主键
     */
    private Long id;

    /**
     * 请求参数
     */
    @ToString.Exclude
    private String userRequestParams;

    private static final long serialVersionUID = 1L;
}
