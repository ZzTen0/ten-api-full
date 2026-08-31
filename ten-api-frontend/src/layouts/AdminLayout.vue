<template>
  <div class="admin-layout">
    <header class="header">
      <div class="container header-inner">
        <router-link to="/" class="logo">管理后台</router-link>
        <nav class="nav">
          <router-link to="/">返回首页</router-link>
          <router-link to="/dashboard">控制台</router-link>
          <a href="javascript:void(0)" @click="handleLogout">退出</a>
        </nav>
      </div>
    </header>
    <div class="admin-body">
      <aside class="sidebar">
        <router-link to="/admin/interfaces" class="sidebar-item">接口管理</router-link>
        <router-link to="/admin/users" class="sidebar-item">用户管理</router-link>
      </aside>
      <main class="admin-main">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const router = useRouter()

const handleLogout = async () => {
  await userStore.logout()
  ElMessage.success('已退出登录')
  router.push('/')
}
</script>

<style scoped>
.admin-layout {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

.header {
  position: sticky;
  top: 0;
  z-index: 100;
  background: #1f2937;
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
  color: #ffffff;
}

.nav {
  display: flex;
  align-items: center;
  gap: 24px;
}

.nav a {
  color: #d1d5db;
  font-size: 14px;
}

.nav a:hover {
  color: #ffffff;
}

.admin-body {
  flex: 1;
  display: flex;
}

.sidebar {
  width: 220px;
  background: #ffffff;
  border-right: 1px solid #f0f0f0;
  padding: 16px 0;
}

.sidebar-item {
  display: block;
  padding: 10px 24px;
  color: #4b5563;
  font-size: 14px;
}

.sidebar-item:hover,
.sidebar-item.router-link-exact-active {
  background: #eff6ff;
  color: #2563eb;
  border-left: 3px solid #2563eb;
}

.admin-main {
  flex: 1;
  padding: 24px;
  background: #f9fafb;
}
</style>
