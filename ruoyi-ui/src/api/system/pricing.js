import request from '@/utils/request'

// 查询模型配置列表
export function listPricing(query) {
  return request({
    url: '/system/pricing/list',
    method: 'get',
    params: query
  })
}

// 查询模型配置详细
export function getPricing(id) {
  return request({
    url: '/system/pricing/' + id,
    method: 'get'
  })
}

// 新增模型配置
export function addPricing(data) {
  return request({
    url: '/system/pricing',
    method: 'post',
    data: data
  })
}

// 修改模型配置
export function updatePricing(data) {
  return request({
    url: '/system/pricing',
    method: 'put',
    data: data
  })
}

// 删除模型配置
export function delPricing(id) {
  return request({
    url: '/system/pricing/' + id,
    method: 'delete'
  })
}

/**
 * 测试模型连接
 * 增加了 timeout 属性，防止 AI 生成（特别是图片）耗时过长导致前端中断
 */
export function testConnection(data) {
  return request({
    url: '/system/pricing/testConnection',
    method: 'post',
    data: data,
    timeout: 600000 // 设置为 60 秒，给文生图留出足够的生成时间
  })
}