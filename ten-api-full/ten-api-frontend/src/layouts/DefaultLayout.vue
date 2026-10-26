<template>
  <div class="default-layout">
    <header class="site-header">
      <div class="container header-inner">
        <router-link to="/" class="brand">
          TEN API <small>接口开放平台</small>
        </router-link>
        <nav class="main-nav" aria-label="主导航">
          <router-link to="/">首页</router-link>
          <router-link to="/market" :class="{ 'section-active': isMarketRoute }">
            接口市场
          </router-link>
          <router-link to="/docs/overview" :class="{ 'section-active': isDocsRoute }">
            开发文档
          </router-link>
        </nav>
        <nav class="account-nav" aria-label="账户导航">
          <template v-if="!isLoggedIn">
            <router-link to="/login">登录</router-link>
            <router-link class="register-link" to="/register">注册</router-link>
          </template>
          <template v-else>
            <router-link class="user-link" to="/dashboard">控制台</router-link>
            <router-link v-if="isAdmin" class="user-link" to="/admin">管理后台</router-link>
            <button type="button" @click="handleLogout">退出</button>
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
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const route = useRoute()
const router = useRouter()

const isLoggedIn = computed(() => userStore.isLoggedIn)
const isAdmin = computed(() => userStore.userRole === 'admin')
const isMarketRoute = computed(() => ['ApiMarket', 'ApiDetail'].includes(route.name))
const isDocsRoute = computed(() => route.name === 'Docs')

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

.site-header {
  position: sticky;
  top: 0;
  z-index: 100;
  height: 64px;
  background: rgba(255, 255, 255, 0.96);
  border-bottom: 1px solid #e2e6ea;
}

.header-inner {
  display: flex;
  align-items: center;
  height: 100%;
  gap: 42px;
}

.brand {
  flex: 0 0 auto;
  color: #111827;
  font-size: 17px;
  font-weight: 750;
}

.brand small {
  margin-left: 8px;
  color: #8a94a3;
  font-size: 12px;
  font-weight: 500;
}

.main-nav {
  display: flex;
  align-items: stretch;
  gap: 26px;
  height: 100%;
}

.main-nav a {
  position: relative;
  display: flex;
  align-items: center;
  color: #596273;
  font-size: 14px;
  white-space: nowrap;
}

.main-nav a:hover,
.main-nav a.router-link-exact-active,
.main-nav a.section-active {
  color: #111827;
}

.main-nav a.router-link-exact-active::after,
.main-nav a.section-active::after {
  position: absolute;
  right: 0;
  bottom: -1px;
  left: 0;
  height: 2px;
  background: #111827;
  content: '';
}

.account-nav {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 20px;
  color: #596273;
  font-size: 14px;
  white-space: nowrap;
}

.account-nav a,
.account-nav button {
  color: inherit;
  font: inherit;
}

.account-nav button {
  padding: 0;
  border: 0;
  background: transparent;
  cursor: pointer;
}

.account-nav a:hover,
.account-nav button:hover {
  color: #111827;
}

.main {
  flex: 1;
}

.footer {
  border-top: 1px solid #e2e6ea;
  padding: 22px 0;
  text-align: center;
  color: #89919e;
  font-size: 12px;
}

@media (max-width: 820px) {
  .header-inner {
    gap: 20px;
  }

  .brand small {
    display: none;
  }

  .main-nav {
    gap: 18px;
  }

  .account-nav {
    gap: 12px;
  }
}

@media (max-width: 600px) {
  .header-inner {
    padding: 0 14px;
    gap: 14px;
  }

  .brand {
    font-size: 14px;
  }

  .main-nav {
    margin-left: auto;
    gap: 12px;
  }

  .main-nav a,
  .account-nav {
    font-size: 12px;
  }

  .register-link,
  .user-link {
    display: none;
  }
}
</style>
