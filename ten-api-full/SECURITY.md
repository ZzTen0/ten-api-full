# 签名协议 v2 与迁移

## 升级要求

SDK、ten-api-common、网关和接口服务必须一起升级。v1 的 body 请求头和
SHA256(body + "." + secretKey) 不再被接受；不提供旧协议回退。
先构建 common，再构建 SDK，最后构建各服务。

网关与接口服务必须配置相同的 GATEWAY_INTERNAL_SECRET，至少 32 个字符，
使用随机生成的独立值，不能使用 .env.example 中的占位值或用户 SK。
Docker Compose 从项目根目录 .env 读取并向这两个服务传入该值。
本地直接启动 Java 进程时也必须设置同名环境变量。
缺少有效配置时开放接口返回 503，健康检查仍可用。

客户端只访问网关；SDK 不再向服务端 Redis 预登记 nonce，不需要获得 Redis 凭据。
保留带 RedisTemplate 的旧构造函数以兼容源码，但该参数不再参与鉴权。

## 客户端请求

必须携带 accessKey、timestamp、nonce、signatureVersion: 2 和 sign 请求头。
不发送 secretKey，也不再发送 body 请求头。
nonce 为每次请求新生成的 16～64 位字母、数字、下划线或连字符；
timestamp 为 UNIX 秒级时间戳，允许与服务器相差 300 秒。
请求体最多 1 MiB，按实际传输字节进行 SHA-256 摘要。

规范字符串由以下 7 行构成，以 LF 分隔，最后一行后无换行：

```text
TEN-API-V2
大写 HTTP 方法
原始编码路径及查询串
accessKey
timestamp
nonce
实际请求体的 SHA-256 小写十六进制摘要
```

sign = HMAC-SHA256(secretKey 的 UTF-8 字节, 规范字符串的 UTF-8 字节)，
结果为 64 位小写十六进制。空请求体使用空字节串的摘要。
路径必须保留百分号编码、查询参数顺序及原始查询串，不进行解码重排。
SDK 与网关共用 common 中的 RequestSignature，避免协议实现漂移。

验签通过后，网关使用 Redis SETNX 原子登记 nonce，TTL 为 601 秒，
覆盖时间戳允许向未来偏移 300 秒后仍有的完整有效窗口。重复请求拒绝放行。
签名错误不登记 nonce，也不扣额度。

## 网关与接口服务

网关移除客户端传入的 X-User-Id、X-Gateway-Signature 和旧 body 头。
验证客户端签名后，使用独立内部密钥计算新的 X-Gateway-Signature，
规范字符串中的身份改为已验证的用户 ID，其余字段绑定同一实际请求。
接口服务对所有业务入口统一验签，并原子消费自己 Redis 中的内部 nonce。
单独伪造 X-User-Id 无法通过；仅 GET /api/actuator/health 免内部鉴权。
网关使用 Redis database 1，接口服务使用 database 0，两者 nonce 独立。
内部密钥必须由网关和接口服务共同保管，定期轮换。

## 日志

后端请求日志仅记录请求 ID、HTTP 方法、路径、客户端地址和耗时；
不再字符串化 Controller 参数，不记录请求体、Cookie、签名或密钥。
密码和 SK 在相关 DTO、用户模型、SDK 配置及客户端的 toString 中排除。

## 验证与剩余边界

见 [优化分类](OPTIMIZATION_PLAN.md)。新增工具接口后已按方法和路径解析真实接口 ID；仍采用调用额度预扣规则。
生产部署仍须配置 HTTPS 与服务间网络隔离。
