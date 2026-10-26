<template>
  <div class="dashboard">
    <!-- 1. 顶部欢迎区 -->
    <div class="welcome">
      <h1 class="welcome-title">控制台</h1>
      <p class="welcome-sub">欢迎, {{ userName }}</p>
    </div>

    <!-- 2. 统计卡片 -->
    <div class="stat-grid">
      <div v-for="item in stats" :key="item.label" class="stat-card">
        <div class="stat-label">{{ item.label }}</div>
        <div class="stat-value">{{ item.value.toLocaleString() }}</div>
        <div v-if="item.trend !== null" class="stat-trend up">↑ {{ item.trend }}%</div>
      </div>
    </div>

    <!-- 3. 调用趋势图 -->
    <div class="panel">
      <div class="panel-title">近12天调用趋势</div>
      <div ref="chartRef" class="chart"></div>
    </div>

    <!-- 4. 访问凭证 -->
    <div class="panel">
      <div class="panel-title">访问凭证</div>
      <div v-if="hasKeys" class="key-list">
        <div class="key-row">
          <div class="key-label">AccessKey</div>
          <div class="key-value">{{ maskedAk }}</div>
          <el-button text class="key-btn" :icon="DocumentCopy" @click="copy(accessKey)">复制</el-button>
        </div>
      </div>
      <div v-else class="key-empty">
        <span>尚未生成访问凭证</span>
      </div>
    </div>

    <!-- 5. 最近调用记录 -->
    <div class="panel">
      <div class="panel-title">调用记录</div>
      <el-table v-if="callRecords.length" :data="callRecords" stripe class="record-table">
        <el-table-column prop="time" label="时间" width="180" />
        <el-table-column prop="api" label="接口" min-width="220" />
        <el-table-column label="方法" width="100">
          <template #default="{ row }">
            <el-tag :type="methodTagType(row.method)" size="small">{{ row.method }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'success' ? 'success' : 'danger'" size="small">
              {{ row.status === 'success' ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="耗时(ms)" width="110" align="right">
          <template #default="{ row }">
            <span class="duration">{{ row.duration }}</span>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-else description="暂无调用记录" />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import { DocumentCopy } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

// ===== 用户信息 =====
const userInfo = computed(() => userStore.userInfo || {})
const userName = computed(() => userInfo.value.userName || userInfo.value.userAccount || '用户')
const accessKey = computed(() => userInfo.value.accessKey || '')
const hasKeys = computed(() => !!accessKey.value)
const maskedAk = computed(() => {
  const ak = accessKey.value
  if (!ak) return ''
  return ak.length >= 8 ? ak.slice(0, 8) + '****' : ak + '****'
})
// ===== 统计数据 =====
const stats = [
  { label: '总调用次数', value: 26181, trend: 12.5 },
  { label: '今日调用', value: 1247, trend: 8.3 },
  { label: '剩余额度', value: 73819, trend: null },
  { label: '接口数量', value: 5, trend: null },
]

// ===== 调用记录（模拟） =====
const callRecords = [
  { time: '2026-08-06 14:32:10', api: 'GET /api/weather', method: 'GET', status: 'success', duration: 128 },
  { time: '2026-08-06 14:28:45', api: 'POST /api/translate', method: 'POST', status: 'success', duration: 256 },
  { time: '2026-08-06 14:20:03', api: 'GET /api/phone', method: 'GET', status: 'fail', duration: 89 },
  { time: '2026-08-06 14:15:22', api: 'PUT /api/user/update', method: 'PUT', status: 'success', duration: 312 },
  { time: '2026-08-06 14:02:17', api: 'POST /api/translate', method: 'POST', status: 'success', duration: 198 },
]

const methodTagType = (method) => {
  const map = { GET: 'success', POST: 'primary', PUT: 'warning' }
  return map[method] || 'info'
}

// ===== 复制访问凭证 =====
const copy = async (text) => {
  if (!text) {
    ElMessage.warning('无内容可复制')
    return
  }
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success('已复制到剪贴板')
  } catch {
    ElMessage.error('复制失败')
  }
}

// ===== ECharts 调用趋势图 =====
const chartRef = ref(null)
let chartInstance = null

const initChart = () => {
  if (!chartRef.value) return
  chartInstance = echarts.init(chartRef.value)
  // 生成近12天日期标签
  const today = new Date()
  const dateLabels = []
  for (let i = 11; i >= 0; i--) {
    const d = new Date(today)
    d.setDate(d.getDate() - i)
    dateLabels.push(`${d.getMonth() + 1}/${d.getDate()}`)
  }
  chartInstance.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 0, right: 0, top: 16, bottom: 0, containLabel: false },
    xAxis: {
      type: 'category',
      data: dateLabels,
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { color: '#9ca3af', fontSize: 12, margin: 12 },
    },
    yAxis: { show: false },
    series: [
      {
        type: 'bar',
        data: [320, 520, 360, 640, 440, 720, 560, 680, 480, 760, 400, 600],
        barWidth: 22,
        itemStyle: {
          borderRadius: [6, 6, 0, 0],
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#60a5fa' },
            { offset: 1, color: '#2563eb' },
          ]),
        },
      },
    ],
  })
}

const handleResize = () => {
  if (chartInstance) chartInstance.resize()
}

onMounted(async () => {
  // 拉取最新用户信息并同步到 store
  try {
    await userStore.fetchUserInfo()
  } catch (e) {
    // 未登录由请求拦截器统一处理
  }
  initChart()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  if (chartInstance) {
    chartInstance.dispose()
    chartInstance = null
  }
})
</script>

<style scoped>
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

/* 欢迎区 */
.welcome {
  margin-bottom: 4px;
}
.welcome-title {
  font-size: 26px;
  font-weight: 700;
  color: #1f2937;
  margin: 0;
}
.welcome-sub {
  color: #6b7280;
  font-size: 14px;
  margin-top: 6px;
}

/* 统计卡片网格 */
.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}
.stat-card {
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 20px;
}
.stat-label {
  color: #6b7280;
  font-size: 13px;
}
.stat-value {
  color: #1f2937;
  font-size: 26px;
  font-weight: 700;
  margin-top: 8px;
}
.stat-trend {
  margin-top: 8px;
  font-size: 13px;
}
.stat-trend.up {
  color: #16a34a;
}

/* 面板通用 */
.panel {
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 20px;
}
.panel-title {
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 16px;
}

/* 图表 */
.chart {
  width: 100%;
  height: 280px;
}

/* 密钥管理 */
.key-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.key-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  background: #f9fafb;
  border: 1px solid #f3f4f6;
  border-radius: 8px;
}
.key-label {
  width: 90px;
  color: #6b7280;
  font-size: 13px;
  flex-shrink: 0;
}
.key-value {
  flex: 1;
  color: #1f2937;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 13px;
  word-break: break-all;
}
.key-btn {
  flex-shrink: 0;
}
.key-empty {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 14px;
  background: #f9fafb;
  border: 1px solid #f3f4f6;
  border-radius: 8px;
  color: #6b7280;
}

/* 调用记录 */
.record-table {
  width: 100%;
}
.duration {
  color: #6b7280;
}

/* 响应式 */
@media (max-width: 992px) {
  .stat-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
@media (max-width: 576px) {
  .stat-grid {
    grid-template-columns: 1fr;
  }
}
</style>
