# 内置工具 API

本版新增 11 个工具 API，不包含“一言”。全部在本地实现，无第三方密钥或联网调用。
统一通过网关访问；使用 [v2 签名](SECURITY.md)，或直接在接口市场详情页编辑 JSON 参数试调用。

## 目录

| API | 方法 | 网关路径 |
| --- | --- | --- |
| 随机密码生成 | POST | `/api/tools/password` |
| UUID 生成 | GET | `/api/tools/uuid` |
| 时间戳转换 | POST | `/api/tools/timestamp` |
| 文本统计 | POST | `/api/tools/text/stats` |
| Base64 编解码 | POST | `/api/tools/base64` |
| JSON 格式化 | POST | `/api/tools/json` |
| 文本摘要计算 | POST | `/api/tools/digest` |
| 随机整数生成 | GET | `/api/tools/random` |
| 日期计算 | POST | `/api/tools/date` |
| 单位换算 | POST | `/api/tools/units` |
| HTTP 状态码查询 | GET | `/api/tools/http-status` |

## 返回与额度

成功返回 `{"code":0,"message":"success","data":{...}}`；
参数错误返回 HTTP 400 和 `{"code":40000,"message":"参数说明","data":null}`。
网关鉴权失败为 HTTP 403，接口未注册或已下线为 HTTP 404，依赖不可用为 HTTP 503。
所有工具响应标记 `Cache-Control: no-store`。

网关按请求方法和路径解析真实接口 ID，只有上线接口才可调用，并按用户与接口分别扣减额度。
首次调用每个接口默认分配 100 次，首次请求成功扣减后剩余 99 次。
当前仍采用转发前预扣额度：下游参数错误、超时或失败也可能扣减一次；本版没有实现失败补偿。
GET、POST 均要求完整 v2 签名；调用方不需要连接服务端 Redis。

## 调用与参数

以下 POST 示例是实际请求体。GET 的 JSON 示例表示查询参数；在线试调用页面会自动转换。
Java SDK 可使用 `client.request("POST", "/api/tools/base64", "{\"text\":\"hello\"}")`；
GET 使用 `client.request("GET", "/api/tools/uuid?count=3&hyphens=false", "")`。
SDK 地址应设置为当前部署的 HTTP/HTTPS 入口，或本地网关 `http://localhost:8090`。

### 1. 随机密码生成

`POST /api/tools/password`

按长度和字符类型生成随机密码，保证包含每种选定字符。

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `length` | integer | 否 | 长度 4～128，默认 16 |
| `lowercase` | boolean | 否 | 包含小写字母，默认 true |
| `uppercase` | boolean | 否 | 包含大写字母，默认 true |
| `digits` | boolean | 否 | 包含数字，默认 true |
| `symbols` | boolean | 否 | 包含特殊字符，默认 false |

```json
{
  "length": 16,
  "lowercase": true,
  "uppercase": true,
  "digits": true,
  "symbols": false
}
```

### 2. UUID 生成

`GET /api/tools/uuid`

批量生成 UUID v4，可选择是否保留连字符。

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `count` | integer | 否 | 数量 1～100，默认 1 |
| `hyphens` | boolean | 否 | 保留连字符，默认 true |

```json
{
  "count": 3,
  "hyphens": true
}
```

### 3. 时间戳转换

`POST /api/tools/timestamp`

日期时间与秒或毫秒时间戳互转，支持 IANA 时区。

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `operation` | string | 否 | toTimestamp 或 toDateTime，默认 toTimestamp |
| `dateTime` | string | 否 | toTimestamp 时必填，ISO 本地日期时间 |
| `timestamp` | integer | 否 | toDateTime 时必填 |
| `unit` | string | 否 | seconds 或 milliseconds，默认 seconds |
| `zoneId` | string | 否 | IANA 时区，默认 UTC |

```json
{
  "operation": "toTimestamp",
  "dateTime": "2026-10-27T12:00:00",
  "unit": "seconds",
  "zoneId": "Asia/Hong_Kong"
}
```

