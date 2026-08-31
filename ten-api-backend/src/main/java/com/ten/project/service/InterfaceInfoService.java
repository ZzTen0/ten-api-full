package com.ten.project.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ten.project.common.DeleteRequest;
import com.ten.project.common.IdRequest;
import com.ten.project.model.dto.interfaceinfo.InterfaceInfoAddRequest;
import com.ten.project.model.dto.interfaceinfo.InterfaceInfoInvokeRequest;
import com.ten.project.model.dto.interfaceinfo.InterfaceInfoQueryRequest;
import com.ten.project.model.dto.interfaceinfo.InterfaceInfoUpdateRequest;
import com.ten.project.model.entity.InterfaceInfo;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author TEN
* @description 针对表【interface_info(接口信息)】的数据库操作Service
* @createDate 2025-07-14 15:45:41
*/
public interface InterfaceInfoService extends IService<InterfaceInfo> {

    void validInterfaceInfo(InterfaceInfo interfaceinfo, boolean add);

    /**
     * 创建接口
     *
     * @param interfaceinfoAddRequest
     * @param request
     * @return
     */
    Long addInterfaceInfo(InterfaceInfoAddRequest interfaceinfoAddRequest, HttpServletRequest request);

    /**
     * 删除接口
     * @param deleteRequest
     * @param request
     */
    Boolean deleteInterfaceInfo(DeleteRequest deleteRequest, HttpServletRequest request);

    /**
     * 更新接口
     * @param interfaceinfoUpdateRequest
     * @param request
     * @return
     */
    Boolean updateInterfaceInfo(InterfaceInfoUpdateRequest interfaceinfoUpdateRequest, HttpServletRequest request);

    /**
     * 获取接口列表
     * @param interfaceinfoQueryRequest
     * @return
     */
    List<InterfaceInfo> listInterfaceInfo(InterfaceInfoQueryRequest interfaceinfoQueryRequest);

    /**
     * 获取所有已上线接口，供接口市场使用
     *
     * @return 已上线接口列表
     */
    List<InterfaceInfo> listOnlineInterfaceInfo();

    /**
     * 获取已上线接口详情，供接口市场使用
     *
     * @param id 接口 id
     * @return 已上线接口，不存在或未上线时返回 null
     */
    InterfaceInfo getOnlineInterfaceInfoById(long id);

    /**
     * 获取分页接口列表
     * @param interfaceinfoQueryRequest
     * @return
     */
    Page<InterfaceInfo> listInterfaceInfoByPage(InterfaceInfoQueryRequest interfaceinfoQueryRequest);

    /**
     * 上线接口
     * @param idRequest
     * @param request
     * @return
     */
    Boolean onlineInterfaceInfo(IdRequest idRequest, HttpServletRequest request);

    /**
     * 下线接口
     * @param idRequest
     * @param request
     * @return
     */
    Boolean offlineInterfaceInfo(IdRequest idRequest, HttpServletRequest request);

    /**
     * 接口调用
     * @param interfaceInfoInvokeRequest
     * @param request
     * @return
     */
    Object invokeInterfaceInfo(InterfaceInfoInvokeRequest interfaceInfoInvokeRequest, HttpServletRequest request);
}
