package com.ten.tenapiinterface.service;

import com.ten.tenapiinterface.model.ToolRequests;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 工具业务服务单元测试。
 *
 * <p>覆盖全部工具的正常结果、输入边界、编码精度、日期及随机数约束。</p>
 */
class ToolServiceTest {
    private final ToolService tools = new ToolService();

    @Test
    void passwordsHaveSelectedCharacterClassesAndLength() {
        ToolRequests.Password request = new ToolRequests.Password();
        request.setLength(128);
        request.setSymbols(true);
        String password = (String) tools.password(request).get("password");
        assertEquals(128, password.length());
        assertTrue(password.matches(".*[a-z].*"));
        assertTrue(password.matches(".*[A-Z].*"));
        assertTrue(password.matches(".*[0-9].*"));
        assertTrue(password.matches(".*[^A-Za-z0-9].*"));
    }

    @Test
    void passwordCanContainOnlyDigitsAndRejectsEmptyAlphabet() {
        ToolRequests.Password request = new ToolRequests.Password();
        request.setLowercase(false);
        request.setUppercase(false);
        assertTrue(((String) tools.password(request).get("password")).matches("[0-9]{16}"));
        request.setDigits(false);
        assertThrows(IllegalArgumentException.class, () -> tools.password(request));
        request.setLength(129);
        assertThrows(IllegalArgumentException.class, () -> tools.password(request));
    }

    @Test
    @SuppressWarnings("unchecked")
    void createsUuidV4WithOrWithoutHyphens() {
        List<String> ids = (List<String>) tools.uuids(100, true).get("uuids");
        assertEquals(100, new HashSet<>(ids).size());
        ids.forEach(id -> assertEquals(4, UUID.fromString(id).version()));
        List<String> compact = (List<String>) tools.uuids(2, false).get("uuids");
        assertTrue(compact.stream().allMatch(id -> id.matches("[0-9a-f]{32}")));
        assertThrows(IllegalArgumentException.class, () -> tools.uuids(101, false));
    }

    @Test
    void timestampsRoundTripWithTimezoneAndMilliseconds() {
        ToolRequests.Timestamp request = new ToolRequests.Timestamp();
        request.setDateTime("1970-01-01T08:00:00");
        request.setZoneId("Asia/Hong_Kong");
        assertEquals(0L, tools.timestamp(request).get("timestamp"));
        request.setOperation("toDateTime");
        request.setTimestamp(1234L);
        request.setUnit("milliseconds");
        assertEquals("1970-01-01T08:00:01.234+08:00", tools.timestamp(request).get("dateTime"));
        request.setTimestamp(-1000L);
        assertEquals(-1000L, tools.timestamp(request).get("timestamp"));
    }

    @Test
    void invalidDatesAndDstAmbiguitiesAreRejected() {
        ToolRequests.Timestamp request = new ToolRequests.Timestamp();
        request.setZoneId("America/New_York");
        request.setDateTime("2026-03-08T02:30:00");
        assertThrows(IllegalArgumentException.class, () -> tools.timestamp(request));
        request.setDateTime("2026-11-01T01:30:00");
        assertThrows(IllegalArgumentException.class, () -> tools.timestamp(request));
        request.setOperation("toDateTime");
        request.setTimestamp(Long.MAX_VALUE);
        assertThrows(java.time.DateTimeException.class, () -> tools.timestamp(request));
    }

    @Test
    void countsUnicodeCodePointsWordsAndLines() {
        ToolRequests.Text request = new ToolRequests.Text();
        request.setText("你好 World!\r\nHi 😀");
        Map<String, Object> stats = tools.text(request);
        assertEquals(15, stats.get("characters"));
        assertEquals(11L, stats.get("nonWhitespaceCharacters"));
        assertEquals(2L, stats.get("chineseCharacters"));
        assertEquals(2, stats.get("englishWords"));
        assertEquals(2, stats.get("lines"));
        request.setText("");
        assertEquals(0, tools.text(request).get("lines"));
    }

    @Test
    void base64Utf8RoundTripAndInvalidInput() {
        ToolRequests.Base64 request = new ToolRequests.Base64();
        request.setText("你好 API");
        String encoded = (String) tools.base64(request).get("result");
        request.setOperation("decode");
        request.setText(encoded);
        assertEquals("你好 API", tools.base64(request).get("result"));
        request.setText("/w==");
        assertThrows(IllegalArgumentException.class, () -> tools.base64(request));
        request.setText("%not-base64%");
        assertThrows(IllegalArgumentException.class, () -> tools.base64(request));
    }

    @Test
    void jsonPreservesNumbersAndSupportsFormatAndCompress() {
        ToolRequests.Json request = new ToolRequests.Json();
        request.setMode("compress");
        request.setText("{ \"large\": 123456789012345678901234567890, \"fraction\": 0.1234567890123456789 }");
        assertEquals("{\"large\":123456789012345678901234567890,\"fraction\":0.1234567890123456789}",
                tools.json(request).get("result"));
        request.setMode("format");
        assertTrue(((String) tools.json(request).get("result")).contains("\n"));
        request.setText("null");
        assertEquals("null", tools.json(request).get("result"));
    }

