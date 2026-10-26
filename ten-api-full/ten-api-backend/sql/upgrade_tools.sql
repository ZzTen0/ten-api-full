-- Built-in tools and legacy name examples. Apply after ddl.sql and db.sql.
-- Repeatable: existing active registrations (including deliberately offline ones) are preserved.
SET NAMES utf8mb4;
START TRANSACTION;
SET @catalog_owner = COALESCE((SELECT MIN(id) FROM `user` WHERE user_role = 'admin' AND is_delete = 0), 1);

INSERT INTO interface_info (name, description, url, request_params, request_header, response_header, status, method, user_id)
SELECT '随机密码生成', '工具：按长度和字符类型生成随机密码，保证包含每种选定字符', '/api/tools/password',
       '[{"name":"length","type":"integer","required":false,"description":"长度 4～128，默认 16","example":16},{"name":"lowercase","type":"boolean","required":false,"description":"包含小写字母，默认 true","example":true},{"name":"uppercase","type":"boolean","required":false,"description":"包含大写字母，默认 true","example":true},{"name":"digits","type":"boolean","required":false,"description":"包含数字，默认 true","example":true},{"name":"symbols","type":"boolean","required":false,"description":"包含特殊字符，默认 false","example":false}]', '{"Content-Type":"application/json","signatureVersion":"2"}',
       '{"Content-Type":"application/json"}', 1, 'POST', @catalog_owner
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM interface_info WHERE url = '/api/tools/password' AND method = 'POST' AND is_delete = 0);

INSERT INTO interface_info (name, description, url, request_params, request_header, response_header, status, method, user_id)
SELECT 'UUID 生成', '工具：批量生成 UUID v4，可选择是否保留连字符', '/api/tools/uuid',
       '[{"name":"count","type":"integer","required":false,"description":"数量 1～100，默认 1","example":3},{"name":"hyphens","type":"boolean","required":false,"description":"保留连字符，默认 true","example":true}]', '{"Content-Type":"application/json","signatureVersion":"2"}',
       '{"Content-Type":"application/json"}', 1, 'GET', @catalog_owner
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM interface_info WHERE url = '/api/tools/uuid' AND method = 'GET' AND is_delete = 0);

INSERT INTO interface_info (name, description, url, request_params, request_header, response_header, status, method, user_id)
SELECT '时间戳转换', '工具：日期时间与秒或毫秒时间戳互转，支持 IANA 时区', '/api/tools/timestamp',
       '[{"name":"operation","type":"string","required":false,"description":"toTimestamp 或 toDateTime，默认 toTimestamp","example":"toTimestamp"},{"name":"dateTime","type":"string","required":false,"description":"toTimestamp 时必填，ISO 本地日期时间","example":"2026-10-27T12:00:00"},{"name":"timestamp","type":"integer","required":false,"description":"toDateTime 时必填"},{"name":"unit","type":"string","required":false,"description":"seconds 或 milliseconds，默认 seconds","example":"seconds"},{"name":"zoneId","type":"string","required":false,"description":"IANA 时区，默认 UTC","example":"Asia/Hong_Kong"}]', '{"Content-Type":"application/json","signatureVersion":"2"}',
       '{"Content-Type":"application/json"}', 1, 'POST', @catalog_owner
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM interface_info WHERE url = '/api/tools/timestamp' AND method = 'POST' AND is_delete = 0);

INSERT INTO interface_info (name, description, url, request_params, request_header, response_header, status, method, user_id)
SELECT '文本统计', '工具：统计 Unicode 字符、非空白字符、汉字、英文单词及行数', '/api/tools/text/stats',
       '[{"name":"text","type":"string","required":true,"description":"待统计文本，最多 65536 个 UTF-16 代码单元","example":"你好 World!\\nHello API"}]', '{"Content-Type":"application/json","signatureVersion":"2"}',
       '{"Content-Type":"application/json"}', 1, 'POST', @catalog_owner
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM interface_info WHERE url = '/api/tools/text/stats' AND method = 'POST' AND is_delete = 0);

INSERT INTO interface_info (name, description, url, request_params, request_header, response_header, status, method, user_id)
SELECT 'Base64 编解码', '工具：标准 Base64 与 UTF-8 文本互相转换', '/api/tools/base64',
       '[{"name":"operation","type":"string","required":false,"description":"encode 或 decode，默认 encode","example":"encode"},{"name":"text","type":"string","required":true,"description":"UTF-8 文本或合法的 Base64 字符串","example":"你好 TEN API"}]', '{"Content-Type":"application/json","signatureVersion":"2"}',
       '{"Content-Type":"application/json"}', 1, 'POST', @catalog_owner
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM interface_info WHERE url = '/api/tools/base64' AND method = 'POST' AND is_delete = 0);

INSERT INTO interface_info (name, description, url, request_params, request_header, response_header, status, method, user_id)
SELECT 'JSON 格式化', '工具：校验并格式化或压缩 JSON，拒绝重复键及超过 128 层的嵌套', '/api/tools/json',
       '[{"name":"mode","type":"string","required":false,"description":"format 或 compress，默认 format","example":"format"},{"name":"text","type":"string","required":true,"description":"JSON 文本，最多 65536 个 UTF-16 代码单元","example":"{\\"name\\":\\"TEN API\\",\\"enabled\\":true}"}]', '{"Content-Type":"application/json","signatureVersion":"2"}',
       '{"Content-Type":"application/json"}', 1, 'POST', @catalog_owner
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM interface_info WHERE url = '/api/tools/json' AND method = 'POST' AND is_delete = 0);

