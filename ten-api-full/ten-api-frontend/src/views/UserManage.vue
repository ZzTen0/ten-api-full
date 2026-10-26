<template>
  <div class="manage-page">
    <div class="manage-header">
      <h1 class="page-title">用户管理</h1>
    </div>

    <div class="manage-card">
      <div class="toolbar">
        <el-input
          v-model="keyword"
          placeholder="按用户名搜索"
          clearable
          :prefix-icon="Search"
          class="search-input"
        />
      </div>

      <el-table
        v-loading="loading"
        :data="pagedData"
        border
        stripe
        row-key="id"
        style="width: 100%"
      >
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="userName" label="用户名" min-width="140" show-overflow-tooltip />
        <el-table-column prop="userAccount" label="账号" min-width="140" show-overflow-tooltip />
        <el-table-column label="角色" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.userRole === 'admin' ? 'danger' : 'info'" size="small">
              {{ row.userRole === 'admin' ? 'admin' : 'user' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" min-width="180">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无用户数据" :image-size="80" />
        </template>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50]"
          :total="filteredList.length"
          layout="total, sizes, prev, pager, next, jumper"
          background
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { listUsers, deleteUser } from '@/api/admin'

const loading = ref(false)
const list = ref([])
const keyword = ref('')
const currentPage = ref(1)
const pageSize = ref(10)

const filteredList = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  if (!kw) return list.value
  return list.value.filter((u) => (u.userName || '').toLowerCase().includes(kw))
})

const pagedData = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredList.value.slice(start, start + pageSize.value)
})

const formatTime = (t) => {
  if (!t) return '-'
  const d = new Date(t)
  if (isNaN(d.getTime())) return '-'
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await listUsers()
    list.value = Array.isArray(res.data) ? res.data : []
  } catch (e) {
    list.value = []
  } finally {
    loading.value = false
  }
}

const handleDelete = (row) => {
  ElMessageBox.confirm(
    `确定要删除用户「${row.userName || row.userAccount || row.id}」吗？`,
    '删除确认',
    {
      type: 'warning',
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
    }
  )
    .then(async () => {
      try {
        await deleteUser(row.id)
        ElMessage.success('删除成功')
        currentPage.value = 1
        loadData()
      } catch (e) {
        // 错误已由请求拦截器统一提示
      }
    })
    .catch(() => {})
}

onMounted(loadData)
</script>

<style scoped>
.manage-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.manage-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.page-title {
  font-size: 24px;
  font-weight: 700;
  color: #1f2937;
}

.manage-card {
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 20px;
}

.toolbar {
  margin-bottom: 16px;
}

.search-input {
  max-width: 320px;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
