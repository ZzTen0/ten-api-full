# TEN API

TEN API 是一个基于 Spring Boot、Spring Cloud Gateway 和 Dubbo 的 API 开放平台，
提供接口管理、用户鉴权、签名校验、调用限流及调用次数统计等能力。

## 项目结构

| 模块 | 说明 |
| --- | --- |
| `ten-api-frontend` | Vue 3 管理端和接口市场 |
| `ten-api-gateway` | API 网关、签名鉴权、限流与防重放 |
| `ten-api-backend` | 用户、接口及调用额度管理，提供 Dubbo 服务 |
| `ten-api-interface` | 示例开放接口服务 |
| `ten-api-common` | Dubbo 公共接口与模型 |
| `api-client-sdk` | API 调用客户端 SDK |

## 技术栈

- Java 8、Spring Boot、Spring Cloud Gateway
- Dubbo、Zookeeper
- MySQL、Redis、MyBatis-Plus
- Vue 3、Vite、Element Plus
- Docker Compose、Nginx

## 快速部署

准备环境变量：

```bash
cp .env.example .env
```

修改 `.env` 中的密码，然后按顺序构建 Java 模块：

```bash
cd ten-api-common && mvn clean install -DskipTests
cd ../api-client-sdk && mvn clean install -DskipTests
cd ../ten-api-backend && mvn clean package -DskipTests
cd ../ten-api-gateway && mvn clean package -DskipTests
cd ../ten-api-interface && mvn clean package -DskipTests
cd ..
```

启动全部服务：

```bash
docker compose --env-file .env up -d --build
```

启动完成后访问 `http://服务器地址/`。详细配置及数据库升级步骤参见
[DEPLOYMENT.md](DEPLOYMENT.md)。

## 安全说明

请勿提交 `.env`、数据库密码、AK/SK 或其他真实凭据。生产环境仅开放 HTTP/HTTPS
和必要的 SSH 端口。
