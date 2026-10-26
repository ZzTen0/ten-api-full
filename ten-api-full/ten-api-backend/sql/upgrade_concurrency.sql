-- 执行前应确保下方查询无结果；存在重复记录时先人工合并额度。
SELECT user_id, interface_id, COUNT(*) AS duplicate_count
FROM user_interface_info
GROUP BY user_id, interface_id
HAVING COUNT(*) > 1;

ALTER TABLE user_interface_info
    ADD UNIQUE KEY uk_user_interface (user_id, interface_id);
