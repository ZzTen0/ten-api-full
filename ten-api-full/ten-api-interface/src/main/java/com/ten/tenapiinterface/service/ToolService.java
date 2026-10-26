package com.ten.tenapiinterface.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.ten.tenapiinterface.model.ToolRequests;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ToolService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final MathContext PRECISION = MathContext.DECIMAL128;
    private static final Pattern WORD = Pattern.compile("[A-Za-z]+(?:['’-][A-Za-z]+)*");
    private static final Map<String, BigDecimal> LENGTH = factors(
            "mm", "0.001", "cm", "0.01", "m", "1", "km", "1000",
            "in", "0.0254", "ft", "0.3048", "yd", "0.9144", "mi", "1609.344");
    private static final Map<String, BigDecimal> WEIGHT = factors(
            "mg", "0.000001", "g", "0.001", "kg", "1", "lb", "0.45359237", "oz", "0.028349523125");
    private final ObjectMapper json = new ObjectMapper()
            .enable(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .enable(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS)
            .setNodeFactory(JsonNodeFactory.withExactBigDecimals(true));

    public Map<String, Object> password(ToolRequests.Password request) {
        range(request.getLength(), 4, 128, "length");
        List<String> groups = new ArrayList<>();
        if (request.isLowercase()) groups.add("abcdefghijklmnopqrstuvwxyz");
        if (request.isUppercase()) groups.add("ABCDEFGHIJKLMNOPQRSTUVWXYZ");
        if (request.isDigits()) groups.add("0123456789");
        if (request.isSymbols()) groups.add("!@#$%^&*()-_=+[]{}:,.?");
        if (groups.isEmpty()) throw invalid("至少选择一种字符类型");
        String alphabet = String.join("", groups);
        List<Character> chars = new ArrayList<>();
        for (String group : groups) chars.add(group.charAt(RANDOM.nextInt(group.length())));
        while (chars.size() < request.getLength()) chars.add(alphabet.charAt(RANDOM.nextInt(alphabet.length())));
        Collections.shuffle(chars, RANDOM);
        StringBuilder password = new StringBuilder();
        chars.forEach(password::append);
        return result("password", password.toString(), "length", request.getLength());
    }

    public Map<String, Object> uuids(int count, boolean hyphens) {
        range(count, 1, 100, "count");
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String id = UUID.randomUUID().toString();
            ids.add(hyphens ? id : id.replace("-", ""));
        }
        return result("uuids", ids, "count", count);
    }

    public Map<String, Object> timestamp(ToolRequests.Timestamp request) {
        String unit = choice(request.getUnit(), "unit", "seconds", "milliseconds");
        ZoneId zone = ZoneId.of(request.getZoneId());
        Instant instant;
        if ("toTimestamp".equalsIgnoreCase(request.getOperation())) {
            LocalDateTime date = LocalDateTime.parse(required(request.getDateTime(), "dateTime"));
            if (zone.getRules().getValidOffsets(date).size() != 1) {
                throw invalid("该本地时间在指定时区不存在或有夏令时歧义，请选择明确的时间");
            }
            instant = date.atZone(zone).toInstant();
        } else if ("toDateTime".equalsIgnoreCase(request.getOperation())) {
            if (request.getTimestamp() == null) throw invalid("timestamp 不能为空");
            instant = "seconds".equals(unit) ? Instant.ofEpochSecond(request.getTimestamp())
                    : Instant.ofEpochMilli(request.getTimestamp());
        } else {
            throw invalid("operation 必须为 toTimestamp 或 toDateTime");
        }
        return result("timestamp", "seconds".equals(unit) ? instant.getEpochSecond() : instant.toEpochMilli(),
                "unit", unit, "dateTime", DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(instant.atZone(zone)),
                "zoneId", zone.getId());
    }

    public Map<String, Object> text(ToolRequests.Text request) {
        String text = checkedText(request);
        long chinese = text.codePoints().filter(cp -> Character.UnicodeScript.of(cp) == Character.UnicodeScript.HAN).count();
        long nonWhitespace = text.codePoints().filter(cp -> !Character.isWhitespace(cp) && !Character.isSpaceChar(cp)).count();
        Matcher words = WORD.matcher(text);
        int englishWords = 0;
        while (words.find()) englishWords++;
        return result("characters", text.codePointCount(0, text.length()), "nonWhitespaceCharacters", nonWhitespace,
                "chineseCharacters", chinese, "englishWords", englishWords,
                "lines", text.isEmpty() ? 0 : text.split("\\r\\n|\\n|\\r", -1).length);
    }

    public Map<String, Object> base64(ToolRequests.Base64 request) {
        String text = checkedText(request);
        String operation = choice(request.getOperation(), "operation", "encode", "decode");
        String output;
        if ("encode".equals(operation)) {
            output = Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8));
        } else {
            try {
                byte[] bytes = Base64.getDecoder().decode(text);
                output = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                        .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
            } catch (IllegalArgumentException | CharacterCodingException e) {
                throw invalid("text 必须是合法 Base64，解码结果必须为 UTF-8 文本");
            }
        }
        return result("operation", operation, "result", output);
    }

    public Map<String, Object> json(ToolRequests.Json request) {
        String text = checkedText(request);
        String mode = choice(request.getMode(), "mode", "format", "compress");
        // Bound recursive parsing independently of the HTTP byte limit.
        checkJsonDepth(text);
        try {
            JsonNode node = json.readTree(text);
            if (node == null || node.isMissingNode()) throw invalid("JSON 内容不能为空");
            String output = "compress".equals(mode) ? json.writeValueAsString(node)
                    : json.writerWithDefaultPrettyPrinter().writeValueAsString(node);
            return result("valid", true, "mode", mode, "result", output);
        } catch (JsonProcessingException e) {
            String location = e.getLocation() == null ? "" :
                    "：第 " + e.getLocation().getLineNr() + " 行，第 " + e.getLocation().getColumnNr() + " 列";
            throw invalid("JSON 格式无效或存在重复键" + location);
        }
    }

    public Map<String, Object> digest(ToolRequests.Digest request) {
        String algorithm = choice(request.getAlgorithm().toUpperCase(Locale.ROOT),
                "algorithm", "MD5", "SHA-1", "SHA-256", "SHA-512");
        try {
            byte[] bytes = MessageDigest.getInstance(algorithm).digest(checkedText(request).getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte value : bytes) hex.append(String.format(Locale.ROOT, "%02x", value & 255));
            return result("algorithm", algorithm, "digest", hex.toString());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("摘要算法不可用", e);
        }
    }

    public Map<String, Object> random(int min, int max, int count, boolean unique) {
        range(min, -1000000000, 1000000000, "min");
        range(max, -1000000000, 1000000000, "max");
        range(count, 1, 1000, "count");
        if (min > max) throw invalid("min 不能大于 max");
        long size = (long) max - min + 1;
        if (unique && count > size) throw invalid("不重复随机数的数量不能超过范围内的整数个数");
        List<Integer> numbers = new ArrayList<>();
        if (unique) {
            Set<Integer> selected = new LinkedHashSet<>();
            for (long j = size - count; j < size; j++) {
                int value = RANDOM.nextInt((int) (j + 1));
                if (!selected.add(value)) selected.add((int) j);
            }
            selected.forEach(value -> numbers.add(value + min));
            Collections.shuffle(numbers, RANDOM);
        } else {
            for (int i = 0; i < count; i++) numbers.add(min + RANDOM.nextInt((int) size));
        }
        return result("numbers", numbers, "min", min, "max", max, "count", count, "unique", unique);
    }

    public Map<String, Object> date(ToolRequests.DateCalculation request) {
        if ("difference".equalsIgnoreCase(request.getOperation())) {
            LocalDate start = LocalDate.parse(required(request.getStartDate(), "startDate"));
            LocalDate end = LocalDate.parse(required(request.getEndDate(), "endDate"));
            return result("startDate", start.toString(), "endDate", end.toString(),
                    "days", ChronoUnit.DAYS.between(start, end));
        }
        if ("add".equalsIgnoreCase(request.getOperation())) {
            if (Math.abs(request.getDays()) > 365000 || request.getDays() == Long.MIN_VALUE) {
                throw invalid("days 必须在 -365000 到 365000 之间");
            }
            LocalDate date = LocalDate.parse(required(request.getDate(), "date"));
            return result("date", date.toString(), "days", request.getDays(),
                    "result", date.plusDays(request.getDays()).toString());
        }
        throw invalid("operation 必须为 difference 或 add");
    }

    public Map<String, Object> units(ToolRequests.UnitConversion request) {
        String category = choice(request.getCategory(), "category", "length", "weight", "temperature");
        String from = required(request.getFrom(), "from").toLowerCase(Locale.ROOT);
        String to = required(request.getTo(), "to").toLowerCase(Locale.ROOT);
        BigDecimal value = request.getValue();
        if (value == null || value.scale() < -100 || value.scale() > 100 || value.precision() > 100
                || value.abs().compareTo(new BigDecimal("1000000000000")) > 0) {
            throw invalid("value 不能为空且绝对值不能超过 1000000000000");
        }
        BigDecimal converted;
        if ("temperature".equals(category)) {
            choice(from, "from", "c", "f", "k");
            choice(to, "to", "c", "f", "k");
            BigDecimal kelvin = "c".equals(from) ? value.add(new BigDecimal("273.15"))
                    : "f".equals(from) ? value.add(new BigDecimal("459.67")).multiply(new BigDecimal("5"))
                    .divide(new BigDecimal("9"), PRECISION) : value;
            if (kelvin.signum() < 0) throw invalid("温度不能低于绝对零度");
            converted = "c".equals(to) ? kelvin.subtract(new BigDecimal("273.15"))
                    : "f".equals(to) ? kelvin.multiply(new BigDecimal("9")).divide(new BigDecimal("5"), PRECISION)
                    .subtract(new BigDecimal("459.67")) : kelvin;
        } else {
            Map<String, BigDecimal> factors = "length".equals(category) ? LENGTH : WEIGHT;
            if (!factors.containsKey(from) || !factors.containsKey(to)) throw invalid("单位不属于指定类别");
            if (value.signum() < 0) throw invalid("长度和重量不能为负数");
            converted = value.multiply(factors.get(from), PRECISION).divide(factors.get(to), PRECISION);
        }
        return result("category", category, "from", from, "to", to, "value", value,
                "result", converted.stripTrailingZeros());
    }

    public Map<String, Object> httpStatus(int code) {
        HttpStatus status = HttpStatus.resolve(code);
        if (status == null) throw invalid("未收录该 HTTP 状态码，请输入标准状态码，例如 200、404、500");
        String[] categories = {"", "信息响应", "成功响应", "重定向", "客户端错误", "服务端错误"};
        String description;
        switch (code) {
            case 200: description = "请求成功，常用于查询或处理成功"; break;
            case 201: description = "资源创建成功，常用于新增记录"; break;
            case 202: description = "请求已接受，常用于异步任务"; break;
            case 204: description = "处理成功且无响应体，常用于删除操作"; break;
            case 301: description = "资源永久迁移，常用于固定地址变更"; break;
            case 302: description = "临时重定向到其他地址"; break;
            case 304: description = "资源未修改，可使用客户端缓存"; break;
            case 400: description = "请求参数或格式无效，需要修正请求"; break;
            case 401: description = "缺少有效身份凭据，需要登录或重新认证"; break;
            case 403: description = "服务器拒绝访问，常用于权限不足"; break;
            case 404: description = "请求的资源不存在"; break;
            case 405: description = "该资源不支持当前 HTTP 方法"; break;
            case 409: description = "请求与资源当前状态冲突"; break;
            case 413: description = "请求体超过服务器允许的大小"; break;
            case 415: description = "请求的媒体类型不被支持"; break;
            case 422: description = "请求格式可识别，但业务语义无法处理"; break;
            case 429: description = "请求频率超过限制，应降低频率后再试"; break;
            case 500: description = "服务器内部发生异常"; break;
            case 502: description = "网关收到无效的上游响应"; break;
            case 503: description = "服务暂不可用，常用于维护或过载"; break;
            case 504: description = "网关等待上游响应超时"; break;
            default: description = categories[code / 100] + "：" + status.getReasonPhrase();
        }
        return result("code", code, "name", status.name(), "reason", status.getReasonPhrase(),
                "category", categories[code / 100], "description", description);
    }

    private static String checkedText(ToolRequests.Text request) {
        String text = request.getText();
        if (text == null || text.length() > 65536) throw invalid("text 不能为空且不能超过 65536 个 UTF-16 代码单元");
        return text;
    }

    private static void checkJsonDepth(String text) {
        int depth = 0;
        boolean quoted = false, escaped = false;
        for (char c : text.toCharArray()) {
            if (quoted) {
                if (escaped) escaped = false;
                else if (c == '\\') escaped = true;
                else if (c == '"') quoted = false;
            } else if (c == '"') quoted = true;
            else if (c == '{' || c == '[') {
                if (++depth > 128) throw invalid("JSON 嵌套层数不能超过 128");
            } else if (c == '}' || c == ']') depth--;
        }
    }

    private static void range(int value, int min, int max, String name) {
        if (value < min || value > max) throw invalid(name + " 必须在 " + min + " 到 " + max + " 之间");
    }

    private static String required(String value, String name) {
        if (value == null || value.trim().isEmpty()) throw invalid(name + " 不能为空");
        return value;
    }

    private static String choice(String value, String name, String... choices) {
        for (String choice : choices) if (choice.equalsIgnoreCase(value)) return choice;
        throw invalid(name + " 必须为 " + String.join("、", choices));
    }

    private static IllegalArgumentException invalid(String message) { return new IllegalArgumentException(message); }

    private static Map<String, BigDecimal> factors(String... entries) {
        Map<String, BigDecimal> map = new HashMap<>();
        for (int i = 0; i < entries.length; i += 2) map.put(entries[i], new BigDecimal(entries[i + 1]));
        return Collections.unmodifiableMap(map);
    }

    private static Map<String, Object> result(Object... values) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) map.put((String) values[i], values[i + 1]);
        return map;
    }
}
