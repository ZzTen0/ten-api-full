# TEN API

TEN API 是一个基于 **Spring Boot + Spring Cloud Gateway + Dubbo** 的 API 开放平台，
提供接口发布与管理、用户登录鉴权、AK/SK 签名校验、防重放、限流熔断、调用次数统计与
配额扣减等全套能力，并配套 Web 管理端（接口市场、控制台、管理后台）和 Java 调用 SDK。

## 功能特性

- **API 市场**：浏览在线接口、搜索与分类筛选、查看接口详情与调试示例
- **接口管理**：管理员新增/修改/删除接口，控制接口上线、下线状态
- **用户体系**：注册、登录、注销；基于 Session 的登录态（Redis 存储，支持多实例共享）
- **开放接口鉴权**：AK/SK 签名（SHA-256）+ 时间戳有效期 + nonce 防重放
- **流量治理**：网关令牌桶限流（按 accessKey / IP）、Resilience4j 熔断降级、超时控制
- **配额扣减**：网关通过 Dubbo RPC 调用后端，以原子 SQL 完成调用次数判断与扣减
- **调用 SDK**：`api-client-sdk` 支持 Spring Boot 自动装配与纯 Java 手动调用

## 系统架构

```
                          ┌──────────────────────────────┐
   浏览器 / 管理端  ──────▶│  ten-api-frontend (Nginx 80) │
                          └───────────────┬──────────────┘
                                          │ /api/** 反向代理
                                          ▼
                          ┌──────────────────────────────┐
   SDK / 第三方客户端 ────▶│  ten-api-gateway (8090)      │
                          │  签名鉴权 / 防重放 / 限流 / 熔断 │
                          └──────┬────────────────┬──────┘
                                 │ /api/name/**   │ /api/user|interfaceInfo|
                                 │ (AK/SK 鉴权)    │ userInterfaceInfo|post
                                 ▼                ▼   (Session 鉴权, 直接放行)
                  ┌────────────────────┐  ┌───────────────────────────┐
                  │ ten-api-interface  │  │ ten-api-backend (7529)    │
                  │ (8123, 示例接口)    │  │ 用户/接口/配额管理 + Dubbo │
                  └─────────┬──────────┘  └──────────┬────────────────┘
                            │                        │ Dubbo RPC (20880)
                            │                        ▼
                            │              ┌────────────────────┐
                            │              │ ten-api-common     │
                            │              │ Dubbo 接口与模型    │
                            │              └────────────────────┘
                            ▼
              ┌───────────────────────────────────────────┐
              │ MySQL(3306) │ Redis(6379) │ Zookeeper(2181) │
              └───────────────────────────────────────────┘
```

### 两条鉴权链路

| 链路 | 适用请求 | 鉴权方式 | 关键实现 |
| --- | --- | --- | --- |
| 管理端 / Web | `/api/user/**`、`/api/interfaceInfo/**`、`/api/userInterfaceInfo/**`、`/api/post/**` | **Session**（Spring Session + Redis，Cookie 携带 JSESSIONID） | `UserServiceImpl` 读写 `userLoginState`，网关直接放行由后端校验 |
| 开放接口 | 其余 `/api/**`（如 `/api/name/**`） | **AK/SK 签名**（无状态） | `ApiAuthInvokeFilter` 校验签名 → nonce 防重放 → 扣减调用次数 |

### 开放接口调用时序

1. 客户端用 `accessKey` / `secretKey` 生成签名：`sign = SHA256(body + "." + secretKey)`
2. 请求头携带 `accessKey`、`nonce`、`timestamp`、`sign`、`body` 访问网关
3. 网关校验参数完整性、nonce 长度（16~64）、时间戳偏移（±300s）
4. 网关 `SETNX gateway:nonce:{accessKey}:{nonce}`（5 分钟 TTL）实现防重放
5. 网关经 Dubbo 查询用户（Caffeine 本地缓存 2 分钟），比对签名
6. 网关经 Dubbo 原子扣减调用次数，成功后透传 `X-User-Id` 到下游接口服务
7. 接口服务（如 `POST /api/name/object`）校验并消费 Redis 中预登记的 nonce 后返回结果

> 鉴权失败时网关返回 HTTP 403 + `{"code":40300,"message":"...","data":null}`。

## 项目结构

