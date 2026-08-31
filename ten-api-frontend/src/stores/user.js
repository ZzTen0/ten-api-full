import { defineStore } from 'pinia'
import { userLogin, getCurrentUser, userLogout } from '@/api/user'

export const useUserStore = defineStore('user', {
  state: () => ({
    userInfo: JSON.parse(localStorage.getItem('userInfo') || 'null'),
    token: localStorage.getItem('token') || '',
  }),

  getters: {
    isLoggedIn: (state) => !!state.token,
    userRole: (state) => state.userInfo?.userRole || '',
  },

  actions: {
    // 登录
    async login(loginData) {
      const res = await userLogin(loginData)
      const token = res.data
      this.token = token
      localStorage.setItem('token', token)
      return res
    },

    // 获取当前用户信息
    async fetchUserInfo() {
      const res = await getCurrentUser()
      this.userInfo = res.data
      localStorage.setItem('userInfo', JSON.stringify(res.data))
      return res
    },

    // 退出登录
    async logout() {
      try {
        await userLogout()
      } catch (e) {
        // 退出接口失败也清除本地状态
      } finally {
        this.token = ''
        this.userInfo = null
        localStorage.removeItem('token')
        localStorage.removeItem('userInfo')
      }
    },
  },
})
