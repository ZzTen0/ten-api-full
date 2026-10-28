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

/**
 * 内置工具业务服务。
 *
 * <p>集中实现工具计算、操作参数校验及资源上限控制，所有工具均在本地完成计算。</p>
 */
@Service
public class ToolService {
    /**
     * 密码、随机整数和随机打乱共用的安全随机数生成器。
     */
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * 单位换算的十进制运算精度。
     */
    private static final MathContext PRECISION = MathContext.DECIMAL128;

    /**
     * 英文单词识别规则，允许单词内部包含连字符或撇号。
     */
    private static final Pattern WORD = Pattern.compile("[A-Za-z]+(?:['’-][A-Za-z]+)*");

    /**
     * 长度单位相对于米的精确换算系数。
     */
    private static final Map<String, BigDecimal> LENGTH = factors(
            "mm", "0.001", "cm", "0.01", "m", "1", "km", "1000",
            "in", "0.0254", "ft", "0.3048", "yd", "0.9144", "mi", "1609.344");
    /**
     * 重量单位相对于千克的精确换算系数。
     */
    private static final Map<String, BigDecimal> WEIGHT = factors(
            "mg", "0.000001", "g", "0.001", "kg", "1", "lb", "0.45359237", "oz", "0.028349523125");
    /**
     * 工具专用 JSON 解析器，拒绝重复键和多根值，并保留数值精度。
     */
    private final ObjectMapper json = new ObjectMapper()
            .enable(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .enable(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS)
            .setNodeFactory(JsonNodeFactory.withExactBigDecimals(true));

    /**
     * 生成满足长度及字符类型要求的随机密码。
     *
     * @param request 密码长度与字符类型配置
     * @return 密码及长度
     * @throws IllegalArgumentException 长度超出范围或未选择字符类型
     */
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
        // 先为每种选定类型取一个字符，再补足长度并安全打乱，保证类型覆盖。
        for (String group : groups) chars.add(group.charAt(RANDOM.nextInt(group.length())));
        while (chars.size() < request.getLength()) chars.add(alphabet.charAt(RANDOM.nextInt(alphabet.length())));
        Collections.shuffle(chars, RANDOM);
        StringBuilder password = new StringBuilder();
        chars.forEach(password::append);
        return result("password", password.toString(), "length", request.getLength());
    }

