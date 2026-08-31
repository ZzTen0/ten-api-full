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
            <el-button type="primary" :loading="invoking" @click="handleInvoke">试调用</el-button>
            <div v-if="invokeResult !== null" class="invoke-result">
              <div class="invoke-result__title">调用结果</div>
              <pre class="code-block__pre"><code>{{ invokeResultText }}</code></pre>
            </div>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getInterfaceById, invokeInterface } from '@/api/interface'
import MethodTag from '@/components/MethodTag.vue'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const invoking = ref(false)
const detail = ref(null)
const activeTab = ref('params')
const invokeResult = ref(null)

// 接口获取失败时使用的兜底数据
const fallbackDetail = {
  id: 1,
  name: '获取名字',
  description: '根据参数返回名字',
  method: 'GET',
  url: '/api/name/get',
  status: 1,
  requestParams: JSON.stringify([
    { name: 'name', type: 'string', required: true, description: '需要查询的名字' },
  ]),
}

const goBack = () => router.push('/market')

const isOnline = (item) => Number(item.status) === 1

const paramList = computed(() => {
  const raw = detail.value?.requestParams
  if (!raw) return []
  try {
    const parsed = JSON.parse(raw)
    if (!Array.isArray(parsed)) return []
    return parsed.map((p) => ({
      name: p.name || p.paramName || p.key || '-',
      type: p.type || p.dataType || 'string',
      required: !!(p.required ?? p.isRequired ?? false),
      description: p.description || p.desc || p.remark || '',
    }))
  } catch (e) {
    return []
  }
})

const curlExample = computed(() => {
  const m = (detail.value?.method || 'GET').toUpperCase()
  const u = detail.value?.url || '/api/...'
  return `curl -X ${m} http://localhost:8090${u} \\
  -H "accessKey: your-ak" \\
  -H "sign: computed-signature" \\
  -H "nonce: random-string" \\
  -H "timestamp: 1690000000"`
})

const javaExample = computed(() => {
  const u = detail.value?.url || '/api/name/get'
  const m = (detail.value?.method || 'GET').toUpperCase()
  return `import com.ten.apiclientsdk.client.ApiClient;

ApiClient client = new ApiClient(accessKey, secretKey);
// 调用 ${u} (${m})
String result = client.request("${m}", "${u}", params);
System.out.println(result);`
})

const responseExample = computed(() => {
  return `{
  "code": 200,
  "data": "Hello, World!",
  "message": "success"
}`
})

const invokeResultText = computed(() => {
  if (invokeResult.value === null) return ''
  try {
    return JSON.stringify(invokeResult.value, null, 2)
  } catch (e) {
    return String(invokeResult.value)
  }
})

const loadDetail = async () => {
  loading.value = true
  try {
    const id = route.params.id
    const res = await getInterfaceById(id)
    detail.value = res.data || fallbackDetail
  } catch (e) {
    detail.value = fallbackDetail
  } finally {
    loading.value = false
  }
}

const handleInvoke = async () => {
  if (!detail.value) return
  if (!isOnline(detail.value)) {
    ElMessage.warning('该接口已离线，无法调用')
    return
  }
  invoking.value = true
  try {
    const res = await invokeInterface({
      id: Number(detail.value.id),
      userRequestParams: '{}',
    })
    invokeResult.value = res.data
    ElMessage.success('调用成功')
  } catch (e) {
    invokeResult.value = null
  } finally {
    invoking.value = false
  }
}

onMounted(loadDetail)
</script>

<style scoped>
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
