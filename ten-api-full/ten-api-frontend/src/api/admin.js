import request from '@/utils/request'

// 获取用户列表（管理员）
export function listUsers() {
  return request({
    url: '/user/list',
    method: 'get',
  })
}

// 删除用户（管理员）
export function deleteUser(id) {
  return request({
    url: '/user/delete',
    method: 'post',
    data: { id },
  })
}