    /**
     * 批量生成 UUID v4。
     *
     * @param count 生成数量，范围为 1～100
     * @param hyphens 是否保留 UUID 中的连字符
     * @return UUID 列表及数量
     * @throws IllegalArgumentException 数量超出范围
     */
    public Map<String, Object> uuids(int count, boolean hyphens) {
        range(count, 1, 100, "count");
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String id = UUID.randomUUID().toString();
            ids.add(hyphens ? id : id.replace("-", ""));
        }
        return result("uuids", ids, "count", count);
    }

    /**
     * 转换带指定时区的日期时间与 UNIX 时间戳。
     *
     * @param request 转换方向、时间单位、时区和待转换值
     * @return 时间戳、带偏移的日期时间、单位及时区
     * @throws IllegalArgumentException 操作无效、缺少参数或本地时间存在夏令时歧义
     * @throws DateTimeException 日期时间、时区无效或时间戳超出支持范围
     * @throws ArithmeticException 结果无法用对应精度的时间戳表示
     */
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

    /**
     * 统计文本的 Unicode 字符、非空白字符、汉字、英文单词及行数。
     *
     * @param request 待统计文本，字符上限按 UTF-16 代码单元校验
     * @return 各统计维度的计数；空文本为 0 行，末尾换行保留一个空行
     * @throws IllegalArgumentException 文本为 null 或长度超出范围
     */
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

    /**
     * 编码 UTF-8 文本或严格解码标准 Base64。
     *
     * @param request 操作类型和待转换文本
     * @return 操作类型及转换后的文本
     * @throws IllegalArgumentException 操作无效、Base64 无效或解码字节不是合法 UTF-8
     */
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

    /**
     * 校验 JSON，并按指定模式格式化或压缩。
     *
     * @param request JSON 文本及处理模式
     * @return 校验状态、处理模式及处理后的 JSON 文本
     * @throws IllegalArgumentException JSON 无效、重复键、多根值、空白输入或嵌套超过 128 层
     */
    public Map<String, Object> json(ToolRequests.Json request) {
        String text = checkedText(request);
        String mode = choice(request.getMode(), "mode", "format", "compress");
        // HTTP 字节上限不能限制递归深度，因此在 JSON 解析前单独检查嵌套层数。
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

    /**
     * 计算 UTF-8 文本的 MD5、SHA-1、SHA-256 或 SHA-512 摘要。
     *
     * @param request 待计算文本和摘要算法
     * @return 算法名称与小写十六进制摘要
     * @throws IllegalArgumentException 文本无效或算法不受支持
     * @throws IllegalStateException 运行环境缺少所选摘要算法
     */
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

    /**
     * 在闭区间内生成随机整数，支持结果不重复。
     *
     * @param min 最小值，范围为正负十亿
     * @param max 最大值，范围为正负十亿且不得小于 min
     * @param count 结果数量，范围为 1～1000
     * @param unique 是否要求结果不重复
     * @return 随机整数列表及实际生成参数
     * @throws IllegalArgumentException 取值范围无效或不重复数量超过可选整数个数
     */
    public Map<String, Object> random(int min, int max, int count, boolean unique) {
        range(min, -1000000000, 1000000000, "min");
        range(max, -1000000000, 1000000000, "max");
        range(count, 1, 1000, "count");
        if (min > max) throw invalid("min 不能大于 max");
        long size = (long) max - min + 1;
        if (unique && count > size) throw invalid("不重复随机数的数量不能超过范围内的整数个数");
        List<Integer> numbers = new ArrayList<>();
        if (unique) {
            // Floyd 抽样只执行 count 次，避免接近满区间时反复碰撞导致重试。
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

    /**
     * 计算日期的有符号间隔或加减指定天数。
     *
     * @param request 操作类型、日期和增减天数
     * @return 日期间隔或加减后的日期
     * @throws IllegalArgumentException 操作无效、缺少必要日期或天数超出范围
     * @throws DateTimeException 日期无效或运算结果超出支持范围
     */
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

    /**
     * 使用 DECIMAL128 精度换算长度、重量或温度。
     *
     * @param request 单位类别、源单位、目标单位及待换算数值
     * @return 换算结果及原始参数
     * @throws IllegalArgumentException 单位不匹配、数值超限或结果违反非负长度、重量及绝对零度约束
     */
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
            // 温度先统一转换为开尔文，以便集中校验绝对零度，再转换为目标单位。
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

    /**
     * 查询框架收录的标准 HTTP 状态码及使用说明。
     *
     * @param code HTTP 状态码
     * @return 状态码名称、原因短语、类别和中文说明
     * @throws IllegalArgumentException 状态码未被收录
     */
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

    /**
     * 校验通用文本输入。
     *
     * @param request 文本请求
     * @return 已通过非空及长度校验的文本
     * @throws IllegalArgumentException 文本为 null 或超过 65536 个 UTF-16 代码单元
     */
    private static String checkedText(ToolRequests.Text request) {
        String text = request.getText();
        if (text == null || text.length() > 65536) throw invalid("text 不能为空且不能超过 65536 个 UTF-16 代码单元");
        return text;
    }

    /**
     * 在递归解析前限制 JSON 容器嵌套，忽略字符串内部的括号。
     *
     * @param text 待检查 JSON 文本
     * @throws IllegalArgumentException 对象和数组的嵌套超过 128 层
     */
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

    /**
     * 校验整数是否落在闭区间内。
     *
     * @param value 待检查值
     * @param min 下界
     * @param max 上界
     * @param name 用于错误提示的参数名称
     * @throws IllegalArgumentException 参数超出范围
     */
    private static void range(int value, int min, int max, String name) {
        if (value < min || value > max) throw invalid(name + " 必须在 " + min + " 到 " + max + " 之间");
    }

    /**
     * 校验字符串参数是否必填。
     *
     * @param value 待检查值
     * @param name 参数名称
     * @return 非空白的原始值
     * @throws IllegalArgumentException 参数为 null 或空白
     */
    private static String required(String value, String name) {
        if (value == null || value.trim().isEmpty()) throw invalid(name + " 不能为空");
        return value;
    }

    /**
     * 按忽略大小写的方式匹配枚举值。
     *
     * @param value 待检查值
     * @param name 参数名称
     * @param choices 支持的规范取值
     * @return 命中的规范取值
     * @throws IllegalArgumentException 取值不在支持列表中
     */
    private static String choice(String value, String name, String... choices) {
        for (String choice : choices) if (choice.equalsIgnoreCase(value)) return choice;
        throw invalid(name + " 必须为 " + String.join("、", choices));
    }

    /**
     * 构造面向调用方的参数错误。
     *
     * @param message 不包含用户原始文本的错误说明
     * @return 参数异常
     */
    private static IllegalArgumentException invalid(String message) {
        return new IllegalArgumentException(message);
    }

    /**
     * 构建只读单位换算系数表。
     *
     * @param entries 按单位名称、十进制系数交替排列的键值
     * @return 单位到精确换算系数的只读映射
     */
    private static Map<String, BigDecimal> factors(String... entries) {
        Map<String, BigDecimal> map = new HashMap<>();
        for (int i = 0; i < entries.length; i += 2) map.put(entries[i], new BigDecimal(entries[i + 1]));
        return Collections.unmodifiableMap(map);
    }

    /**
     * 按传入顺序构建工具结果。
     *
     * @param values 按字符串字段名、字段值交替排列的键值
     * @return 保持插入顺序的结果映射
     */
    private static Map<String, Object> result(Object... values) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) map.put((String) values[i], values[i + 1]);
        return map;
    }
}
