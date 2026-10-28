package com.ten.tenapiinterface.controller;

import com.ten.tenapiinterface.model.ToolRequests;
import com.ten.tenapiinterface.model.ToolResponse;
import com.ten.tenapiinterface.service.ToolService;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * 内置工具接口。
 *
 * <p>提供密码、编码、日期及单位换算等工具能力。请求在进入控制器前由网关身份过滤器统一鉴权，
 * 控制器负责参数校验和响应封装，业务计算交由工具服务处理。</p>
 */
@RestController
@RequestMapping("/tools")
public class ToolController {

    /**
     * 工具业务服务。
     */
    private final ToolService tools;

    /**
     * 创建工具接口控制器。
     *
     * @param tools 工具业务服务
     */
    public ToolController(ToolService tools) {
        this.tools = tools;
    }

    /**
     * 禁止缓存工具响应，避免生成的密码或用户输入衍生结果被中间缓存保存。
     *
     * @param response 当前 HTTP 响应
     */
    @ModelAttribute
    public void noCache(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
    }

    /**
     * 生成随机密码，保证每种选定的字符类型至少出现一次。
     *
     * @param request 密码长度及字符类型配置
     * @return 生成的密码和密码长度
     */
    @PostMapping("/password")
    public ToolResponse<Map<String, Object>> password(@Valid @RequestBody ToolRequests.Password request) {
        return ToolResponse.success(tools.password(request));
    }

    /**
     * 批量生成 UUID v4。
     *
     * @param count 生成数量，范围为 1～100，默认为 1
     * @param hyphens 是否保留连字符，默认为 true
     * @return UUID 列表及数量
     */
    @GetMapping("/uuid")
    public ToolResponse<Map<String, Object>> uuid(@RequestParam(defaultValue = "1") int count,
                                                 @RequestParam(defaultValue = "true") boolean hyphens) {
        return ToolResponse.success(tools.uuids(count, hyphens));
    }

    /**
     * 在指定时区内转换日期时间与秒级或毫秒级时间戳。
     *
     * @param request 转换方向、日期时间或时间戳、时间单位及时区
     * @return 转换后的时间戳、日期时间、单位及时区
     */
    @PostMapping("/timestamp")
    public ToolResponse<Map<String, Object>> timestamp(@Valid @RequestBody ToolRequests.Timestamp request) {
        return ToolResponse.success(tools.timestamp(request));
    }

    /**
     * 统计文本中的 Unicode 字符、汉字、英文单词及行数。
     *
     * @param request 待统计文本
     * @return 字符数、非空白字符数、汉字数、英文单词数和行数
     */
    @PostMapping("/text/stats")
    public ToolResponse<Map<String, Object>> text(@Valid @RequestBody ToolRequests.Text request) {
        return ToolResponse.success(tools.text(request));
    }

    /**
     * 对 UTF-8 文本进行标准 Base64 编码或解码。
     *
     * @param request 操作类型及待处理文本
     * @return 操作类型和转换结果
     */
    @PostMapping("/base64")
    public ToolResponse<Map<String, Object>> base64(@Valid @RequestBody ToolRequests.Base64 request) {
        return ToolResponse.success(tools.base64(request));
    }

    /**
     * 校验并格式化或压缩 JSON，拒绝重复键、多个根值及过深嵌套。
     *
     * @param request JSON 文本及格式化模式
     * @return JSON 校验结果、处理模式和处理后的文本
     */
    @PostMapping("/json")
    public ToolResponse<Map<String, Object>> json(@Valid @RequestBody ToolRequests.Json request) {
        return ToolResponse.success(tools.json(request));
    }

    /**
     * 计算 UTF-8 文本的摘要。
     *
     * @param request 待计算文本及摘要算法
     * @return 算法名称和小写十六进制摘要
     */
    @PostMapping("/digest")
    public ToolResponse<Map<String, Object>> digest(@Valid @RequestBody ToolRequests.Digest request) {
        return ToolResponse.success(tools.digest(request));
    }

    /**
     * 在包含上下界的整数范围内生成随机数。
     *
     * @param min 下界，取值范围为正负十亿，默认为 0
     * @param max 上界，取值范围为正负十亿，默认为 100
     * @param count 生成数量，范围为 1～1000，默认为 1
     * @param unique 是否要求结果互不重复，默认为 false
     * @return 随机整数列表及实际使用的生成参数
     */
    @GetMapping("/random")
    public ToolResponse<Map<String, Object>> random(@RequestParam(defaultValue = "0") int min,
            @RequestParam(defaultValue = "100") int max, @RequestParam(defaultValue = "1") int count,
            @RequestParam(defaultValue = "false") boolean unique) {
        return ToolResponse.success(tools.random(min, max, count, unique));
    }

    /**
     * 计算两个日期的有符号间隔，或对日期加减指定天数。
     *
     * @param request 日期计算方向、日期及增减天数
     * @return 日期间隔或加减天数后的日期
     */
    @PostMapping("/date")
    public ToolResponse<Map<String, Object>> date(@Valid @RequestBody ToolRequests.DateCalculation request) {
        return ToolResponse.success(tools.date(request));
    }

    /**
     * 换算长度、重量和温度单位。
     *
     * @param request 单位类别、源单位、目标单位和待换算数值
     * @return 换算结果及原始换算参数
     */
    @PostMapping("/units")
    public ToolResponse<Map<String, Object>> units(@Valid @RequestBody ToolRequests.UnitConversion request) {
        return ToolResponse.success(tools.units(request));
    }

    /**
     * 查询已收录的标准 HTTP 状态码。
     *
     * @param code 状态码，例如 200、404 或 500
     * @return 状态码名称、原因短语、类别及中文使用说明
     */
    @GetMapping("/http-status")
    public ToolResponse<Map<String, Object>> httpStatus(@RequestParam int code) {
        return ToolResponse.success(tools.httpStatus(code));
    }
}