```
Backstage-end/
├── ten-api-common/          # Dubbo 公共接口与模型（被 backend 实现、gateway 消费）
│   └── com.ten.common
│       ├── service/         # InnerUserService、InnerUserInterfaceInfoService
│       └── model/vo/        # InvokeUserVO
├── api-client-sdk/          # 开放接口调用 SDK（0.0.2）
│   └── com.ten.apiclientsdk
│       ├── ApiClientConfig  # 自动装配入口（spring.factories）
│       ├── client/ApiClient # 调用客户端
│       ├── utils/SignUtils  # SHA-256 签名 / AK、SK 生成
│       └── config/RedisConfig
├── ten-api-backend/         # 业务后端：用户、接口、配额管理 + Dubbo 提供者（7529 / 20880）
│   └── com.ten.project
│       ├── controller/      # UserController、InterfaceInfoController 等
│       ├── service/impl/    # UserServiceImpl（Session 登录）、InterfaceInfoServiceImpl
│       ├── dubbo/           # InnerUserServiceImpl、InnerUserInterfaceInfoServiceImpl
│       ├── aop/             # AuthInterceptor（@AuthCheck）、LogInterceptor
│       └── config/          # CorsConfig、Knife4jConfig、MyBatisPlusConfig
├── ten-api-gateway/         # API 网关：鉴权、防重放、限流、熔断（8090）
│   └── com.ten.tenapigateway
│       ├── filter/          # ApiAuthInvokeFilter、RequestLogFilter、GatewaySignUtils
│       └── config/          # GatewayRateLimitConfig（apiKeyResolver）
├── ten-api-interface/       # 示例开放接口服务（8123）
│   └── com.ten.tenapiinterface
│       ├── controller/      # NameController
│       ├── service/impl/    # NameServiceImpl（nonce 校验）
│       └── aop/             # InvokeCountAOP
├── ten-api-frontend/        # Vue 3 前端：接口市场 / 用户控制台 / 管理后台
│   └── src
│       ├── views/           # Home、ApiMarket、ApiDetail、Docs、Dashboard、InterfaceManage、UserManage
│       ├── layouts/         # DefaultLayout、DashboardLayout、AdminLayout
│       ├── api/             # user.js、interface.js、admin.js
│       ├── stores/user.js   # Pinia 用户状态
│       ├── utils/request.js # Axios 封装（baseURL=/api，withCredentials）
│       └── router/index.js  # 路由与登录/管理员守卫
├── docker-compose.yml       # 单机一键部署编排（7 个服务）
├── .env.example             # 环境变量模板
├── DEPLOYMENT.md            # 生产部署详细步骤
└── README.md
```

## 技术栈

| 分类 | 技术 |
| --- | --- |
| 语言 / 运行时 | Java 8、Node.js 22（仅构建前端镜像时使用） |
| 后端框架 | Spring Boot 2.7.x（interface 模块 2.6.13）、MyBatis-Plus |
| 微服务 | Dubbo（Zookeeper 注册中心）、Spring Cloud Gateway 2021.0.8 |
| 流量治理 | Spring Cloud Gateway RequestRateLimiter、Resilience4j |
| 存储 | MySQL 8.0、Redis 6.2（含 Spring Session） |
| 前端 | Vue 3、Vite、Element Plus、Pinia、Vue Router、Axios、ECharts |
| 部署 | Docker / Docker Compose、Nginx |

## 端口与中间件

| 服务 | 端口 | 说明 |
| --- | --- | --- |
| ten-api-frontend | 80 | Nginx 托管静态资源，`/api/` 反代至网关 |
| ten-api-gateway | 8090 | 统一入口，对外只暴露前端端口 |
| ten-api-backend | 7529（HTTP，context-path `/api`）、20880（Dubbo） | 管理端 API 与 Dubbo 提供者 |
| ten-api-interface | 8123（HTTP，context-path `/api`） | 示例开放接口 |
| MySQL | 3306 | 业务库 `ten_api` |
| Redis | 6379 | 网关用 `database 1`，后端与接口服务用 `database 0` |
| Zookeeper | 2181 | Dubbo 注册中心 |

## 环境要求

- JDK 8
- Maven 3.6+
- Node.js 18+（仅本地开发前端时需要）
- MySQL 8 / Redis 6 / Zookeeper 3.7（本地直接启动服务时需要，或用 Docker 只启动中间件）

