<template>
  <div class="auth-page">
    <!-- 左侧品牌区 -->
    <div class="auth-left">
      <div class="brand">
        <h1 class="brand-title">TenAPI</h1>
        <p class="brand-subtitle">API 接口开放平台</p>
        <p class="brand-desc">
          基于 Spring Boot + Dubbo 微服务架构，提供接口管理、签名鉴权、调用统计、网关路由等完整能力，面向开发者的统一接口调用入口。
        </p>
        <div class="brand-tags">
          <span class="tag">Spring Cloud Gateway</span>
          <span class="tag">Dubbo RPC</span>
          <span class="tag">AK/SK 签名</span>
        </div>
      </div>
    </div>

    <!-- 右侧表单区 -->
    <div class="auth-right">
      <div class="auth-card">
        <h2 class="card-title">注册</h2>
        <p class="card-desc">创建您的账号，开始使用接口服务</p>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          size="large"
          @submit.prevent="handleRegister"
        >
          <el-form-item prop="userAccount">
            <el-input
              v-model="form.userAccount"
              placeholder="请输入用户名"
              :prefix-icon="User"
              clearable
            />
          </el-form-item>
          <el-form-item prop="userPassword">
            <el-input
              v-model="form.userPassword"
              type="password"
              placeholder="请输入密码"
              :prefix-icon="Lock"
              show-password
            />
          </el-form-item>
          <el-form-item prop="checkPassword">
            <el-input
              v-model="form.checkPassword"
              type="password"
              placeholder="请再次输入密码"
              :prefix-icon="Lock"
              show-password
              @keyup.enter="handleRegister"
            />
          </el-form-item>
          <el-form-item>
            <el-button
              class="submit-btn"
              :loading="loading"
              @click="handleRegister"
            >
              注册
            </el-button>
          </el-form-item>
        </el-form>

        <p class="auth-footer">
          已有账号？
          <router-link to="/login">去登录</router-link>
        </p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { userRegister } from '@/api/user'

const router = useRouter()

const formRef = ref()
const loading = ref(false)

const form = reactive({
  userAccount: '',
  userPassword: '',
  checkPassword: '',
})

const validateCheckPassword = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请再次输入密码'))
  } else if (value !== form.userPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules = {
  userAccount: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 4, message: '用户名不少于 4 位', trigger: 'blur' },
  ],
  userPassword: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 8, message: '密码不少于 8 位', trigger: 'blur' },
  ],
  checkPassword: [
    { required: true, validator: validateCheckPassword, trigger: 'blur' },
  ],
}

const handleRegister = async () => {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch (e) {
    return
  }
  loading.value = true
  try {
    await userRegister({
      userAccount: form.userAccount,
      userPassword: form.userPassword,
      checkPassword: form.checkPassword,
    })
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } catch (e) {
    // 错误信息已由请求拦截器统一提示
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-page {
  display: flex;
  min-height: 100vh;
  background: #ffffff;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC',
    'Helvetica Neue', Arial, sans-serif;
}

/* ===== 左侧品牌区 ===== */
.auth-left {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px;
  background: linear-gradient(180deg, #ffffff 0%, #f9fafb 100%);
  border-right: 1px solid #e5e7eb;
}

.brand {
  max-width: 420px;
}

.brand-title {
  font-size: 48px;
  font-weight: 800;
  letter-spacing: -1px;
  background: linear-gradient(135deg, #2563eb 0%, #60a5fa 100%);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  color: transparent;
}

.brand-subtitle {
  margin-top: 12px;
  font-size: 22px;
  font-weight: 600;
  color: #1f2937;
}

.brand-desc {
  margin-top: 20px;
  font-size: 15px;
  line-height: 1.8;
  color: #6b7280;
}

.brand-tags {
  margin-top: 28px;
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.tag {
  padding: 6px 14px;
  font-size: 13px;
  color: #1f2937;
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 999px;
}

/* ===== 右侧表单区 ===== */
.auth-right {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px 24px;
}

.auth-card {
  width: 100%;
  max-width: 420px;
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 40px 36px;
}

.card-title {
  font-size: 26px;
  font-weight: 700;
  color: #1f2937;
}

.card-desc {
  margin-top: 8px;
  font-size: 14px;
  color: #6b7280;
}

.auth-card :deep(.el-input__wrapper) {
  border-radius: 8px;
  box-shadow: 0 0 0 1px #e5e7eb inset;
}

.auth-card :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #c7d2fe inset;
}

.auth-card :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #2563eb inset;
}

.submit-btn {
  width: 100%;
  height: 46px;
  background: #111827;
  border-color: #111827;
  color: #ffffff;
  font-size: 15px;
  font-weight: 600;
  border-radius: 8px;
}

.submit-btn:hover,
.submit-btn:focus {
  background: #000000;
  border-color: #000000;
  color: #ffffff;
}

.auth-footer {
  margin-top: 20px;
  text-align: center;
  font-size: 14px;
  color: #6b7280;
}

.auth-footer a {
  color: #2563eb;
  font-weight: 500;
}

/* ===== 响应式 ===== */
@media (max-width: 900px) {
  .auth-left {
    display: none;
  }
}

@media (max-width: 480px) {
  .auth-card {
    padding: 32px 24px;
  }
}
</style>
