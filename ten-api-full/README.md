# TEN API

新增 11 个可调用工具 API：密码、UUID、时间戳、文本统计、Base64、JSON、摘要、随机数、日期、单位换算和 HTTP 状态码。
参数、示例与数据库升级步骤见 [API_TOOLS.md](API_TOOLS.md)。接口详情页支持编辑参数并在线试调用。

代码同时维护于 [GitHub](https://github.com/ZzTen0/ten-api-full) 和
[Gitee](https://gitee.com/zzten00/ten-api-full)。后续项目变更完成验证后，应提交并同步推送两个仓库。
本地已配置包含两个推送地址的 `all` 远程，使用 `git push all HEAD` 同步当前分支。
两个目标的推送分别执行，需确认两边均成功。

TEN API 是一个基于 **Spring Boot + Spring Cloud Gateway + Dubbo** 的 API 开放平台，
提供接口发布与管理、用户登录鉴权、AK/SK 签名校验、防重放、限流熔断、调用次数统计与
配额扣减等全套能力，并配套 Web 管理端（接口市场、控制台、管理后台）和 Java 调用 SDK。

## 功能特性

- **API 市场**：浏览在线接口、搜索与分类筛选、查看接口详情与调试示例
- **接口管理**：管理员新增/修改/删除接口，控制接口上线、下线状态
- **用户体系**：注册、登录、注销；基于 Session 的登录态（Redis 存储，支持多实例共享）
- **开放接口鉴权**：AK/SK 签名（HMAC-SHA256 v2）+ 时间戳有效期 + nonce 防重放
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

1. SDK 对方法、原始路径及查询串、AK、时间戳、nonce 和真实请求体摘要计算 HMAC-SHA256。
2. 请求头携带 `accessKey`、`nonce`、`timestamp`、`signatureVersion: 2`、`sign`。
3. 网关验证时间戳窗口、签名和实际请求体（最大 1 MiB），随后原子登记 nonce（601 秒 TTL）。
4. 网关经 Dubbo 扣减额度，清理客户端伪造的身份头，并生成绑定实际请求与用户 ID 的内部签名。
5. 接口服务统一验证内部签名和单次 nonce 后执行业务；客户端不需要访问 Redis。

完整协议及升级要求见 [SECURITY.md](SECURITY.md)，优化分类见 [OPTIMIZATION_PLAN.md](OPTIMIZATION_PLAN.md)。

> 鉴权失败时网关返回 HTTP 403 + `{"code":40300,"message":"...","data":null}`。

## 项目结构

```
ten-api-full/
├── ten-api-common/          # Dubbo 公共接口与模型（被 backend 实现、gateway 消费）
│   └── com.ten.common
│       ├── service/         # InnerUserService、InnerUserInterfaceInfoService
│       └── model/vo/        # InvokeUserVO
├── api-client-sdk/          # 开放接口调用 SDK（0.0.2）
│   └── com.ten.apiclientsdk
│       ├── ApiClientConfig  # 自动装配入口（spring.factories）
│       ├── client/ApiClient # 调用客户端
│       ├── utils/SignUtils  # v2 签名 / AK、SK 生成
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
│       ├── filter/          # ApiAuthInvokeFilter、RequestLogFilter
│       └── config/          # GatewayRateLimitConfig（apiKeyResolver）
├── ten-api-interface/       # 示例开放接口服务（8123）
│   └── com.ten.tenapiinterface
│       ├── controller/      # NameController
│       ├── service/impl/    # NameServiceImpl（入口由统一内部签名过滤器保护）
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

以下命令均从 `ten-api-full/` 项目根目录执行。在本地工作区中先执行：

```powershell
cd D:\CODE\Backstage-end\ten-api-full
```

目录布局参考 [ZzTen0/ten-api-full](https://github.com/ZzTen0/ten-api-full)。
本地附加的 `frontend-preview/` 保存页面预览和设计笔记；`.env` 与密码文件
也保存在本目录，继续忽略提交。

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

SDK 自动生成 v2 签名和 nonce，客户端无需连接服务端 Redis。所有调用都应经过网关。

### 4. ApiClient 方法一览

| 方法 | 请求 | 说明 |
| --- | --- | --- |
| `getNameByGET(String name)` | `GET /api/name/ga?name=xxx` | 自动签名并编码查询参数 |
| `getNameByPOSTPath(String name)` | `POST /api/name/path/{name}` | 自动签名并编码路径参数 |
| `getNameByPOSTJson(Username username)` | `POST /api/name/object` | 按实际 UTF-8 JSON 字节签名 |
| `request(String method, String path, Object body)` | 自定义相对路径 | JSON 字符串或对象，只序列化一次 |

### 5. 签名与请求头规则

请求头包含 `accessKey`、`nonce`、`timestamp`、`signatureVersion: 2`、`sign`。
使用 HMAC-SHA256 绑定方法、原始编码路径及查询串、AK、时间戳、nonce 和真实请求体摘要。
不再发送 `body` 头；SK 只用于本地计算，不能随请求发送。
完整规范与迁移步骤见 [SECURITY.md](SECURITY.md)。

### 6. 生成 AK / SK

`SignUtils` 已提供生成能力（基于 UUID）：

```java
SignUtils signUtils = new SignUtils();
String accessKey = signUtils.generateAccessKey();
String secretKey = signUtils.generateSecretKey();
```

## 接口调用示例（curl）

以下示例依赖 Bash、OpenSSL 与 sha256sum。必须使用当前 v2 协议：

```bash
BODY='{"username":"ten"}'
AK='your-access-key'
SECRET='your-secret-key'
TARGET='/api/name/object'
NONCE=$(openssl rand -hex 16)
TS=$(date +%s)
BODY_HASH=$(printf '%s' "$BODY" | sha256sum | awk '{print $1}')
SIGN=$(printf 'TEN-API-V2\nPOST\n%s\n%s\n%s\n%s\n%s' \
  "$TARGET" "$AK" "$TS" "$NONCE" "$BODY_HASH" \
  | openssl dgst -sha256 -hmac "$SECRET" | awk '{print $NF}')

curl -X POST "http://localhost:8090$TARGET" \
  -H "Content-Type: application/json; charset=UTF-8" \
  -H "accessKey: $AK" \
  -H "nonce: $NONCE" \
  -H "timestamp: $TS" \
  -H "signatureVersion: 2" \
  -H "sign: $SIGN" \
  --data-binary "$BODY"
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
nonce 已被使用，请每次请求重新生成。防重放记录保留 601 秒以覆盖完整时间戳窗口。

**Q：调用返回 40300 "请求已超时"？**
客户端时间与服务器偏差超过 300 秒，请校准服务器时间或使用 NTP 同步。

**Q：调用返回 40300 "签名错误"？**
检查是否发送 signatureVersion: 2，规范字符串与实际请求的方法、原始路径、查询串及请求体是否完全一致。参见 [SECURITY.md](SECURITY.md)。

**Q：管理端接口返回 40100？**
登录态已过期（默认 86400 秒）。重新登录即可，Session 存储于 Redis，服务重启不会丢失。

**Q：启动报 Springfox / PathPatternParser 相关的 NPE？**
Springfox 3.0.0 与 Spring Boot 2.6+ 的 `PathPatternParser` 不兼容，生产环境务必保持
`springfox.documentation.enabled=false`，并将 `spring.mvc.pathmatch.matching-strategy`
设为 `ANT_PATH_MATCHER`。

## 相关文档

- [DEPLOYMENT.md](DEPLOYMENT.md)：单机生产部署详细步骤与数据库升级流程
