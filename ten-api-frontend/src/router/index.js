import { createRouter, createWebHistory } from 'vue-router'
import DefaultLayout from '@/layouts/DefaultLayout.vue'
import DashboardLayout from '@/layouts/DashboardLayout.vue'
import AdminLayout from '@/layouts/AdminLayout.vue'
import Home from '@/views/Home.vue'
import ApiMarket from '@/views/ApiMarket.vue'
import ApiDetail from '@/views/ApiDetail.vue'
import Login from '@/views/Login.vue'
import Register from '@/views/Register.vue'
import Dashboard from '@/views/Dashboard.vue'
import InterfaceManage from '@/views/InterfaceManage.vue'
import UserManage from '@/views/UserManage.vue'

const routes = [
  {
    path: '/',
    component: DefaultLayout,
    children: [
      { path: '', name: 'Home', component: Home },
      { path: 'market', name: 'ApiMarket', component: ApiMarket },
      { path: 'market/:id', name: 'ApiDetail', component: ApiDetail },
    ],
  },
  // 登录、注册无需布局
  { path: '/login', name: 'Login', component: Login },
  { path: '/register', name: 'Register', component: Register },
  // 用户控制台
  {
    path: '/dashboard',
    component: DashboardLayout,
    meta: { requiresAuth: true },
    children: [
      { path: '', name: 'Dashboard', component: Dashboard },
    ],
  },
  // 管理后台（需要 admin 角色）
  {
    path: '/admin',
    component: AdminLayout,
    meta: { requiresAuth: true, requiresAdmin: true },
    redirect: '/admin/interfaces',
    children: [
      { path: 'interfaces', name: 'InterfaceManage', component: InterfaceManage },
      { path: 'users', name: 'UserManage', component: UserManage },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// 全局前置守卫
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  // 需要登录但未登录
  if (to.meta.requiresAuth && !token) {
    next({ path: '/login', query: { redirect: to.fullPath } })
    return
  }
  // 需要 admin 权限
  if (to.meta.requiresAdmin) {
    const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
    if (userInfo.userRole !== 'admin') {
      next({ path: '/' })
      return
    }
  }
  next()
})

export default router
