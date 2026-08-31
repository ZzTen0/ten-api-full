create table if not exists `interface_info`
(
    `id`               bigint not null auto_increment comment '主键' primary key,
    `name`             varchar(256) not null comment '名称',
    `description`      varchar(256) null comment '描述',
    `url`              varchar(512) not null comment '接口地址',
    `request_params`    text not null comment '请求参数',
    `request_header`    text null comment '请求头',
    `response_header`   text null comment '响应头',
    `status`           int default 0 not null comment '接口状态(0-关闭, 1-开启)',
    `method`           varchar(256) not null comment '请求类型',
    `user_id`           bigint not null comment '创建人',
    `create_time`       datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    `update_time`       datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    `is_delete`         tinyint default 0 not null comment '是否删除(0-未删, 1-已删)'
) comment '接口信息';

INSERT INTO interface_info
(name, description, url, request_params, request_header, response_header, status, method, user_id)
VALUES
    ('获取用户信息', '通过用户ID获取详细信息', 'https://api.example.com/users/{id}',
     '{"id": 123}',
     '{"Content-Type": "application/json", "Authorization": "Bearer token123"}',
     '{"code": 200, "message": "success", "data": {"id": 123, "name": "张三", "age": 30}}',
     1, 'GET', 1001),

    ('创建订单', '提交订单信息创建新订单', 'https://api.example.com/orders',
     '{"productId": 456, "quantity": 2, "amount": 99.99}',
     '{"Content-Type": "application/json", "Authorization": "Bearer token456"}',
     '{"code": 201, "message": "订单创建成功", "data": {"orderId": "ORD20231015001", "status": "pending"}}',
     1, 'POST', 1002),

    ('更新用户资料', '修改用户基本信息', 'https://api.example.com/users/{id}',
     '{"name": "李四", "age": 25, "email": "lisi@example.com"}',
     '{"Content-Type": "application/json", "Authorization": "Bearer token123"}',
     '{"code": 200, "message": "更新成功"}',
     0, 'PUT', 1001),

    ('获取商品列表', '分页获取商品信息', 'https://api.example.com/products',
     '{"page": 1, "size": 10, "category": "electronics"}',
     '{"Content-Type": "application/json"}',
     '{"code": 200, "message": "success", "data": [{"id": 101, "name": "手机", "price": 2999.00}, {"id": 102, "name": "电脑", "price": 5999.00}]}',
     1, 'GET', 1003),

    ('删除订单', '根据订单ID删除指定订单', 'https://api.example.com/orders/{orderId}',
     '{}',
     '{"Authorization": "Bearer token456"}',
     '{"code": 200, "message": "删除成功"}',
     1, 'DELETE', 1002);

-- 用户调用接口关系表
create table if not exists `user_interface_info`
(
    `id`           bigint                             not null auto_increment comment '主键' primary key,
    `user_id`      bigint                             not null comment '调用用户id',
    `interface_id` bigint                             not null comment '接口id',
    `total_num`    int      default 0                 not null comment '调用次数',
    `left_num`     int      default 0                 not null comment '剩余调用次数',
    `status`       int      default 0                 not null comment '状态 0-正常 1-禁用',
    `create_time`  datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    `update_time`  datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    `is_delete`    tinyint  default 0                 not null comment '是否删除(0-未删, 1-已删)',
    unique key `uk_user_interface` (`user_id`, `interface_id`)
) comment '用户调用接口关系';
