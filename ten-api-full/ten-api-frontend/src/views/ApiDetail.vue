<template>
  <div class="page">
    <div class="container detail-container">
      <div class="back-btn" @click="goBack">
        <el-icon><ArrowLeft /></el-icon>
        <span>返回接口市场</span>
      </div>

      <div v-loading="loading" class="detail-card">
        <template v-if="detail">
          <div class="detail-head">
            <MethodTag :method="detail.method" />
            <span class="detail-path">{{ detail.url }}</span>
            <el-tag
              :type="isOnline(detail) ? 'success' : 'info'"
              size="small"
              class="detail-status"
            >
              {{ isOnline(detail) ? '在线' : '离线' }}
            </el-tag>
          </div>
          <h2 class="detail-name">{{ detail.name }}</h2>
          <p class="detail-desc">{{ detail.description || '暂无描述' }}</p>

          <el-tabs v-model="activeTab" class="detail-tabs">
            <el-tab-pane label="请求参数" name="params">
              <el-table :data="paramList" border stripe size="default">
                <el-table-column prop="name" label="参数名" min-width="140" />
                <el-table-column prop="type" label="类型" width="140" />
                <el-table-column label="是否必填" width="120">
                  <template #default="{ row }">
                    <el-tag :type="row.required ? 'danger' : 'info'" size="small" effect="plain">
                      {{ row.required ? '是' : '否' }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="description" label="说明" min-width="200" />
                <template #empty>
                  <el-empty description="该接口暂无请求参数" :image-size="80" />
                </template>
              </el-table>
            </el-tab-pane>

            <el-tab-pane label="调用示例" name="invoke">
              <div class="code-block">
                <div class="code-block__title">cURL</div>
                <pre class="code-block__pre"><code>{{ curlExample }}</code></pre>
              </div>
              <div class="code-block">
                <div class="code-block__title">Java SDK</div>
                <pre class="code-block__pre"><code>{{ javaExample }}</code></pre>
              </div>
            </el-tab-pane>

            <el-tab-pane label="响应示例" name="response">
              <pre class="code-block__pre"><code>{{ responseExample }}</code></pre>
            </el-tab-pane>
          </el-tabs>

          <div class="invoke-area">
            <div class="invoke-editor-head">
              <label for="invoke-params">请求参数（JSON 对象）</label>
              <el-button text @click="resetExample">恢复示例</el-button>
            </div>
            <el-input
              id="invoke-params"
              v-model="requestParams"
              type="textarea"
              :autosize="{ minRows: 6, maxRows: 16 }"
              aria-label="试调用请求参数"
              spellcheck="false"
            />
            <p class="invoke-hint">GET 参数会转为查询参数；POST 参数会作为 JSON 请求体发送。试调用使用当前账号的调用额度。</p>
            <el-button type="primary" :loading="invoking" @click="handleInvoke">试调用</el-button>
            <div v-if="invokeResult !== null" class="invoke-result">
              <div class="invoke-result__title">调用结果</div>
              <pre class="code-block__pre"><code>{{ invokeResultText }}</code></pre>
            </div>
          </div>
        </template>
        <el-empty v-else-if="!loading" description="接口不存在或已下线" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getOnlineInterfaceById, invokeInterface } from '@/api/interface'
import { parseParameterSchema, parameterExample, prepareExample, responseExamples } from '@/utils/apiExamples'
import MethodTag from '@/components/MethodTag.vue'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const invoking = ref(false)
const detail = ref(null)
const activeTab = ref('params')
const invokeResult = ref(null)
const requestParams = ref('{}')
const goBack = () => router.push('/market')
const isOnline = (item) => Number(item.status) === 1

const paramList = computed(() => parseParameterSchema(detail.value?.requestParams).map((p) => ({
  name: p.name || p.paramName || p.key || '-',
  type: p.type || p.dataType || 'string',
  required: !!(p.required ?? p.isRequired ?? false),
  description: p.description || p.desc || p.remark || '',
})))

const resetExample = () => {
  requestParams.value = JSON.stringify(parameterExample(detail.value?.requestParams), null, 2)
}

