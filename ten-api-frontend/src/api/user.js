import request from '@/utils/request'

// 用户注册
export function userRegister(data) {
  return request({
    url: '/user/register',
    method: 'post',
    data,
  })
}

// 用户登录
export function userLogin(data) {
  return request({
    url: '/user/login',
    method: 'post',
    data,
  })
}

// 获取当前登录用户
export function getCurrentUser() {
  return request({
    url: '/user/current',
    method: 'get',
  })
}

// 用户退出登录
export function userLogout() {
  return request({
    url: '/user/logout',
    method: 'post',
  })
}
