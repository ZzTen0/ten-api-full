package com.ten.tenapiinterface.controller;

import com.ten.tenapiinterface.model.ToolRequests;
import com.ten.tenapiinterface.model.ToolResponse;
import com.ten.tenapiinterface.service.ToolService;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

@RestController
@RequestMapping("/tools")
public class ToolController {
    private final ToolService tools;
    public ToolController(ToolService tools) { this.tools = tools; }

    @ModelAttribute
    public void noCache(HttpServletResponse response) { response.setHeader("Cache-Control", "no-store"); }

    @PostMapping("/password")
    public ToolResponse<Map<String, Object>> password(@Valid @RequestBody ToolRequests.Password request) {
        return ToolResponse.success(tools.password(request));
    }
    @GetMapping("/uuid")
    public ToolResponse<Map<String, Object>> uuid(@RequestParam(defaultValue = "1") int count,
                                                 @RequestParam(defaultValue = "true") boolean hyphens) {
        return ToolResponse.success(tools.uuids(count, hyphens));
    }
    @PostMapping("/timestamp")
    public ToolResponse<Map<String, Object>> timestamp(@Valid @RequestBody ToolRequests.Timestamp request) {
        return ToolResponse.success(tools.timestamp(request));
    }
    @PostMapping("/text/stats")
    public ToolResponse<Map<String, Object>> text(@Valid @RequestBody ToolRequests.Text request) {
        return ToolResponse.success(tools.text(request));
    }
    @PostMapping("/base64")
    public ToolResponse<Map<String, Object>> base64(@Valid @RequestBody ToolRequests.Base64 request) {
        return ToolResponse.success(tools.base64(request));
    }
    @PostMapping("/json")
    public ToolResponse<Map<String, Object>> json(@Valid @RequestBody ToolRequests.Json request) {
        return ToolResponse.success(tools.json(request));
    }
    @PostMapping("/digest")
    public ToolResponse<Map<String, Object>> digest(@Valid @RequestBody ToolRequests.Digest request) {
        return ToolResponse.success(tools.digest(request));
    }
    @GetMapping("/random")
    public ToolResponse<Map<String, Object>> random(@RequestParam(defaultValue = "0") int min,
            @RequestParam(defaultValue = "100") int max, @RequestParam(defaultValue = "1") int count,
            @RequestParam(defaultValue = "false") boolean unique) {
        return ToolResponse.success(tools.random(min, max, count, unique));
    }
    @PostMapping("/date")
    public ToolResponse<Map<String, Object>> date(@Valid @RequestBody ToolRequests.DateCalculation request) {
        return ToolResponse.success(tools.date(request));
    }
    @PostMapping("/units")
    public ToolResponse<Map<String, Object>> units(@Valid @RequestBody ToolRequests.UnitConversion request) {
        return ToolResponse.success(tools.units(request));
    }
    @GetMapping("/http-status")
    public ToolResponse<Map<String, Object>> httpStatus(@RequestParam int code) {
        return ToolResponse.success(tools.httpStatus(code));
    }
}
