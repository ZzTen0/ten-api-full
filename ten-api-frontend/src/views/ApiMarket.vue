<template>
  <div class="market-page">
    <div class="market-shell container">
      <header class="market-header">
        <div>
          <h1>接口市场</h1>
          <p>浏览平台当前已上线的接口服务，查看请求方式、接口地址和接入说明。</p>
        </div>
        <span class="availability">仅展示已上线</span>
      </header>

      <div class="market-tools" aria-label="接口筛选">
        <el-input
          v-model="keyword"
          :prefix-icon="Search"
          clearable
          placeholder="搜索接口名称、路径或说明"
          aria-label="搜索接口"
        />
        <el-select v-model="activeCategory" aria-label="接口分类">
          <el-option
            v-for="category in categories"
            :key="category.value"
            :label="category.label"
            :value="category.value"
          />
        </el-select>
        <span class="market-count">{{ filteredList.length }} 个接口</span>
      </div>

      <div v-loading="loading" class="market-list" aria-live="polite">
        <article v-for="item in filteredList" :key="item.id" class="market-item">
          <MethodTag :method="item.method" />
          <div class="market-item-main">
            <strong>{{ item.name }}</strong>
            <p>
              <span class="api-path">{{ item.url }}</span>
              <span class="description-separator">·</span>
              {{ item.description || '暂无描述' }}
            </p>
          </div>
          <span class="market-category">{{ categoryLabel(item) }}</span>
          <el-button
            link
            type="primary"
            :icon="View"
            @click="goDetail(item)"
          >
            查看详情
          </el-button>
        </article>
        <el-empty
          v-if="!loading && filteredList.length === 0"
          description="没有符合条件的接口"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Search, View } from '@element-plus/icons-vue'
import { listOnlineInterfaces } from '@/api/interface'
import MethodTag from '@/components/MethodTag.vue'

const router = useRouter()
const loading = ref(false)
const interfaceList = ref([])
const keyword = ref('')
const activeCategory = ref('all')

const categories = [
  { label: '全部分类', value: 'all', keywords: [] },
  { label: '示例接口', value: 'example', keywords: ['name', '名称', '示例'] },
  { label: 'AI 服务', value: 'ai', keywords: ['ai', '人工智能'] },
  { label: '工具类', value: 'tool', keywords: ['tool', '工具'] },
]

const searchableText = (item) => (
  `${item.name || ''} ${item.description || ''} ${item.url || ''}`.toLowerCase()
)

const matchesCategory = (item, categoryValue) => {
  if (categoryValue === 'all') return true
  const category = categories.find((entry) => entry.value === categoryValue)
  return category?.keywords.some((entry) => searchableText(item).includes(entry)) || false
}

const isOnline = (item) => Number(item.status) === 1

const filteredList = computed(() => {
  const search = keyword.value.trim().toLowerCase()
  return interfaceList.value.filter((item) => (
    matchesCategory(item, activeCategory.value)
    && (!search || searchableText(item).includes(search))
  ))
})

const categoryLabel = (item) => {
  const category = categories
    .filter((entry) => entry.value !== 'all')
    .find((entry) => entry.keywords.some((keywordItem) => (
      searchableText(item).includes(keywordItem)
    )))
  return category?.label || '其他'
}

const goDetail = (item) => {
  router.push({ name: 'ApiDetail', params: { id: item.id } })
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await listOnlineInterfaces()
    const list = Array.isArray(res.data) ? res.data : []
    interfaceList.value = list.filter(isOnline)
  } catch (e) {
    interfaceList.value = []
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.market-shell {
  min-height: calc(100vh - 64px);
  padding-top: 58px;
  padding-bottom: 96px;
}

.market-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 28px;
  margin-bottom: 30px;
}

.market-header h1 {
  color: #111827;
  font-size: 34px;
  line-height: 1.25;
}

.market-header p {
  max-width: 640px;
  margin-top: 7px;
  color: #667085;
}

.availability {
  color: #16734a;
  font-family: 'SFMono-Regular', Consolas, monospace;
  font-size: 12px;
  white-space: nowrap;
}

.market-tools {
  display: grid;
  grid-template-columns: minmax(240px, 1fr) 180px auto;
  align-items: center;
  gap: 12px;
  padding: 18px 0;
  border-top: 1px solid #cfd5dc;
  border-bottom: 1px solid #e2e6ea;
}

.market-tools :deep(.el-input__wrapper),
.market-tools :deep(.el-select__wrapper) {
  min-height: 40px;
  border-radius: 5px;
  box-shadow: 0 0 0 1px #cfd5dc inset;
}

.market-count {
  color: #667085;
  font-size: 13px;
  text-align: right;
  white-space: nowrap;
}

.market-list {
  min-height: 120px;
  border-bottom: 1px solid #e2e6ea;
}

.market-item {
  display: grid;
  grid-template-columns: 72px minmax(0, 1fr) 130px 110px;
  align-items: center;
  gap: 18px;
  min-height: 92px;
  padding: 18px 12px;
  border-bottom: 1px solid #e2e6ea;
}

.market-item:last-of-type {
  border-bottom: 0;
}

.market-item:hover {
  background: #fafbfc;
}

.market-item-main {
  min-width: 0;
}

.market-item-main strong {
  display: block;
  margin-bottom: 3px;
  color: #111827;
  font-size: 15px;
}

.market-item-main p {
  overflow: hidden;
  color: #667085;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.api-path {
  color: #252d3a;
  font-family: 'SFMono-Regular', Consolas, monospace;
}

.description-separator {
  margin: 0 4px;
  color: #a0a8b3;
}

.market-category {
  color: #596273;
  font-size: 13px;
}

@media (max-width: 820px) {
  .market-tools {
    grid-template-columns: minmax(200px, 1fr) 170px;
  }

  .market-count {
    grid-column: 1 / -1;
    text-align: left;
  }

  .market-item {
    grid-template-columns: 64px minmax(0, 1fr) 110px;
  }

  .market-category {
    display: none;
  }
}

@media (max-width: 560px) {
  .market-shell {
    padding-top: 38px;
    padding-bottom: 72px;
  }

  .market-header {
    align-items: flex-start;
    flex-direction: column;
    gap: 12px;
  }

  .market-header h1 {
    font-size: 29px;
  }

  .market-tools {
    grid-template-columns: 1fr;
  }

  .market-count {
    grid-column: auto;
  }

  .market-item {
    grid-template-columns: 54px minmax(0, 1fr);
    gap: 10px;
    padding: 18px 2px;
  }

  .market-item-main p {
    white-space: normal;
  }

  .market-item :deep(.el-button) {
    grid-column: 2;
    width: max-content;
    margin-left: 0;
  }
}
</style>
