package com.ten.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ten.project.model.entity.UserInterfaceInfo;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

/**
* @author TEN
* @description 针对表【user_interface_info(用户调用接口关系)】的数据库操作Mapper
* @createDate 2025-08-04 15:26:39
* @Entity generator.domain.UserInterfaceInfo
*/
public interface UserInterfaceInfoMapper extends BaseMapper<UserInterfaceInfo> {

    /**
     * 原子初始化或扣减调用额度。
     *
     * @return 受影响行数；额度不足或记录被禁用时为 0
     */
    @Insert("INSERT INTO user_interface_info " +
            "(user_id, interface_id, total_num, left_num, status) " +
            "VALUES (#{userId}, #{interfaceId}, 1, #{initialLeftNum} - 1, 0) " +
            "ON DUPLICATE KEY UPDATE " +
            "total_num = IF(status = 0 AND is_delete = 0 AND left_num > 0, total_num + 1, total_num), " +
            "left_num = IF(status = 0 AND is_delete = 0 AND left_num > 0, left_num - 1, left_num)")
    int consumeInvokeCount(@Param("userId") long userId,
                           @Param("interfaceId") long interfaceId,
                           @Param("initialLeftNum") int initialLeftNum);
}




