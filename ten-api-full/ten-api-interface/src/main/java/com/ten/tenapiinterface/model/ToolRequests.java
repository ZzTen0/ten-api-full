package com.ten.tenapiinterface.model;

import lombok.Data;
import lombok.ToString;
import javax.validation.constraints.*;
import java.math.BigDecimal;

/**
 * 内置工具接口请求模型。
 *
 * <p>按工具类型集中定义参数、默认值和基础校验约束，操作相关的条件校验由工具服务完成。</p>
 */
public final class ToolRequests {

    /**
     * 仅用于组织请求类型，不允许实例化外层容器。
     */
    private ToolRequests() {
    }

    /**
     * 随机密码生成请求，至少需要启用一种字符类型。
     */
    @Data
    public static class Password {

        /**
         * 密码长度，范围为 4～128，默认 16。
         */
        @Min(4)
        @Max(128)
        private int length = 16;

        /**
         * 是否包含小写英文字母，默认 true。
         */
        private boolean lowercase = true;

        /**
         * 是否包含大写英文字母，默认 true。
         */
        private boolean uppercase = true;

        /**
         * 是否包含数字，默认 true。
         */
        private boolean digits = true;

        /**
         * 是否包含特殊字符，默认 false。
         */
        private boolean symbols = false;
    }

    /**
     * 时间戳转换请求。
     */
    @Data
    public static class Timestamp {

        /**
         * 转换方向：toTimestamp 转时间戳，toDateTime 转日期时间，默认 toTimestamp。
         */
        @NotBlank
        private String operation = "toTimestamp";

        /**
         * 不带时区偏移的 ISO 本地日期时间，转时间戳时必填。
         */
        private String dateTime;

        /**
         * UNIX 时间戳，转日期时间时必填，其单位由 unit 指定。
         */
        private Long timestamp;

        /**
         * 时间戳单位：seconds 或 milliseconds，默认 seconds。
         */
        @NotBlank
        private String unit = "seconds";

        /**
         * IANA 时区标识，例如 Asia/Hong_Kong，默认 UTC。
         */
        @NotBlank
        private String zoneId = "UTC";
    }

    /**
     * 文本工具通用请求。
     */
    @Data
    public static class Text {

        /**
         * 待处理文本，允许空字符串，最大长度为 65536 个 UTF-16 代码单元。
         * 排除于 toString，避免日志间接输出用户文本。
         */
        @NotNull
        @Size(max = 65536)
        @ToString.Exclude
        private String text;
    }

    /**
     * Base64 编解码请求，文本内容继承自通用文本请求。
     */
    @Data
    public static class Base64 extends Text {

        /**
         * 操作类型：encode 编码或 decode 解码，默认 encode。
         */
        @NotBlank
        private String operation = "encode";
    }

    /**
     * JSON 校验、格式化及压缩请求。
     */
    @Data
    public static class Json extends Text {

        /**
         * 处理模式：format 格式化或 compress 压缩，默认 format。
         */
        @NotBlank
        private String mode = "format";
    }

    /**
     * 文本摘要计算请求。
     */
    @Data
    public static class Digest extends Text {

        /**
         * 摘要算法：MD5、SHA-1、SHA-256 或 SHA-512，默认 SHA-256。
         */
        @NotBlank
        private String algorithm = "SHA-256";
    }

    /**
     * 日期间隔计算及日期加减请求。
     */
    @Data
    public static class DateCalculation {

        /**
         * 操作类型：difference 计算间隔或 add 加减天数，默认 difference。
         */
        @NotBlank
        private String operation = "difference";

        /**
         * 起始日期，格式为 yyyy-MM-dd，计算间隔时必填。
         */
        private String startDate;

        /**
         * 结束日期，格式为 yyyy-MM-dd，计算间隔时必填。
         */
        private String endDate;

        /**
         * 基准日期，格式为 yyyy-MM-dd，加减天数时必填。
         */
        private String date;

        /**
         * 增减天数，范围为 -365000～365000，负数表示向前计算，默认 0。
         */
        @Min(-365000)
        @Max(365000)
        private long days;
    }

    /**
     * 长度、重量和温度单位换算请求。
     */
    @Data
    public static class UnitConversion {

        /**
         * 单位类别：length、weight 或 temperature。
         */
        @NotBlank
        private String category;

        /**
         * 源单位，例如 m、kg 或 c，大小写不敏感。
         */
        @NotBlank
        private String from;

        /**
         * 目标单位，必须与源单位属于同一类别。
         */
        @NotBlank
        private String to;

        /**
         * 待换算数值，绝对值不得超过十的十二次方。
         * 服务层进一步限制精度和 scale，并校验非负重量、长度及绝对零度。
         */
        @NotNull
        @DecimalMin("-1000000000000")
        @DecimalMax("1000000000000")
        private BigDecimal value;
    }
}
