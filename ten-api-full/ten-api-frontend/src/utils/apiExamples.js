/**
 * 解析接口目录中的参数描述，兼容空值、旧结构和无效 JSON。
 *
 * @param {string|undefined} raw 序列化后的参数描述数组
 * @returns {Array<object>} 参数描述列表，解析失败时返回空数组
 */
export function parseParameterSchema(raw) {
  try {
    const schema = JSON.parse(raw || '[]')
    return Array.isArray(schema) ? schema : []
  } catch {
    return []
  }
}

/**
 * 提取参数描述中的示例值，保留数字和布尔值等原始类型。
 *
 * @param {string|undefined} raw 序列化后的参数描述数组
 * @returns {object} 用于填充试调用编辑器的示例参数对象
 */
export function parameterExample(raw) {
  return Object.fromEntries(parseParameterSchema(raw)
    .filter((field) => field.example !== undefined)
    .map((field) => [field.name, field.example]))
}

/**
 * 根据编辑器参数生成 SDK 与 cURL 示例的路径和请求体。
 *
 * @param {object} detail 接口元数据，包含 method 和 url
 * @param {string} text 用户填写的 JSON 对象字符串
 * @returns {{method: string, path: string, body: string}} 编码后的路径及请求体
 * @throws {Error} 参数不是合法 JSON 对象或缺少路径变量
 */
export function prepareExample(detail, text) {
  const params = JSON.parse(text)
  if (!params || Array.isArray(params) || typeof params !== 'object') {
    throw new Error('请输入 JSON 对象')
  }
  const remaining = { ...params }
  const method = (detail?.method || 'GET').toUpperCase()
  let path = detail?.url || '/api/...'
  if (/^https?:\/\//i.test(path)) path = new URL(path).pathname
  path = path.replace(/\{([A-Za-z][A-Za-z0-9_]*)\}/g, (_, key) => {
    if (remaining[key] === undefined || remaining[key] === null) throw new Error(`缺少路径参数 ${key}`)
    const value = encodeURIComponent(String(remaining[key]))
    delete remaining[key]
    return value
  })
  if (method === 'GET') {
    const query = Object.entries(remaining).filter(([, value]) => value !== null)
      .map(([key, value]) => `${encodeURIComponent(key)}=${encodeURIComponent(String(value))}`).join('&')
    if (query) path += `?${query}`
  }
  return { method, path, body: method === 'GET' ? '' : JSON.stringify(remaining) }
}

/**
 * 内置工具的静态响应示例，仅说明响应结构，不代表真实调用结果。
 */
export const responseExamples = {
  '/api/tools/password': { password: 'aB7!qR8@tY2#uI6$', length: 16 },
  '/api/tools/uuid': { uuids: ['b3e968fe-5b92-4639-8aa8-712ae632017e'], count: 1 },
  '/api/tools/timestamp': { timestamp: 0, unit: 'seconds', dateTime: '1970-01-01T00:00:00Z', zoneId: 'UTC' },
  '/api/tools/text/stats': { characters: 8, nonWhitespaceCharacters: 7, chineseCharacters: 2, englishWords: 1, lines: 1 },
  '/api/tools/base64': { operation: 'encode', result: 'VEVOIEFQSQ==' },
  '/api/tools/json': { valid: true, mode: 'compress', result: '{"ok":true}' },
  '/api/tools/digest': { algorithm: 'SHA-256', digest: 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855' },
  '/api/tools/random': { numbers: [3, 9, 1], min: 1, max: 10, count: 3, unique: true },
  '/api/tools/date': { startDate: '2026-10-01', endDate: '2026-10-27', days: 26 },
  '/api/tools/units': { category: 'length', from: 'm', to: 'cm', value: 1, result: 100 },
  '/api/tools/http-status': { code: 404, name: 'NOT_FOUND', reason: 'Not Found', category: '客户端错误', description: '请求的资源不存在' },
}
