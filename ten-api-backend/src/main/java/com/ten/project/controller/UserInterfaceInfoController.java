package com.ten.project.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ten.project.annotation.AuthCheck;
import com.ten.project.common.*;
import com.ten.project.constant.UserConstant;
import com.ten.project.exception.BusinessException;
import com.ten.project.model.dto.userinterfaceinfo.UserInterfaceInfoAddRequest;
import com.ten.project.model.dto.userinterfaceinfo.UserInterfaceInfoQueryRequest;
import com.ten.project.model.dto.userinterfaceinfo.UserInterfaceInfoUpdateRequest;
import com.ten.project.model.entity.UserInterfaceInfo;
import com.ten.project.service.UserInterfaceInfoService;
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
@RequestMapping("/userInterfaceInfo")
@Slf4j
public class UserInterfaceInfoController {

    @Resource
    private UserInterfaceInfoService userInterfaceInfoService;

    // region 增删改查
    /**
     * 创建
     *
     * @param userInterfaceInfoAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addUserInterfaceInfo(@RequestBody UserInterfaceInfoAddRequest userInterfaceInfoAddRequest, HttpServletRequest request) {
        log.info("新增接口：{}", userInterfaceInfoAddRequest);
        Long addUserInterfaceInfo = userInterfaceInfoService.addUserInterfaceInfo(userInterfaceInfoAddRequest, request);
        return ResultUtils.success(addUserInterfaceInfo);
    }

    /**
     * 删除
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteUserInterfaceInfo(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        log.info("删除接口：{}", deleteRequest);
        Boolean b = userInterfaceInfoService.deleteUserInterfaceInfo(deleteRequest, request);
        return ResultUtils.success(b);
    }

    /**
     * 更新
     *
     * @param userInterfaceInfoUpdateRequest
     * @param request
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateUserInterfaceInfo(@RequestBody UserInterfaceInfoUpdateRequest userInterfaceInfoUpdateRequest,
                                            HttpServletRequest request) {
        log.info("更新接口：{}", userInterfaceInfoUpdateRequest);
        Boolean result = userInterfaceInfoService.updateUserInterfaceInfo(userInterfaceInfoUpdateRequest, request);
        return ResultUtils.success(result);
    }

    /**
     * 根据 id 获取
     *
     * @param id
     * @return
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<UserInterfaceInfo> getUserInterfaceInfoById(long id) {
        log.info("获取接口id：{}", id);
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        UserInterfaceInfo userInterfaceInfo = userInterfaceInfoService.getById(id);
        return ResultUtils.success(userInterfaceInfo);
    }

    /**
     * 获取列表（仅管理员可使用）
     *
     * @param userInterfaceInfoQueryRequest
     * @return
     */
    @GetMapping("/list")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<List<UserInterfaceInfo>> listUserInterfaceInfo(UserInterfaceInfoQueryRequest userInterfaceInfoQueryRequest) {
        log.info("查询接口列表：{}", userInterfaceInfoQueryRequest);
        List<UserInterfaceInfo> userInterfaceInfos = userInterfaceInfoService.listUserInterfaceInfo(userInterfaceInfoQueryRequest);
        return ResultUtils.success(userInterfaceInfos);
    }

    /**
     * 分页获取列表
     *
     * @param userInterfaceInfoQueryRequest
     * @param request
     * @return
     */
    @GetMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<UserInterfaceInfo>> listUserInterfaceInfoByPage(UserInterfaceInfoQueryRequest userInterfaceInfoQueryRequest,
                                                                             HttpServletRequest request) {
        log.info("查询分页接口：{}", userInterfaceInfoQueryRequest);
        Page<UserInterfaceInfo> page = userInterfaceInfoService.listUserInterfaceInfoByPage(userInterfaceInfoQueryRequest);
        return ResultUtils.success(page);
    }





    // endregion



}
