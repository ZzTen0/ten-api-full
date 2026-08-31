<template>
  <div class="manage-page">
    <div class="manage-header">
      <h1 class="page-title">接口管理</h1>
      <el-button type="primary" :icon="Plus" @click="openAddDialog">新增接口</el-button>
    </div>

    <div class="manage-card">
      <div class="toolbar">
        <el-input
          v-model="keyword"
          placeholder="按接口名称搜索"
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
        <el-table-column prop="name" label="接口名称" min-width="160" show-overflow-tooltip />
        <el-table-column label="接口路径" min-width="280">
          <template #default="{ row }">
            <div class="path-cell">
              <MethodTag :method="row.method" />
              <span class="path-text">{{ row.url }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="isOnline(row) ? 'success' : 'info'" size="small">
              {{ isOnline(row) ? '在线' : '离线' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="调用次数" width="110" align="center">
          <template #default="{ row }">
            {{ row.totalNum ?? row.invokeCount ?? '-' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="!isOnline(row)"
              type="success"
              link
              size="small"
              @click="handleOnline(row)"
            >上线</el-button>
            <el-button
              v-else
              type="warning"
              link
              size="small"
              @click="handleOffline(row)"
            >下线</el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无接口数据" :image-size="80" />
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

    <!-- 新增接口对话框 -->
    <el-dialog v-model="dialogVisible" title="新增接口" width="600px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="接口名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入接口名称" />
        </el-form-item>
        <el-form-item label="接口描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="2"
            placeholder="请输入接口描述"
          />
        </el-form-item>
        <el-form-item label="请求方法" prop="method">
          <el-select v-model="form.method" placeholder="请选择请求方法" style="width: 100%">
            <el-option label="GET" value="GET" />
            <el-option label="POST" value="POST" />
            <el-option label="PUT" value="PUT" />
            <el-option label="DELETE" value="DELETE" />
          </el-select>
        </el-form-item>
        <el-form-item label="接口路径" prop="url">
          <el-input v-model="form.url" placeholder="如 /api/name/get" />
        </el-form-item>
        <el-form-item label="请求参数" prop="requestParams">
          <el-input
            v-model="form.requestParams"
            type="textarea"
            :rows="3"
            placeholder='JSON 格式，如 [{"name":"name","type":"string","required":true,"description":"名字"}]'
          />
        </el-form-item>
        <el-form-item label="请求头" prop="requestHeader">
          <el-input
            v-model="form.requestHeader"
            type="textarea"
            :rows="2"
            placeholder='JSON 格式，如 {"Content-Type":"application/json"}'
          />
        </el-form-item>
        <el-form-item label="响应示例" prop="responseExample">
          <el-input
            v-model="form.responseExample"
            type="textarea"
            :rows="3"
            placeholder="JSON 格式的响应示例"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import {
  listInterface,
  onlineInterface,
  offlineInterface,
  deleteInterface,
  addInterface,
} from '@/api/interface'
import MethodTag from '@/components/MethodTag.vue'

const loading = ref(false)
const submitting = ref(false)
const list = ref([])
const keyword = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const dialogVisible = ref(false)
const formRef = ref(null)

const form = reactive({
  name: '',
  description: '',
  method: 'GET',
  url: '',
  requestParams: '',
  requestHeader: '',
  responseExample: '',
})

const rules = {
  name: [{ required: true, message: '请输入接口名称', trigger: 'blur' }],
  method: [{ required: true, message: '请选择请求方法', trigger: 'change' }],
  url: [{ required: true, message: '请输入接口路径', trigger: 'blur' }],
}

const isOnline = (row) => Number(row.status) === 1

const filteredList = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  if (!kw) return list.value
  return list.value.filter((item) => (item.name || '').toLowerCase().includes(kw))
})

const pagedData = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredList.value.slice(start, start + pageSize.value)
})

const loadData = async () => {
  loading.value = true
  try {
    const res = await listInterface()
    list.value = Array.isArray(res.data) ? res.data : []
  } catch (e) {
    list.value = []
  } finally {
    loading.value = false
  }
}

const refresh = () => {
  currentPage.value = 1
  loadData()
}

const handleOnline = async (row) => {
  try {
    await onlineInterface(row.id)
    ElMessage.success('接口已上线')
    refresh()
  } catch (e) {
    // 错误已由请求拦截器统一提示
  }
}

const handleOffline = async (row) => {
  try {
    await offlineInterface(row.id)
    ElMessage.success('接口已下线')
    refresh()
  } catch (e) {
    // 错误已由请求拦截器统一提示
  }
}

const handleDelete = (row) => {
  ElMessageBox.confirm(`确定要删除接口「${row.name}」吗？`, '删除确认', {
    type: 'warning',
    confirmButtonText: '确定删除',
    cancelButtonText: '取消',
  })
    .then(async () => {
      try {
        await deleteInterface(row.id)
        ElMessage.success('删除成功')
        refresh()
      } catch (e) {
        // 错误已由请求拦截器统一提示
      }
    })
    .catch(() => {})
}

const openAddDialog = () => {
  Object.assign(form, {
    name: '',
    description: '',
    method: 'GET',
    url: '',
    requestParams: '',
    requestHeader: '',
    responseExample: '',
  })
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch (e) {
    return
  }
  submitting.value = true
  try {
    const payload = {
      name: form.name,
      description: form.description,
      method: form.method,
      url: form.url,
      requestParams: form.requestParams,
      requestHeader: form.requestHeader,
      responseHeader: form.responseExample,
      status: 0,
    }
    await addInterface(payload)
    ElMessage.success('新增成功')
    dialogVisible.value = false
    refresh()
  } catch (e) {
    // 错误已由请求拦截器统一提示
  } finally {
    submitting.value = false
  }
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

.path-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.path-text {
  font-family: 'SF Mono', 'Fira Code', 'Consolas', monospace;
  font-size: 13px;
  color: #1f2937;
  word-break: break-all;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
