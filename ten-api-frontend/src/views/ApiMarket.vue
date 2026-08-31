<template>
  <div class="page">
    <div class="container">
      <h1 class="page-title">接口市场</h1>
      <p class="page-subtitle">浏览所有可用的 API 接口，点击查看详情与调用方式</p>

      <div class="filter-bar">
        <el-radio-group v-model="activeCategory">
          <el-radio-button
            v-for="c in categories"
            :key="c.value"
            :value="c.value"
          >{{ c.label }}</el-radio-button>
        </el-radio-group>
      </div>

      <div v-loading="loading" class="market-grid">
        <div v-for="item in filteredList" :key="item.id" class="api-card">
          <div class="api-card__main">
            <div class="api-card__head">
              <MethodTag :method="item.method" />
              <span class="api-card__path">{{ item.url }}</span>
            </div>
            <div class="api-card__name">{{ item.name }}</div>
            <div class="api-card__desc">{{ item.description || '暂无描述' }}</div>
            <div class="api-card__status">
              <span
                class="status-dot"
                :class="isOnline(item) ? 'status-dot--online' : 'status-dot--offline'"
              ></span>
              <span :class="isOnline(item) ? 'status-text--online' : 'status-text--offline'">
                {{ isOnline(item) ? '在线' : '离线' }}
              </span>
            </div>
          </div>
          <div class="api-card__action">
            <el-button type="primary" plain @click="goDetail(item)">查看详情</el-button>
          </div>
        </div>
        <el-empty v-if="!loading && filteredList.length === 0" description="暂无接口" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { listOnlineInterfaces } from '@/api/interface'
import MethodTag from '@/components/MethodTag.vue'

const router = useRouter()
const loading = ref(false)
const interfaceList = ref([])
const activeCategory = ref('all')

const categories = [
  { label: '全部', value: 'all' },
  { label: '示例接口', value: 'example' },
  { label: 'AI服务', value: 'ai' },
  { label: '工具类', value: 'tool' },
]

const categoryKeywords = {
  example: ['name', '示例'],
  ai: ['ai', '人工智能'],
  tool: ['tool', '工具'],
}

const isOnline = (item) => Number(item.status) === 1

const filteredList = computed(() => {
  if (activeCategory.value === 'all') return interfaceList.value
  const kws = categoryKeywords[activeCategory.value] || []
  return interfaceList.value.filter((item) => {
    const text = `${item.name || ''} ${item.description || ''} ${item.url || ''}`.toLowerCase()
    return kws.some((k) => text.includes(k.toLowerCase()))
  })
})

const goDetail = (item) => {
  router.push(`/market/${item.id}`)
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await listOnlineInterfaces()
    const list = res.data || []
    interfaceList.value = Array.isArray(list) ? list : []
  } catch (e) {
    interfaceList.value = []
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.page {
  padding: 32px 0 48px;
}

.page-title {
  font-size: 28px;
  font-weight: 700;
  color: #1f2937;
  margin-bottom: 8px;
}

.page-subtitle {
  color: #6b7280;
  font-size: 14px;
  margin-bottom: 24px;
}

.filter-bar {
  margin-bottom: 24px;
}

.market-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(420px, 1fr));
  gap: 16px;
  min-height: 120px;
}

.api-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 20px;
  transition: box-shadow 0.2s ease, border-color 0.2s ease;
}

.api-card:hover {
  border-color: #c7d2fe;
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.08);
}

.api-card__main {
  flex: 1;
  min-width: 0;
}

.api-card__head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.api-card__path {
  font-family: 'SF Mono', 'Fira Code', 'Consolas', monospace;
  font-size: 14px;
  color: #1f2937;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.api-card__name {
  font-size: 15px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 4px;
}

.api-card__desc {
  font-size: 13px;
  color: #6b7280;
  margin-bottom: 10px;
}

.api-card__status {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  display: inline-block;
}

.status-dot--online {
  background: #10b981;
}

.status-dot--offline {
  background: #9ca3af;
}

.status-text--online {
  color: #059669;
}

.status-text--offline {
  color: #6b7280;
}

.api-card__action {
  flex-shrink: 0;
}
</style>
