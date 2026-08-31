package com.ten.project.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ten.project.common.DeleteRequest;
import com.ten.project.model.dto.userinterfaceinfo.UserInterfaceInfoAddRequest;
import com.ten.project.model.dto.userinterfaceinfo.UserInterfaceInfoQueryRequest;
import com.ten.project.model.dto.userinterfaceinfo.UserInterfaceInfoUpdateRequest;
import com.ten.project.model.entity.InterfaceInfo;
import com.ten.project.model.entity.UserInterfaceInfo;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author TEN
* @description 针对表【user_interface_info(用户调用接口关系)】的数据库操作Service
* @createDate 2025-08-04 15:33:18
*/
public interface UserInterfaceInfoService extends IService<UserInterfaceInfo> {
    /**
     * 新增接口
     * @param userInterfaceInfoAddRequest
     * @param request
     * @return
     */
    Long addUserInterfaceInfo(UserInterfaceInfoAddRequest userInterfaceInfoAddRequest, HttpServletRequest request);

    /**
     * 更新接口
     * @param userInterfaceInfoUpdateRequest
     * @param request
     * @return
     */
    Boolean updateUserInterfaceInfo(UserInterfaceInfoUpdateRequest userInterfaceInfoUpdateRequest, HttpServletRequest request);

    /**
     * 删除接口
     * @param deleteRequest
     * @param request
     * @return
     */
    Boolean deleteUserInterfaceInfo(DeleteRequest deleteRequest, HttpServletRequest request);

    /**
     * 获取接口调用信息
     * @param userInterfaceInfoQueryRequest
     * @return
     */
    List<UserInterfaceInfo> listUserInterfaceInfo(UserInterfaceInfoQueryRequest userInterfaceInfoQueryRequest);

    /**
     * 获取接口调用信息（分页）
     * @param userInterfaceInfoQueryRequest
     * @return
     */
    Page<UserInterfaceInfo> listUserInterfaceInfoByPage(UserInterfaceInfoQueryRequest userInterfaceInfoQueryRequest);

    /**
     * 校验
     * @param userInterfaceInfo
     * @param add
     */
    void validUserInterfaceInfo(UserInterfaceInfo userInterfaceInfo, boolean add);

    /**
     * 调用接口统计
     * @param interfaceInfoId
     * @param userId
     * @return
     */
    boolean invokeCount(long interfaceInfoId, long userId);

}
