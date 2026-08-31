package com.ten.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.Gson;
import com.ten.apiclientsdk.client.ApiClient;
import com.ten.apiclientsdk.model.Username;
import com.ten.project.annotation.AuthCheck;
import com.ten.project.common.DeleteRequest;
import com.ten.project.common.ErrorCode;
import com.ten.project.common.IdRequest;
import com.ten.project.constant.CommonConstant;
import com.ten.project.exception.BusinessException;
import com.ten.project.mapper.InterfaceInfoMapper;
import com.ten.project.model.dto.interfaceinfo.InterfaceInfoAddRequest;
import com.ten.project.model.dto.interfaceinfo.InterfaceInfoInvokeRequest;
import com.ten.project.model.dto.interfaceinfo.InterfaceInfoQueryRequest;
import com.ten.project.model.dto.interfaceinfo.InterfaceInfoUpdateRequest;
import com.ten.project.model.entity.InterfaceInfo;
import com.ten.project.model.entity.User;
import com.ten.project.model.enums.InterfaceInfoStatusEnum;
import com.ten.project.service.InterfaceInfoService;
import com.ten.project.service.UserService;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author TEN
* @description 针对表【interface_info(接口信息)】的数据库操作Service实现
* @createDate 2025-07-14 15:45:41
*/
@Service
public class InterfaceInfoServiceImpl extends ServiceImpl<InterfaceInfoMapper, InterfaceInfo>
    implements InterfaceInfoService {

    @Resource
    private UserService userService;

    @Resource
    private ApiClient apiClient;

    @Resource
    private RedisTemplate<String,Object> redisTemplate;



    /**
     * 校验接口是否有效
     * @param interfaceInfo
     * @param add
     */
    public void validInterfaceInfo(InterfaceInfo interfaceInfo, boolean add) {

        if (interfaceInfo == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String name = interfaceInfo.getName();
        String description = interfaceInfo.getDescription();
        String url = interfaceInfo.getUrl();
        String requestHeader = interfaceInfo.getRequestHeader();
        String responseHeader = interfaceInfo.getResponseHeader();
        Integer status = interfaceInfo.getStatus();
        String method = interfaceInfo.getMethod();


        // 创建时，所有参数必须非空
        if (add) {
            if (StringUtils.isAnyBlank(name, description, url, requestHeader, responseHeader, method) || ObjectUtils.anyNull(status)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR);
            }
        }
        if (StringUtils.isNotBlank(name) && name.length() > 50) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "名称过长");
        }

    }

    /**
     * 新增接口
     *
     * @param interfaceInfoAddRequest
     * @param request
     * @return
     */
    public Long addInterfaceInfo(InterfaceInfoAddRequest interfaceInfoAddRequest, HttpServletRequest request) {
        if (interfaceInfoAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        InterfaceInfo interfaceinfo = new InterfaceInfo();
        BeanUtils.copyProperties(interfaceInfoAddRequest, interfaceinfo);
        // 校验
        validInterfaceInfo(interfaceinfo, true);
        User loginUser = userService.getLoginUser(request);
        interfaceinfo.setUserId(loginUser.getId());
        boolean result = save(interfaceinfo);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }
        return interfaceinfo.getId();
    }

    /**
     * 删除接口
     * @param deleteRequest
     * @param request
     * @return
     */
    @Override
    public Boolean deleteInterfaceInfo(DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        // 判断是否存在
        InterfaceInfo oldInterfaceInfo = getById(id);
        if (oldInterfaceInfo == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        // 仅本人或管理员可删除
        if (!oldInterfaceInfo.getUserId().equals(user.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        return removeById(id);
    }

    /**
     * 更新接口
     * @param interfaceinfoUpdateRequest
     * @param request
     * @return
     */
    @Override
    public Boolean updateInterfaceInfo(InterfaceInfoUpdateRequest interfaceinfoUpdateRequest, HttpServletRequest request) {

        if (interfaceinfoUpdateRequest == null || interfaceinfoUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        InterfaceInfo interfaceinfo = new InterfaceInfo();
        BeanUtils.copyProperties(interfaceinfoUpdateRequest, interfaceinfo);
        // 参数校验
        validInterfaceInfo(interfaceinfo, false);
        User user = userService.getLoginUser(request);
        long id = interfaceinfoUpdateRequest.getId();
        // 判断是否存在
        InterfaceInfo oldInterfaceInfo = getById(id);
        if (oldInterfaceInfo == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        // 仅本人或管理员可修改
        if (!oldInterfaceInfo.getUserId().equals(user.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        return updateById(interfaceinfo);
    }

    /**
     * 获取接口列表
     * @param interfaceinfoQueryRequest
     * @return
     */
    @Override
    @AuthCheck(mustRole = "admin")
    public List<InterfaceInfo> listInterfaceInfo(InterfaceInfoQueryRequest interfaceinfoQueryRequest) {
        InterfaceInfo interfaceinfoQuery = new InterfaceInfo();
        if (interfaceinfoQueryRequest != null) {
            BeanUtils.copyProperties(interfaceinfoQueryRequest, interfaceinfoQuery);
        }
        QueryWrapper<InterfaceInfo> queryWrapper = new QueryWrapper<>(interfaceinfoQuery);
        return list(queryWrapper);
    }

    @Override
    public List<InterfaceInfo> listOnlineInterfaceInfo() {
        return lambdaQuery()
                .eq(InterfaceInfo::getStatus, InterfaceInfoStatusEnum.ONLINE.getValue())
                .orderByAsc(InterfaceInfo::getId)
                .list();
    }

    @Override
    public InterfaceInfo getOnlineInterfaceInfoById(long id) {
        return lambdaQuery()
                .eq(InterfaceInfo::getId, id)
                .eq(InterfaceInfo::getStatus, InterfaceInfoStatusEnum.ONLINE.getValue())
                .one();
    }

    /**
     * 获取分页接口列表
     * @param interfaceinfoQueryRequest
     * @return
     */
    @Override
    public Page<InterfaceInfo> listInterfaceInfoByPage(InterfaceInfoQueryRequest interfaceinfoQueryRequest) {
        if (interfaceinfoQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        InterfaceInfo interfaceinfoQuery = new InterfaceInfo();
        BeanUtils.copyProperties(interfaceinfoQueryRequest, interfaceinfoQuery);
        long current = interfaceinfoQueryRequest.getCurrent();
        long size = interfaceinfoQueryRequest.getPageSize();
        String sortField = interfaceinfoQueryRequest.getSortField();
        String sortOrder = interfaceinfoQueryRequest.getSortOrder();
        String description = interfaceinfoQuery.getDescription();
        // description 需支持模糊搜索
        interfaceinfoQuery.setDescription(null);
        // 限制爬虫
        if (size > 50) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        QueryWrapper<InterfaceInfo> queryWrapper = new QueryWrapper<>(interfaceinfoQuery);
        queryWrapper.like(StringUtils.isNotBlank(description), "description", description);
        queryWrapper.orderBy(StringUtils.isNotBlank(sortField),
                sortOrder.equals(CommonConstant.SORT_ORDER_ASC), sortField);

        return page(new Page<>(current, size), queryWrapper);
    }

    /**
     * 上线接口
     * @param idRequest
     * @param request
     * @return
     */
    @Override
    public Boolean onlineInterfaceInfo(IdRequest idRequest, HttpServletRequest request) {
        return statusSwitch(idRequest, InterfaceInfoStatusEnum.ONLINE.getValue());
    }

    /**
     * 下线接口
     * @param idRequest
     * @param request
     * @return
     */
    @Override
    public Boolean offlineInterfaceInfo(IdRequest idRequest, HttpServletRequest request) {
        return statusSwitch(idRequest, InterfaceInfoStatusEnum.OFFLINE.getValue());
    }

    private Boolean statusSwitch(IdRequest idRequest, int targetStatus) {
        if (idRequest == null || idRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long id = idRequest.getId();
        // 判断是否存在
        InterfaceInfo oldInterfaceInfo = getById(id);
        if (oldInterfaceInfo == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        if (oldInterfaceInfo.getStatus() == targetStatus) {
            return true;
        }

        InterfaceInfo interfaceInfo = new InterfaceInfo();
        interfaceInfo.setId(id);
        interfaceInfo.setStatus(targetStatus);
        return updateById(interfaceInfo);
    }

    /**
     * 调用接口
     * @param interfaceInfoInvokeRequest
     * @param request
     * @return
     */
    @Override
    public Object invokeInterfaceInfo(InterfaceInfoInvokeRequest interfaceInfoInvokeRequest, HttpServletRequest request) {
        if (interfaceInfoInvokeRequest == null || interfaceInfoInvokeRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long id = interfaceInfoInvokeRequest.getId();
        String userRequestParams = interfaceInfoInvokeRequest.getUserRequestParams();
        // 判断是否存在
        InterfaceInfo oldInterfaceInfo = getById(id);
        if (oldInterfaceInfo == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        // 判断当前接口是否启用
        if (oldInterfaceInfo.getStatus() == InterfaceInfoStatusEnum.OFFLINE.getValue()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "接口已关闭");
        }
        User loginUser = userService.getLoginUser(request);
        String accessKey = loginUser.getAccessKey();
        String secretKey = loginUser.getSecretKey();
        ApiClient tempClient = new ApiClient(accessKey, secretKey,redisTemplate);
        Gson gson = new Gson();
        Username username = gson.fromJson(userRequestParams, Username.class);
        String nameByPOSTJson = tempClient.getNameByPOSTJson(username);
        return nameByPOSTJson;
    }
}