## 部署方法

### 方式一：Docker Compose 单机部署（推荐）

面向 4 核 / 8 GB 服务器，一条命令拉起全部 7 个容器。完整说明与升级步骤见
[DEPLOYMENT.md](DEPLOYMENT.md)，核心步骤如下。

1. 准备环境变量（务必修改所有 `replace_with_...` 值）：

```bash
cp .env.example .env
chmod 600 .env
```

2. 按依赖顺序构建 Java 产物（前端由镜像内多阶段构建，宿主机无需 Node.js）：

```bash
cd ten-api-common && mvn clean install -DskipTests
cd ../api-client-sdk && mvn clean install -DskipTests
cd ../ten-api-backend && mvn clean package -DskipTests
cd ../ten-api-gateway && mvn clean package -DskipTests
cd ../ten-api-interface && mvn clean package -DskipTests
cd ..
```

3. 校验并启动：

```bash
docker compose --env-file .env config
docker compose --env-file .env up -d --build
docker compose ps
```

4. 访问 `http://服务器地址/`，应用 API 走同一域名下的 `/api/**`。

#### 环境变量说明（`.env`）

| 变量 | 必填 | 说明 |
| --- | --- | --- |
| `MYSQL_ROOT_PASSWORD` | 是 | MySQL root 密码 |
| `MYSQL_DATABASE` | 否 | 业务库名，默认 `ten_api` |
| `MYSQL_USER` / `MYSQL_PASSWORD` | 是 | 应用数据库账号与密码 |
| `REDIS_PASSWORD` | 是 | Redis 密码 |
| `HTTP_PORT` | 否 | 前端发布端口，默认 `80` |
| `BACKEND_JAVA_OPTS` / `GATEWAY_JAVA_OPTS` / `INTERFACE_JAVA_OPTS` | 否 | 各服务 JVM 参数覆盖（已按小内存机器预设） |

> `.env` 中的值必须是纯十六进制等无特殊字符的字符串，避免 Compose 插值问题。

#### 数据库初始化

- 全新数据卷首次启动会自动执行 [ddl.sql](ten-api-backend/sql/ddl.sql) 与 [db.sql](ten-api-backend/sql/db.sql)（仅数据目录为空时执行一次）。
- 已有库升级前请先备份，再执行并发额度升级脚本 [upgrade_concurrency.sql](ten-api-backend/sql/upgrade_concurrency.sql)，命令见 [DEPLOYMENT.md](DEPLOYMENT.md)。

主要数据表：

| 表 | 说明 |
| --- | --- |
| `user` | 用户及其 AK/SK、角色 |
| `interface_info` | 接口元数据、路径、方法、状态 |
| `user_interface_info` | 用户与接口的调用额度关系（`user_id + interface_id` 唯一索引） |
| `post` | 帖子示例业务表 |

#### 运维命令

```bash
docker compose ps                              # 查看服务状态
docker compose logs -f --tail=200 ten-api-gateway
docker compose restart ten-api-backend
docker compose down                            # 停止并移除容器（保留数据卷）
```

> 切勿对已有数据执行 `docker compose down -v`，会清空数据卷。

### 方式二：本地开发

1. 启动中间件（可只用 Docker 起依赖）：

```bash
docker run -d --name ten-mysql -p 3306:3306 -e MYSQL_ROOT_PASSWORD=root -e MYSQL_DATABASE=ten_api mysql:8.0.27
docker run -d --name ten-redis -p 6379:6379 redis:6.2-alpine
docker run -d --name ten-zk -p 2181:2181 -e ALLOW_ANONYMOUS_LOGIN=yes bitnami/zookeeper:3.7
```

2. 依次启动后端服务（建议按 `backend → interface → gateway` 顺序，Dubbo 已配置 `check=false` 可独立启动）：

| 模块 | 启动类 | 地址 |
| --- | --- | --- |
| ten-api-backend | `com.ten.project.MyApplication` | http://localhost:7529/api |
| ten-api-interface | `com.ten.tenapiinterface.TenApiInterfaceApplication` | http://localhost:8123/api |
| ten-api-gateway | `com.ten.tenapigateway.TenApiGatewayApplication` | http://localhost:8090 |

3. 启动前端开发服务器（`/api` 已代理到网关 8090）：

