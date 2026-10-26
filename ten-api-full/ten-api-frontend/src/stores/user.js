import { defineStore } from 'pinia'
import { userLogin, getCurrentUser, userLogout } from '@/api/user'

const readStoredUser = () => {
  localStorage.removeItem('token')
  try {
    return JSON.parse(localStorage.getItem('userInfo') || 'null')
  } catch (e) {
    localStorage.removeItem('userInfo')
    return null
  }
}

export const useUserStore = defineStore('user', {
  state: () => ({
    userInfo: readStoredUser(),
  }),

  getters: {
    isLoggedIn: (state) => !!state.userInfo?.id,
    userRole: (state) => state.userInfo?.userRole || '',
  },

  actions: {
    setUserInfo(userInfo) {
      this.userInfo = userInfo || null
      if (this.userInfo) {
        localStorage.setItem('userInfo', JSON.stringify(this.userInfo))
      } else {
        localStorage.removeItem('userInfo')
      }
    },

    // 登录
    async login(loginData) {
      const res = await userLogin(loginData)
      this.setUserInfo(res.data)
      return res
    },

    // 获取当前用户信息
    async fetchUserInfo() {
      const res = await getCurrentUser()
      this.setUserInfo(res.data)
      return res
    },

    // 退出登录
    async logout() {
      try {
        await userLogout()
      } catch (e) {
        // 退出接口失败也清除本地状态
      } finally {
        this.setUserInfo(null)
      }
    },
  },
})
