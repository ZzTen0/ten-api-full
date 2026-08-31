<template>
  <div class="default-layout">
    <header class="header">
      <div class="container header-inner">
        <router-link to="/" class="logo">API 接口开放平台</router-link>
        <nav class="nav">
          <router-link to="/">首页</router-link>
          <router-link to="/market">接口市场</router-link>
          <template v-if="!isLoggedIn">
            <router-link to="/login">登录</router-link>
            <router-link to="/register">注册</router-link>
          </template>
          <template v-else>
            <router-link to="/dashboard">控制台</router-link>
            <router-link v-if="isAdmin" to="/admin">管理后台</router-link>
            <a href="javascript:void(0)" @click="handleLogout">退出</a>
          </template>
        </nav>
      </div>
    </header>
    <main class="main">
      <router-view />
    </main>
    <footer class="footer">
      <div class="container">
        <p>© 2026 API 接口开放平台</p>
      </div>
    </footer>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const router = useRouter()

const isLoggedIn = computed(() => userStore.isLoggedIn)
const isAdmin = computed(() => userStore.userRole === 'admin')

const handleLogout = async () => {
  await userStore.logout()
  ElMessage.success('已退出登录')
  router.push('/')
}
</script>

<style scoped>
.default-layout {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

.header {
  position: sticky;
  top: 0;
  z-index: 100;
  background: #ffffff;
  border-bottom: 1px solid #f0f0f0;
}

.header-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 60px;
}

.logo {
  font-size: 18px;
  font-weight: 700;
  color: #1f2937;
}

.nav {
  display: flex;
  align-items: center;
  gap: 24px;
}

.nav a {
  color: #4b5563;
  font-size: 14px;
}

.nav a:hover,
.nav a.router-link-exact-active {
  color: #2563eb;
}

.main {
  flex: 1;
}

.footer {
  border-top: 1px solid #f0f0f0;
  padding: 24px 0;
  text-align: center;
  color: #9ca3af;
  font-size: 13px;
}
</style>