```bash
cd ten-api-frontend
npm install
npm run dev        # http://localhost:5173
```

## api-client-sdk 使用方法

### 1. 引入依赖

SDK 为独立模块，需先安装到本地仓库：

```bash
cd api-client-sdk && mvn clean install -DskipTests
```

再在业务工程中引入：

```xml
<dependency>
    <groupId>com.ten</groupId>
    <artifactId>api-client-sdk</artifactId>
    <version>0.0.2</version>
</dependency>
```

### 2. 方式 A：Spring Boot 自动装配（推荐）

SDK 通过 `META-INF/spring.factories` 注册了 `ApiClientConfig`，引入依赖后自动生效。
只需在配置文件中声明 AK/SK 与网关地址：

```yaml
api:
  client:
    access-key: your-access-key
    secret-key: your-secret-key
    gateway-host: http://localhost:8090   # 生产环境填网关对外地址
```

在任意 Bean 中直接注入使用：

```java
@Service
public class DemoService {

    @Resource
    private ApiClient apiClient;

    public String call() {
        Username username = new Username();
        username.setUsername("ten");
        // 携带签名头调用（POST JSON 接口）
        return apiClient.getNameByPOSTJson(username);
    }
}
```

### 3. 方式 B：纯 Java 手动调用

```java
ApiClient apiClient = new ApiClient("your-access-key", "your-secret-key");
apiClient.setGatewayHost("http://localhost:8090");

Username username = new Username();
username.setUsername("ten");
String result = apiClient.getNameByPOSTJson(username);
```

如需调用会在服务端校验 nonce 的接口（如 `POST /api/name/object`），需额外传入
`RedisTemplate`（指向与接口服务相同的 Redis 库，默认 `database 0`），SDK 会把
nonce 预登记到 `nonce:{accessKey}:{nonce}`：

```java
@Resource
private RedisTemplate<String, Object> redisTemplate;

ApiClient apiClient = new ApiClient(accessKey, secretKey, redisTemplate);
apiClient.setGatewayHost("http://localhost:8090");
```

### 4. ApiClient 方法一览

| 方法 | 请求 | 携带签名头 | 说明 |
| --- | --- | --- | --- |
| `getNameByGET(String name)` | `GET /api/name/ga?name=xxx` | 否 | 直连演示用法 |
| `getNameByPOSTPath(String name)` | `POST /api/name/path/{name}` | 否 | 路径参数演示用法 |
| `getNameByPOSTJson(Username username)` | `POST /api/name/object` | 是 | 标准签名调用，可按此方法扩展业务接口 |

> 注意：`getNameByGET` 与 `getNameByPOSTPath` 未附带鉴权头，仅作演示；走网关的开放接口
> 必须携带 `accessKey` / `nonce` / `timestamp` / `sign` / `body` 请求头，请参考
> `getHeaderMap` 的实现自行扩展。

### 5. 签名与请求头规则

| 请求头 | 说明 |
| --- | --- |
| `accessKey` | 用户访问密钥，登录控制台查看 |
| `secretKey` | 用户私钥，**仅用于本地计算签名，严禁随请求发送** |
| `nonce` | 随机字符串，长度需为 16~64，且仅由字母、数字、`_`、`-` 组成 |
| `timestamp` | UNIX 秒级时间戳，与服务器偏差不得超过 300 秒 |
| `body` | 请求体内容（GET 场景可为空字符串） |
| `sign` | `SHA256(body + "." + secretKey)` 的十六进制小写串 |

### 6. 生成 AK / SK

`SignUtils` 已提供生成能力（基于 UUID）：

```java
SignUtils signUtils = new SignUtils();
String accessKey = signUtils.generateAccessKey();
String secretKey = signUtils.generateSecretKey();
```

## 接口调用示例（curl）

```bash
# 以 POST /api/name/object 为例
BODY='{"username":"ten"}'
SECRET='your-secret-key'
NONCE='abcdefghij123456'
TS=$(date +%s)
SIGN=$(printf '%s.%s' "$BODY" "$SECRET" | sha256sum | awk '{print $1}')

curl -X POST http://localhost:8090/api/name/object \
  -H "Content-Type: application/json" \
  -H "accessKey: your-access-key" \
  -H "nonce: $NONCE" \
  -H "timestamp: $TS" \
  -H "sign: $SIGN" \
  -H "body: $BODY" \
  -d "$BODY"
```

