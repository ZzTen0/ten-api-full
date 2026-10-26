# 优化分类与实施顺序

| 优先级 | 分类 | 优化项 | 本轮状态 |
| --- | --- | --- | --- |
| P0 | 鉴权与敏感信息保护 | 请求签名绑定真实请求体、方法、原始路径和查询串、AK、时间戳及 nonce | 已实现 v2 协议 |
| P0 | 鉴权与敏感信息保护 | 网关内部身份校验；保护 GET、路径 POST、JSON POST 所有接口入口 | 已实现内部签名与单次 nonce |
| P0 | 鉴权与敏感信息保护 | 请求日志禁止输出参数；密码与 SK 排除于对象字符串表示 | 已实现 |
| P1 | 接口与计费正确性 | 方法与路径映射真实接口 ID、校验接口上下线状态 | 新增工具接口时已完成 |
| P1 | 接口与计费正确性 | 请求 ID 幂等、额度预占、成功确认、超时补偿与账单核对 | 待实施，需先明确成功计费或按请求计费规则 |
| P1 | 调用与部署正确性 | 在线试调用使用配置的网关地址和真实目标接口 | 新增工具接口时已完成 |
| P2 | 数据与开发体验 | 控制台真实统计、调用流水、趋势图和额度查询 | 待实施 |
| P2 | 数据与开发体验 | 可编辑试调用参数、动态文档、示例自动验证 | 已支持参数编辑、目录示例与回归测试；更多文档自动生成仍可完善 |

本轮只推进 P0 及其必要的 SDK、配置、文档和回归测试。详见 [签名协议与迁移](SECURITY.md)。
新增工具接口时已修复写死的接口 ID 和在线试调用目标；仍保留现有预先扣减额度的业务规则。

## 验证范围

新增 11 个工具接口后，验证扩大到 86 项 Java 测试、4 项前端示例测试，以及独立 MySQL 8 的目录初始化、幂等及下线状态保留检查。
工具接口参数与部署步骤见 [API_TOOLS.md](API_TOOLS.md)。

本轮验证结果：common、SDK、gateway、interface、backend 五个模块编译打包通过；
50 项针对性测试与既有回归测试通过（0 failures，0 errors）。使用本机 JDK 17 编译，目标字节码为 Java 8。

新增测试类：`RequestSignatureTest`、`ApiClientSigningTest`、`ApiAuthInvokeFilterTest`、
`GatewayAuthenticationFilterTest`、`LogInterceptorSecurityTest`。
现有回归覆盖 `GatewayRateLimitConfigTest`、`InnerUserInterfaceInfoServiceImplTest`、
`UserControllerTest`、`InterfaceInfoControllerTest`。

- SDK 真实本地 HTTP 请求：中文 JSON、编码路径、查询参数和 HTTP 错误处理。
- 签名字段篡改、过期/未来/溢出时间戳、旧协议拒绝。
- 网关真实请求体缓存、单次 nonce、伪造身份头清理、白名单不能跳过开放接口鉴权。
- 接口服务内部签名、身份/请求体篡改、重放拒绝、各入口统一保护、健康检查放行。
- 日志切面不读取请求参数，异常时仍记录耗时，DTO 字符串不含密码。
- 测试使用本地 HTTP 服务和 Redis/RPC mock；真实 MySQL、Redis、Dubbo 与容器联调需在部署环境继续验证。