    @Test
    void jsonRejectsDuplicatesTrailingValuesAndExcessiveNesting() {
        ToolRequests.Json request = new ToolRequests.Json();
        for (String invalid : Arrays.asList("{\"x\":1,\"x\":2}", "{} {}", "{", " ")) {
            request.setText(invalid);
            assertThrows(IllegalArgumentException.class, () -> tools.json(request));
        }
        request.setText(String.join("", Collections.nCopies(129, "[")) + "0"
                + String.join("", Collections.nCopies(129, "]")));
        assertThrows(IllegalArgumentException.class, () -> tools.json(request));
    }

    @Test
    void digestMatchesKnownVectorsAndRejectsUnknownAlgorithms() {
        ToolRequests.Digest request = new ToolRequests.Digest();
        request.setText("abc");
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
                tools.digest(request).get("digest"));
        request.setAlgorithm("MD5");
        assertEquals("900150983cd24fb0d6963f7d28e17f72", tools.digest(request).get("digest"));
        request.setAlgorithm("SHA-512");
        assertEquals(128, ((String) tools.digest(request).get("digest")).length());
        request.setAlgorithm("unsupported");
        assertThrows(IllegalArgumentException.class, () -> tools.digest(request));
    }

    @Test
    @SuppressWarnings("unchecked")
    void randomUniqueFillsSmallInclusiveRange() {
        List<Integer> values = (List<Integer>) tools.random(-4, 4, 9, true).get("numbers");
        assertEquals(9, new HashSet<>(values).size());
        assertTrue(values.contains(-4));
        assertTrue(values.contains(4));
        assertThrows(IllegalArgumentException.class, () -> tools.random(1, 2, 3, true));
        assertThrows(IllegalArgumentException.class, () -> tools.random(2, 1, 1, false));
        assertEquals(Collections.singletonList(5), tools.random(5, 5, 1, false).get("numbers"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void randomSupportsFullAllowedIntegerRange() {
        List<Integer> values = (List<Integer>) tools.random(-1000000000, 1000000000, 1000, true).get("numbers");
        assertEquals(1000, new HashSet<>(values).size());
        assertTrue(values.stream().allMatch(value -> value >= -1000000000 && value <= 1000000000));
    }

    @Test
    void dateCalculationsHandleLeapYearsAndNegativeDays() {
        ToolRequests.DateCalculation request = new ToolRequests.DateCalculation();
        request.setStartDate("2024-02-28");
        request.setEndDate("2024-03-01");
        assertEquals(2L, tools.date(request).get("days"));
        request.setOperation("add");
        request.setDate("2024-03-01");
        request.setDays(-1);
        assertEquals("2024-02-29", tools.date(request).get("result"));
        request.setDate("2026-02-30");
        assertThrows(java.time.DateTimeException.class, () -> tools.date(request));
    }

    @Test
    void unitsConvertLengthWeightAndTemperaturePrecisely() {
        ToolRequests.UnitConversion request = new ToolRequests.UnitConversion();
        request.setCategory("length"); request.setFrom("m"); request.setTo("cm"); request.setValue(BigDecimal.ONE);
        assertEquals(0, new BigDecimal("100").compareTo((BigDecimal) tools.units(request).get("result")));
        request.setCategory("weight"); request.setFrom("lb"); request.setTo("kg");
        assertEquals(0, new BigDecimal("0.45359237").compareTo((BigDecimal) tools.units(request).get("result")));
        request.setCategory("temperature"); request.setFrom("f"); request.setTo("c"); request.setValue(new BigDecimal("32"));
        assertEquals(0, BigDecimal.ZERO.compareTo((BigDecimal) tools.units(request).get("result")));
        request.setValue(new BigDecimal("-459.67")); request.setTo("k");
        assertEquals(0, BigDecimal.ZERO.compareTo((BigDecimal) tools.units(request).get("result")));
    }

    @Test
    void unitsRejectCrossCategoryNegativePhysicalValuesAndExtremeScales() {
        ToolRequests.UnitConversion request = new ToolRequests.UnitConversion();
        request.setCategory("length"); request.setFrom("m"); request.setTo("kg"); request.setValue(BigDecimal.ONE);
        assertThrows(IllegalArgumentException.class, () -> tools.units(request));
        request.setTo("cm"); request.setValue(new BigDecimal("-1"));
        assertThrows(IllegalArgumentException.class, () -> tools.units(request));
        request.setValue(new BigDecimal("1e-1000000"));
        assertThrows(IllegalArgumentException.class, () -> tools.units(request));
        request.setCategory("temperature"); request.setFrom("k"); request.setTo("c"); request.setValue(new BigDecimal("-1"));
        assertThrows(IllegalArgumentException.class, () -> tools.units(request));
    }

    @Test
    void httpStatusProvidesMeaningAndRejectsUnknownCodes() {
        assertEquals("NOT_FOUND", tools.httpStatus(404).get("name"));
        assertEquals("请求的资源不存在", tools.httpStatus(404).get("description"));
        assertEquals("成功响应", tools.httpStatus(200).get("category"));
        assertThrows(IllegalArgumentException.class, () -> tools.httpStatus(599));
    }
}
