package com.ten.project.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ten.apiclientsdk.client.ApiClient;
import com.ten.project.common.*;
import com.ten.project.exception.BusinessException;
import com.ten.project.model.dto.interfaceinfo.InterfaceInfoAddRequest;
import com.ten.project.model.dto.interfaceinfo.InterfaceInfoInvokeRequest;
import com.ten.project.model.dto.interfaceinfo.InterfaceInfoQueryRequest;
import com.ten.project.model.dto.interfaceinfo.InterfaceInfoUpdateRequest;
import com.ten.project.model.entity.InterfaceInfo;
import com.ten.project.service.InterfaceInfoService;
import com.ten.project.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 帖子接口
 *
 * @author ten
 */
@RestController
@RequestMapping("/interfaceInfo")
@Slf4j
public class InterfaceInfoController {

    @Resource
    private InterfaceInfoService interfaceinfoService;

    // region 增删改查
    /**
     * 创建
     *
     * @param interfaceinfoAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Long> addInterfaceInfo(@RequestBody InterfaceInfoAddRequest interfaceinfoAddRequest, HttpServletRequest request) {
        log.info("新增接口：{}", interfaceinfoAddRequest);
        Long addInterfaceInfo = interfaceinfoService.addInterfaceInfo(interfaceinfoAddRequest, request);
        return ResultUtils.success(addInterfaceInfo);
    }

    /**
     * 删除
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteInterfaceInfo(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        log.info("删除接口：{}", deleteRequest);
        Boolean b = interfaceinfoService.deleteInterfaceInfo(deleteRequest, request);
        return ResultUtils.success(b);
    }

    /**
     * 更新
     *
     * @param interfaceinfoUpdateRequest
     * @param request
     * @return
     */
    @PostMapping("/update")
    public BaseResponse<Boolean> updateInterfaceInfo(@RequestBody InterfaceInfoUpdateRequest interfaceinfoUpdateRequest,
                                            HttpServletRequest request) {
        log.info("更新接口：{}", interfaceinfoUpdateRequest);
        Boolean result = interfaceinfoService.updateInterfaceInfo(interfaceinfoUpdateRequest, request);
        return ResultUtils.success(result);
    }

    /**
     * 根据 id 获取
     *
     * @param id
     * @return
     */
    @GetMapping("/get")
    public BaseResponse<InterfaceInfo> getInterfaceInfoById(long id) {
        log.info("获取接口id：{}", id);
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        InterfaceInfo interfaceinfo = interfaceinfoService.getById(id);
        return ResultUtils.success(interfaceinfo);
    }

    /**
     * 获取列表（仅管理员可使用）
     *
     * @param interfaceinfoQueryRequest
     * @return
     */
    @GetMapping("/list")
    public BaseResponse<List<InterfaceInfo>> listInterfaceInfo(InterfaceInfoQueryRequest interfaceinfoQueryRequest) {
        log.info("查询接口列表：{}", interfaceinfoQueryRequest);
        List<InterfaceInfo> interfaceInfos = interfaceinfoService.listInterfaceInfo(interfaceinfoQueryRequest);
        return ResultUtils.success(interfaceInfos);
    }

    /**
     * 分页获取列表
     *
     * @param interfaceinfoQueryRequest
     * @param request
     * @return
     */
    @GetMapping("/list/page")
    public BaseResponse<Page<InterfaceInfo>> listInterfaceInfoByPage(InterfaceInfoQueryRequest interfaceinfoQueryRequest,
                                                                      HttpServletRequest request) {
        log.info("查询分页接口：{}", interfaceinfoQueryRequest);
        Page<InterfaceInfo> page = interfaceinfoService.listInterfaceInfoByPage(interfaceinfoQueryRequest);
        return ResultUtils.success(page);
    }

    /**
     * 上线接口
     * @param idRequest
     * @param request
     * @return
     */
    @PostMapping("/online")
    public BaseResponse<Boolean> onlineInterfaceInfo(@RequestBody IdRequest idRequest,
                                                     HttpServletRequest request) {
        log.info("上线接口：{}", idRequest);
        Boolean result = interfaceinfoService.onlineInterfaceInfo(idRequest, request);

        return ResultUtils.success(result);
    }


    /**
     * 上线接口
     * @param idRequest
     * @param request
     * @return
     */
    @PostMapping("/offline")
    public BaseResponse<Boolean> offlineInterfaceInfo(@RequestBody IdRequest idRequest,
                                                     HttpServletRequest request) {
        log.info("下线接口：{}", idRequest);
        Boolean result = interfaceinfoService.offlineInterfaceInfo(idRequest, request);

        return ResultUtils.success(result);
    }

    /**
     * 调用接口
     * @param interfaceInfoInvokeRequest
     * @param request
     * @return
     */
    @PostMapping("/invoke")
    public BaseResponse<Object> invokeInterfaceInfo(@RequestBody InterfaceInfoInvokeRequest interfaceInfoInvokeRequest,
                                                     HttpServletRequest request) {
        log.info("调用接口：{}", interfaceInfoInvokeRequest);
        Object result = interfaceinfoService.invokeInterfaceInfo(interfaceInfoInvokeRequest, request);
        return ResultUtils.success(result);
    }


    // endregion



}
