import test from 'node:test'
import assert from 'node:assert/strict'
import { parameterExample, prepareExample } from './apiExamples.js'

test('catalog examples retain their numeric and boolean types', () => {
  assert.deepEqual(parameterExample('[{"name":"count","example":3},{"name":"hyphens","example":false},{"name":"optional"}]'),
    { count: 3, hyphens: false })
})
test('GET examples encode query parameters and omit the request body', () => {
  assert.deepEqual(prepareExample({ method: 'GET', url: '/api/tools/uuid' }, '{"count":3,"hyphens":false}'),
    { method: 'GET', path: '/api/tools/uuid?count=3&hyphens=false', body: '' })
})
test('POST examples retain UTF-8 JSON and substitute encoded path parameters', () => {
  assert.deepEqual(prepareExample({ method: 'POST', url: '/api/name/path/{name}' }, '{"name":"张 三"}'),
    { method: 'POST', path: '/api/name/path/%E5%BC%A0%20%E4%B8%89', body: '{}' })
  assert.equal(prepareExample({ method: 'POST', url: '/api/tools/text/stats' }, '{"text":"你好"}').body, '{"text":"你好"}')
})
test('malformed parameters and missing path variables are rejected', () => {
  for (const text of ['[]', 'null', 'true', 'invalid']) assert.throws(() => prepareExample({}, text))
  assert.throws(() => prepareExample({ url: '/api/name/path/{name}' }, '{}'))
})
