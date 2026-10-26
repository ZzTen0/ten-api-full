package com.ten.project.dubbo;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ten.common.service.InnerUserInterfaceInfoService;
import com.ten.project.mapper.UserInterfaceInfoMapper;
import com.ten.project.model.entity.UserInterfaceInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;

import javax.annotation.Resource;

/**
 * 内部用户-接口调用关系服务 Dubbo 实现
 */
@DubboService
@Slf4j
public class InnerUserInterfaceInfoServiceImpl implements InnerUserInterfaceInfoService {

    @Resource
    private UserInterfaceInfoMapper userInterfaceInfoMapper;

    /**
     * 默认首次调用分配的调用次数
     */
    private static final int DEFAULT_INIT_LEFT_NUM = 100;

    @Override
    public boolean invokeCount(long userId, long interfaceId) {
        if (userId <= 0 || interfaceId <= 0) {
            return false;
        }
        int affectedRows = userInterfaceInfoMapper.consumeInvokeCount(
                userId, interfaceId, DEFAULT_INIT_LEFT_NUM);
        if (affectedRows <= 0) {
            log.debug("调用额度不足或记录不可用 userId={}, interfaceId={}", userId, interfaceId);
        }
        return affectedRows > 0;
    }

    @Override
    public boolean hasLeftNum(long userId, long interfaceId) {
        if (userId <= 0 || interfaceId <= 0) {
            return false;
        }
        QueryWrapper<UserInterfaceInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        wrapper.eq("interface_id", interfaceId);
        wrapper.eq("status", 0);
        UserInterfaceInfo one = userInterfaceInfoMapper.selectOne(wrapper);
        // 无记录时视为有（会在invokeCount中初始化）
        if (one == null) {
            return true;
        }
        return one.getLeftNum() != null && one.getLeftNum() > 0;
    }
}
