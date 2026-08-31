# TEN API 单机部署

该编排面向 4 核 8 GB Linux 服务器。运行时包含 MySQL、Redis、Zookeeper、
backend、interface、gateway 和 frontend 七个容器，仅向宿主机发布前端 HTTP 端口。

## 1. 准备环境变量

```bash
cp .env.example .env
chmod 600 .env
```

修改 `.env` 中所有 `replace_with_...` 项。

## 2. 构建 Java 包

按依赖顺序执行：

```bash
cd ten-api-common && mvn clean install -DskipTests
cd ../api-client-sdk && mvn clean install -DskipTests
cd ../ten-api-backend && mvn clean package -DskipTests
cd ../ten-api-gateway && mvn clean package -DskipTests
cd ../ten-api-interface && mvn clean package -DskipTests
cd ..
```

前端由 Docker 多阶段镜像执行 `npm ci` 和 `npm run build`，宿主机无需安装 Node.js。

## 3. 校验并启动

```bash
docker compose --env-file .env config
docker compose --env-file .env up -d --build
docker compose ps
```

浏览器访问 `http://服务器地址/`。应用 API 通过同一地址下的 `/api/**` 访问。

## 4. 数据库说明

全新 MySQL 数据卷会自动执行 `sql/ddl.sql` 和 `sql/db.sql`。初始化脚本只在数据目录
为空时运行。

已有数据库部署前应先备份，再执行并发额度升级脚本：

```bash
docker compose exec -T mysql sh -c \
  'exec mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE"' > ten_api_backup.sql

docker compose exec -T mysql sh -c \
  'exec mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE"' \
  < ten-api-backend/sql/upgrade_concurrency.sql
```

## 5. 运维命令

```bash
docker compose ps
docker compose logs -f --tail=200 ten-api-gateway
docker compose restart ten-api-backend
docker compose down
```

不要对已有数据执行 `docker compose down -v`。公网部署时应在入口配置 HTTPS，并在
防火墙中只开放 HTTP/HTTPS 和必要的 SSH 端口。
