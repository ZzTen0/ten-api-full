package com.ten.project.dubbo;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ten.common.model.vo.InvokeUserVO;
import com.ten.common.service.InnerUserService;
import com.ten.project.mapper.UserMapper;
import com.ten.project.model.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.BeanUtils;

import javax.annotation.Resource;

/**
 * 内部用户服务 Dubbo 实现
 */
@DubboService
@Slf4j
public class InnerUserServiceImpl implements InnerUserService {

    @Resource
    private UserMapper userMapper;

    @Override
    public InvokeUserVO getInvokeUser(String accessKey) {
        if (accessKey == null || accessKey.trim().isEmpty()) {
            return null;
        }
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("access_key", accessKey);
        User user = userMapper.selectOne(wrapper);
        if (user == null) {
            log.warn("getInvokeUser: accessKey={} 未找到用户", accessKey);
            return null;
        }
        InvokeUserVO vo = new InvokeUserVO();
        BeanUtils.copyProperties(user, vo);
        return vo;
    }
}
