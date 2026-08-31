import request from '@/utils/request'

// 获取接口列表
export function listInterface() {
  return request({
    url: '/interfaceInfo/list',
    method: 'get',
  })
}

// 根据 id 获取接口详情
export function getInterfaceById(id) {
  return request({
    url: '/interfaceInfo/get',
    method: 'get',
    params: { id },
  })
}

// 新增接口（管理员）
export function addInterface(data) {
  return request({
    url: '/interfaceInfo/add',
    method: 'post',
    data,
  })
}

// 删除接口（管理员）
export function deleteInterface(id) {
  return request({
    url: '/interfaceInfo/delete',
    method: 'post',
    params: { id },
  })
}

// 上线接口（管理员）
export function onlineInterface(id) {
  return request({
    url: '/interfaceInfo/online',
    method: 'post',
    params: { id },
  })
}

// 下线接口（管理员）
export function offlineInterface(id) {
  return request({
    url: '/interfaceInfo/offline',
    method: 'post',
    params: { id },
  })
}

// 测试调用接口
export function invokeInterface(data) {
  return request({
    url: '/interfaceInfo/invoke',
    method: 'post',
    data,
  })
}