const example = computed(() => {
  try { return prepareExample(detail.value, requestParams.value) } catch { return null }
})
const shellQuote = (text) => "'" + text.replaceAll("'", "'\\''") + "'"
const curlExample = computed(() => {
  if (!example.value) return '请先填写合法的 JSON 参数。'
  const { method, path, body } = example.value
  const parts = [
    `curl -X ${method} ${shellQuote(window.location.origin + path)}`,
    '-H "accessKey: your-ak"',
    '-H "signatureVersion: 2"',
    '-H "nonce: <每次重新生成的16到64位随机串>"',
    '-H "timestamp: <当前UNIX秒时间戳>"',
    '-H "sign: <按v2协议计算的HMAC-SHA256签名>"',
  ]
  if (body) parts.push('-H "Content-Type: application/json; charset=UTF-8"', `--data-binary ${shellQuote(body)}`)
  return parts.join(' \\\n  ')
})
const javaExample = computed(() => {
  if (!example.value) return '请先填写合法的 JSON 参数。'
  const { method, path, body } = example.value
  return `import com.ten.apiclientsdk.client.ApiClient;

ApiClient client = new ApiClient(accessKey, secretKey);
client.setGatewayHost(${JSON.stringify(window.location.origin)});
String result = client.request(${JSON.stringify(method)}, ${JSON.stringify(path)}, ${JSON.stringify(body)});
System.out.println(result);`
})
const responseExample = computed(() => JSON.stringify({
  code: 0,
  message: 'success',
  data: responseExamples[detail.value?.url] || '示例接口返回文本',
}, null, 2))
const invokeResultText = computed(() => typeof invokeResult.value === 'string'
  ? invokeResult.value : JSON.stringify(invokeResult.value, null, 2))

const loadDetail = async () => {
  loading.value = true
  invokeResult.value = null
  try {
    const res = await getOnlineInterfaceById(route.params.id)
    detail.value = res.data || null
    resetExample()
  } catch {
    detail.value = null
  } finally {
    loading.value = false
  }
}

const handleInvoke = async () => {
  if (!detail.value || !isOnline(detail.value)) {
    ElMessage.warning('该接口已离线，无法调用')
    return
  }
  try { prepareExample(detail.value, requestParams.value) } catch (error) {
    ElMessage.warning(error.message || '请输入合法的 JSON 对象')
    return
  }
  invoking.value = true
  try {
    const res = await invokeInterface({ id: detail.value.id, userRequestParams: requestParams.value })
    invokeResult.value = res.data
    ElMessage.success('调用成功')
  } catch (error) {
    invokeResult.value = { message: error.message || '调用失败' }
  } finally {
    invoking.value = false
  }
}

watch(() => route.params.id, loadDetail, { immediate: true })
</script>

<style scoped>
.invoke-editor-head { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-bottom: 8px; }
.invoke-hint { color: #667085; font-size: 13px; margin: 10px 0 16px; }
.invoke-area :deep(textarea) { font-family: monospace; }
.page {
  padding: 24px 0 48px;
}

.detail-container {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  width: fit-content;
  cursor: pointer;
  color: #6b7280;
  font-size: 14px;
  transition: color 0.2s ease;
}

.back-btn:hover {
  color: #2563eb;
}

.detail-card {
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 28px;
  min-height: 200px;
}

.detail-head {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.detail-path {
  font-family: 'SF Mono', 'Fira Code', 'Consolas', monospace;
  font-size: 20px;
  font-weight: 600;
  color: #1f2937;
  word-break: break-all;
}

.detail-status {
  margin-left: 4px;
}

.detail-name {
  font-size: 18px;
  font-weight: 600;
  color: #1f2937;
  margin-top: 16px;
  margin-bottom: 6px;
}

.detail-desc {
  color: #6b7280;
  font-size: 14px;
  margin-bottom: 20px;
}

.detail-tabs {
  margin-top: 8px;
}

.code-block {
  margin-bottom: 20px;
}

.code-block__title {
  font-size: 13px;
  font-weight: 600;
  color: #6b7280;
  margin-bottom: 8px;
}

.code-block__pre {
  background: #f9fafb;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 16px;
  overflow-x: auto;
  font-family: 'SF Mono', 'Fira Code', 'Consolas', monospace;
  font-size: 13px;
  line-height: 1.6;
  color: #1f2937;
  white-space: pre;
}

.invoke-area {
  margin-top: 24px;
  border-top: 1px solid #e5e7eb;
  padding-top: 20px;
}

.invoke-result {
  margin-top: 16px;
}

.invoke-result__title {
  font-size: 13px;
  font-weight: 600;
  color: #6b7280;
  margin-bottom: 8px;
}
</style>