INSERT INTO interface_info (name, description, url, request_params, request_header, response_header, status, method, user_id)
SELECT '文本摘要计算', '工具：计算 UTF-8 文本的 MD5、SHA-1、SHA-256 或 SHA-512 摘要', '/api/tools/digest',
       '[{"name":"algorithm","type":"string","required":false,"description":"MD5、SHA-1、SHA-256、SHA-512，默认 SHA-256","example":"SHA-256"},{"name":"text","type":"string","required":true,"description":"待计算摘要的文本","example":"TEN API"}]', '{"Content-Type":"application/json","signatureVersion":"2"}',
       '{"Content-Type":"application/json"}', 1, 'POST', @catalog_owner
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM interface_info WHERE url = '/api/tools/digest' AND method = 'POST' AND is_delete = 0);

INSERT INTO interface_info (name, description, url, request_params, request_header, response_header, status, method, user_id)
SELECT '随机整数生成', '工具：在包含端点的整数范围内生成随机数，可选择不重复', '/api/tools/random',
       '[{"name":"min","type":"integer","required":false,"description":"下界，范围 ±10亿，默认 0","example":1},{"name":"max","type":"integer","required":false,"description":"上界，范围 ±10亿，默认 100","example":100},{"name":"count","type":"integer","required":false,"description":"数量 1～1000，默认 1","example":5},{"name":"unique","type":"boolean","required":false,"description":"是否不重复，默认 false","example":true}]', '{"Content-Type":"application/json","signatureVersion":"2"}',
       '{"Content-Type":"application/json"}', 1, 'GET', @catalog_owner
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM interface_info WHERE url = '/api/tools/random' AND method = 'GET' AND is_delete = 0);

INSERT INTO interface_info (name, description, url, request_params, request_header, response_header, status, method, user_id)
SELECT '日期计算', '工具：计算日期间隔（有符号天数），或对日期加减天数', '/api/tools/date',
       '[{"name":"operation","type":"string","required":false,"description":"difference 或 add，默认 difference","example":"difference"},{"name":"startDate","type":"string","required":false,"description":"difference 时必填，yyyy-MM-dd","example":"2026-10-01"},{"name":"endDate","type":"string","required":false,"description":"difference 时必填，yyyy-MM-dd","example":"2026-10-27"},{"name":"date","type":"string","required":false,"description":"add 时必填，yyyy-MM-dd"},{"name":"days","type":"integer","required":false,"description":"add 的增减天数，范围 ±365000，默认 0"}]', '{"Content-Type":"application/json","signatureVersion":"2"}',
       '{"Content-Type":"application/json"}', 1, 'POST', @catalog_owner
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM interface_info WHERE url = '/api/tools/date' AND method = 'POST' AND is_delete = 0);

INSERT INTO interface_info (name, description, url, request_params, request_header, response_header, status, method, user_id)
SELECT '单位换算', '工具：长度、重量和温度单位转换，使用高精度十进制计算', '/api/tools/units',
       '[{"name":"category","type":"string","required":true,"description":"length、weight 或 temperature","example":"length"},{"name":"from","type":"string","required":true,"description":"源单位，详见工具接口文档","example":"m"},{"name":"to","type":"string","required":true,"description":"目标单位，详见工具接口文档","example":"cm"},{"name":"value","type":"number","required":true,"description":"待转换数值，绝对值不超过 10^12","example":1}]', '{"Content-Type":"application/json","signatureVersion":"2"}',
       '{"Content-Type":"application/json"}', 1, 'POST', @catalog_owner
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM interface_info WHERE url = '/api/tools/units' AND method = 'POST' AND is_delete = 0);

INSERT INTO interface_info (name, description, url, request_params, request_header, response_header, status, method, user_id)
SELECT 'HTTP 状态码查询', '工具：查询标准 HTTP 状态码的名称、类别、含义及常见使用场景', '/api/tools/http-status',
       '[{"name":"code","type":"integer","required":true,"description":"已收录的标准 HTTP 状态码，例如 200、404、500","example":404}]', '{"Content-Type":"application/json","signatureVersion":"2"}',
       '{"Content-Type":"application/json"}', 1, 'GET', @catalog_owner
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM interface_info WHERE url = '/api/tools/http-status' AND method = 'GET' AND is_delete = 0);

INSERT INTO interface_info (name, description, url, request_params, request_header, response_header, status, method, user_id)
SELECT '名字查询（GET）', '示例：通过查询参数返回名字', '/api/name/ga',
       '[{"name":"name","type":"string","required":true,"description":"名字","example":"ten"}]', '{"Content-Type":"application/json","signatureVersion":"2"}',
       '{"Content-Type":"text/plain"}', 1, 'GET', @catalog_owner
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM interface_info WHERE url = '/api/name/ga' AND method = 'GET' AND is_delete = 0);

INSERT INTO interface_info (name, description, url, request_params, request_header, response_header, status, method, user_id)
SELECT '名字查询（路径）', '示例：通过路径参数返回名字', '/api/name/path/{name}',
       '[{"name":"name","type":"string","required":true,"description":"名字，作为路径参数","example":"ten"}]', '{"Content-Type":"application/json","signatureVersion":"2"}',
       '{"Content-Type":"text/plain"}', 1, 'POST', @catalog_owner
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM interface_info WHERE url = '/api/name/path/{name}' AND method = 'POST' AND is_delete = 0);

INSERT INTO interface_info (name, description, url, request_params, request_header, response_header, status, method, user_id)
SELECT '名字查询（JSON）', '示例：通过 JSON 请求体返回名字', '/api/name/object',
       '[{"name":"username","type":"string","required":true,"description":"用户名","example":"ten"}]', '{"Content-Type":"application/json","signatureVersion":"2"}',
       '{"Content-Type":"text/plain"}', 1, 'POST', @catalog_owner
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM interface_info WHERE url = '/api/name/object' AND method = 'POST' AND is_delete = 0);

COMMIT;