### 4. 文本统计

`POST /api/tools/text/stats`

统计 Unicode 字符、非空白字符、汉字、英文单词及行数。

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `text` | string | 是 | 待统计文本，最多 65536 个 UTF-16 代码单元 |

```json
{
  "text": "你好 World!\nHello API"
}
```

### 5. Base64 编解码

`POST /api/tools/base64`

标准 Base64 与 UTF-8 文本互相转换。

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `operation` | string | 否 | encode 或 decode，默认 encode |
| `text` | string | 是 | UTF-8 文本或合法的 Base64 字符串 |

```json
{
  "operation": "encode",
  "text": "你好 TEN API"
}
```

### 6. JSON 格式化

`POST /api/tools/json`

校验并格式化或压缩 JSON，拒绝重复键及超过 128 层的嵌套。

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `mode` | string | 否 | format 或 compress，默认 format |
| `text` | string | 是 | JSON 文本，最多 65536 个 UTF-16 代码单元 |

```json
{
  "mode": "format",
  "text": "{\"name\":\"TEN API\",\"enabled\":true}"
}
```

### 7. 文本摘要计算

`POST /api/tools/digest`

计算 UTF-8 文本的 MD5、SHA-1、SHA-256 或 SHA-512 摘要。

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `algorithm` | string | 否 | MD5、SHA-1、SHA-256、SHA-512，默认 SHA-256 |
| `text` | string | 是 | 待计算摘要的文本 |

```json
{
  "algorithm": "SHA-256",
  "text": "TEN API"
}
```

### 8. 随机整数生成

`GET /api/tools/random`

在包含端点的整数范围内生成随机数，可选择不重复。

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `min` | integer | 否 | 下界，范围 ±10亿，默认 0 |
| `max` | integer | 否 | 上界，范围 ±10亿，默认 100 |
| `count` | integer | 否 | 数量 1～1000，默认 1 |
| `unique` | boolean | 否 | 是否不重复，默认 false |

```json
{
  "min": 1,
  "max": 100,
  "count": 5,
  "unique": true
}
```

### 9. 日期计算

`POST /api/tools/date`

计算日期间隔（有符号天数），或对日期加减天数。

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `operation` | string | 否 | difference 或 add，默认 difference |
| `startDate` | string | 否 | difference 时必填，yyyy-MM-dd |
| `endDate` | string | 否 | difference 时必填，yyyy-MM-dd |
| `date` | string | 否 | add 时必填，yyyy-MM-dd |
| `days` | integer | 否 | add 的增减天数，范围 ±365000，默认 0 |

```json
{
  "operation": "difference",
  "startDate": "2026-10-01",
  "endDate": "2026-10-27"
}
```

### 10. 单位换算

`POST /api/tools/units`

长度、重量和温度单位转换，使用高精度十进制计算。

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `category` | string | 是 | length、weight 或 temperature |
| `from` | string | 是 | 源单位，详见工具接口文档 |
| `to` | string | 是 | 目标单位，详见工具接口文档 |
| `value` | number | 是 | 待转换数值，绝对值不超过 10^12 |

```json
{
  "category": "length",
  "from": "m",
  "to": "cm",
  "value": 1
}
```

### 11. HTTP 状态码查询

`GET /api/tools/http-status`

查询标准 HTTP 状态码的名称、类别、含义及常见使用场景。

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `code` | integer | 是 | 已收录的标准 HTTP 状态码，例如 200、404、500 |

```json
{
  "code": 404
}
```

## 进一步约定