## 管理端 API 概览

以下接口经网关转发到 `ten-api-backend`，使用 Session 鉴权，统一响应 `code=0` 表示成功、
`40100` 表示未登录。

| 模块 | 主要接口 |
| --- | --- |
| 用户 `/api/user` | `register`、`login`、`logout`、`get/login`、`add`、`delete`、`update`、`get`、`list`、`list/page` |
| 接口 `/api/interfaceInfo` | `add`、`delete`、`update`、`get`、`get/online`、`list`、`list/online`、`list/page`、`online`、`offline`、`invoke` |
| 调用关系 `/api/userInterfaceInfo` | `add`、`delete`、`update`、`get`、`list`、`list/page` |
| 帖子 `/api/post` | `add`、`delete`、`update`、`get`、`list`、`list/page` |

接口文档（Knife4j / Swagger）仅在非生产环境启用；生产环境已通过
`springfox.documentation.enabled=false` 关闭，避免与 Spring Boot 2.6+ 的
`PathPatternParser` 冲突。

## 前端页面路由

| 路径 | 页面 | 访问要求 |
| --- | --- | --- |
| `/` | 首页（静态接口示例展示） | 公开 |
| `/market` | 接口市场（搜索、分类筛选） | 需登录 |
| `/market/:id` | 接口详情 | 需登录 |
| `/docs/:section?` | 开发文档 | 公开 |
| `/login`、`/register` | 登录 / 注册 | 公开 |
| `/dashboard` | 用户控制台 | 需登录 |
| `/admin/interfaces`、`/admin/users` | 接口管理 / 用户管理 | 需登录且为 `admin` 角色 |

前端登录守卫基于 `localStorage.userInfo` 做跳转控制，真实鉴权由后端 Session 完成；
接口返回 `40100` 或 HTTP 401 时，[request.js](ten-api-frontend/src/utils/request.js)
会清理本地状态并跳转登录页（带 `redirect` 参数回跳）。

## 安全与运维说明

- 严禁提交 `.env`、数据库密码、AK/SK 等真实凭据；`.env` 建议权限设为 `600`
- 生产环境仅开放 HTTP/HTTPS 与必要 SSH 端口，公网部署应在入口终止 HTTPS
- 生产环境已关闭控制台 SQL 日志与高频请求日志（`NoLoggingImpl`、`RequestLogFilter: warn`）
- JVM 堆内存按机器规格限制（backend ≤ 512MB，gateway / interface ≤ 384MB），Redis `maxmemory 256mb`
- Dubbo 已关闭重试（`retries=0`）以避免故障时请求放大；跨服务 RPC 使用 `boundedElastic` 线程池避免阻塞 Netty
- Docker Compose 中所有服务均配置了健康检查与资源限制，容器间通过服务名通信

## 常见问题

**Q：调用开放接口返回 40300 "随机数已使用或重复请求"？**
同一个 `nonce` 在 5 分钟内被重复使用。请每次请求重新生成随机 nonce。

**Q：调用返回 40300 "请求已超时"？**
客户端时间与服务器偏差超过 300 秒，请校准服务器时间或使用 NTP 同步。

**Q：调用返回 40300 "签名错误"？**
检查签名串拼接是否为 `body + "." + secretKey`，且使用 SHA-256 十六进制小写输出；
`body` 必须与请求体完全一致（含空格与换行）。

**Q：`POST /api/name/object` 返回"随机数不符或已使用"？**
该接口会在服务端校验 Redis 中预登记的 nonce。请使用带 `RedisTemplate` 的
`ApiClient` 构造函数，且 Redis 库需与接口服务一致（默认 `database 0`）。

**Q：管理端接口返回 40100？**
登录态已过期（默认 86400 秒）。重新登录即可，Session 存储于 Redis，服务重启不会丢失。

**Q：启动报 Springfox / PathPatternParser 相关的 NPE？**
Springfox 3.0.0 与 Spring Boot 2.6+ 的 `PathPatternParser` 不兼容，生产环境务必保持
`springfox.documentation.enabled=false`，并将 `spring.mvc.pathmatch.matching-strategy`
设为 `ANT_PATH_MATCHER`。

## 相关文档

- [DEPLOYMENT.md](DEPLOYMENT.md)：单机生产部署详细步骤与数据库升级流程
