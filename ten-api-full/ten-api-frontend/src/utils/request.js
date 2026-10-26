import axios from 'axios'
import { ElMessage } from 'element-plus'

const service = axios.create({
  baseURL: '/api',
  timeout: 10000,
  withCredentials: true,
})

// 响应拦截器：统一处理业务码与 HTTP 错误
service.interceptors.response.use(
  (response) => {
    const res = response.data
    // 后端统一响应中 code=0 表示成功
    if (res.code !== 0) {
      ElMessage.error(res.message || '请求错误')
      if (res.code === 40100) {
        handleUnauthorized()
      }
      return Promise.reject(new Error(res.message || '请求错误'))
    }
    return res
  },
  (error) => {
    let message = '请求错误'
    const status = error.response?.status
    if (status === 401) {
      message = '登录已过期，请重新登录'
      handleUnauthorized()
    } else if (error.response?.data?.message) {
      message = error.response.data.message
    } else if (status) {
      message = `请求失败 (${status})`
    } else if (error.message?.includes('timeout')) {
      message = '请求超时，请稍后重试'
    }
    ElMessage.error(message)
    return Promise.reject(error)
  }
)

// 未登录统一处理：清除本地会话缓存并跳转登录页
async function handleUnauthorized() {
  localStorage.removeItem('userInfo')
  try {
    const { useUserStore } = await import('@/stores/user')
    const userStore = useUserStore()
    userStore.userInfo = null
  } catch (e) {
    // store 尚未初始化时忽略
  }
  const { default: router } = await import('@/router')
  if (router.currentRoute.value.path !== '/login') {
    router.push({
      path: '/login',
      query: { redirect: router.currentRoute.value.fullPath },
    })
  }
}

export default service