- 密码：使用 SecureRandom，每种选定的字符类型至少出现一次，默认包含大小写字母与数字。
- UUID：版本为 v4，数量最多 100。
- 时间戳：支持负值；dateTime 是不带偏移的 ISO 本地时间，zoneId 为 IANA 时区（默认 UTC）。转日期时 operation=toDateTime 并提供 timestamp；unit 必须明确为 seconds 或 milliseconds。夏令时缺失或重复的本地时间会被拒绝。
- 文本：characters 按 Unicode 码点计算，emoji 不会拆成两个字符；nonWhitespaceCharacters 排除空白。英文单词以英文字母段识别，可包含内部连字符或撇号。空文本为 0 行，CRLF 计作一个换行，末尾换行保留一个空行。
- Base64：标准 Base64；解码时拒绝无效编码和非 UTF-8 二进制内容。
- JSON：支持对象、数组及 JSON 标量；拒绝空白输入、重复键、多个根值及超过 128 层的嵌套。保留大整数与小数精度。
- 文本类输入上限为 65536 个 UTF-16 代码单元；开放接口 HTTP 请求体上限为 1 MiB。
- 摘要：支持 MD5、SHA-1、SHA-256、SHA-512，返回小写十六进制摘要；用于文本摘要计算。
- 随机数：min/max 包含端点，范围均为 [-1000000000, 1000000000]，count 为 1～1000；unique=true 时数量不能超过可选整数个数。
- 日期：difference 返回 endDate-startDate 的有符号天数。add 需要 date 和 days（±365000），使用 ISO 日历并校验真实日期。
- 长度单位：mm、cm、m、km、in、ft、yd、mi。重量单位：mg、g、kg、lb、oz。长度与重量不能为负。
- 温度单位：c、f、k，不接受低于绝对零度的数值。单位大小写不敏感，跨类别换算会被拒绝。数值绝对值最多 10^12，有效数字与十进制 scale 均限制为最多 100，计算精度为 DECIMAL128。
- HTTP 状态码：查询框架收录的标准状态码；未知码返回 400。

## 初始化与升级

全新 Docker 数据卷会按顺序执行 ddl.sql、db.sql、upgrade_tools.sql，注册这 11 个接口与原有 3 个名字示例。
已有数据库请先备份，再执行 [upgrade_tools.sql](ten-api-backend/sql/upgrade_tools.sql)。
脚本可重复执行；已经存在的同方法、同路径注册（包括管理员主动下线的接口）不会被覆盖。
旧的演示记录不会在升级时删除，管理员可按需要下线。

在项目根目录执行：

```bash
docker compose exec -T mysql sh -c \
  'exec mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE"' \
  < ten-api-backend/sql/upgrade_tools.sql
```

部署时同步发布 common、SDK、backend、gateway、interface。
后端在线试调用使用 api.client.gateway-host，Docker 生产配置默认为 http://ten-api-gateway:8090。
本地直接运行 backend 时使用 http://localhost:8090。可用 API_CLIENT_GATEWAY_HOST 环境变量覆盖。

## 验证

本版通过 86 项 Java 测试、4 项前端示例测试，并在独立 MySQL 8 实例中验证了建表、
14 个目录注册（11 个工具＋3 个原有示例）、重复执行无重复记录及保留下线状态。
测试数据库独立于本地项目数据库。Redis 与 Dubbo 使用 mock，未进行完整容器联调。

构建时先安装 common 和 SDK，再运行各模块的针对性测试，避免执行依赖真实基础设施的旧演示测试：

```bash
mvn -f ten-api-common/pom.xml install -DskipTests
mvn -f api-client-sdk/pom.xml install -Dtest=RequestSignatureTest,ApiClientSigningTest
mvn -f ten-api-gateway/pom.xml test -Dtest=ApiAuthInvokeFilterTest,GatewayRateLimitConfigTest
mvn -f ten-api-interface/pom.xml test -Dtest=ToolServiceTest,ToolControllerTest,GatewayAuthenticationFilterTest
mvn -f ten-api-backend/pom.xml test -Dtest=InterfaceInvocationServiceTest,InnerInterfaceInfoServiceImplTest,InnerUserInterfaceInfoServiceImplTest,UserControllerTest,InterfaceInfoControllerTest,LogInterceptorSecurityTest
cd ten-api-frontend
npm test
npm run build
```
