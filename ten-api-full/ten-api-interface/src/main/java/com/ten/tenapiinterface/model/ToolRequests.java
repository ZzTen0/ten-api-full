package com.ten.tenapiinterface.model;

import lombok.Data;
import lombok.ToString;
import javax.validation.constraints.*;
import java.math.BigDecimal;

/** Request models and limits for the built-in tools. */
public final class ToolRequests {
    private ToolRequests() { }

    @Data public static class Password {
        @Min(4) @Max(128) private int length = 16;
        private boolean lowercase = true;
        private boolean uppercase = true;
        private boolean digits = true;
        private boolean symbols = false;
    }

    @Data public static class Timestamp {
        @NotBlank private String operation = "toTimestamp";
        private String dateTime;
        private Long timestamp;
        @NotBlank private String unit = "seconds";
        @NotBlank private String zoneId = "UTC";
    }

    @Data public static class Text {
        @NotNull @Size(max = 65536) @ToString.Exclude private String text;
    }

    @Data public static class Base64 extends Text {
        @NotBlank private String operation = "encode";
    }

    @Data public static class Json extends Text {
        @NotBlank private String mode = "format";
    }

    @Data public static class Digest extends Text {
        @NotBlank private String algorithm = "SHA-256";
    }

    @Data public static class DateCalculation {
        @NotBlank private String operation = "difference";
        private String startDate;
        private String endDate;
        private String date;
        @Min(-365000) @Max(365000) private long days;
    }

    @Data public static class UnitConversion {
        @NotBlank private String category;
        @NotBlank private String from;
        @NotBlank private String to;
        @NotNull @DecimalMin("-1000000000000") @DecimalMax("1000000000000")
        private BigDecimal value;
    }
}
