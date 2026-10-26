export function parseParameterSchema(raw) {
  try {
    const schema = JSON.parse(raw || '[]')
    return Array.isArray(schema) ? schema : []
  } catch {
    return []
  }
}

export function parameterExample(raw) {
  return Object.fromEntries(parseParameterSchema(raw)
    .filter((field) => field.example !== undefined)
    .map((field) => [field.name, field.example]))
}

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
