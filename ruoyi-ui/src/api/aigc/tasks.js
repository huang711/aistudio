import request from '@/utils/request'

// 查询AI任务列表
export function listTasks(query) {
  return request({
    url: '/aigc/tasks/list',
    method: 'get',
    params: query
  })
}

// 查询AI任务详细
export function getTasks(id) {
  return request({
    url: '/aigc/tasks/' + id,
    method: 'get'
  })
}

// 新增AI任务
export function addTasks(data) {
  return request({
    url: '/aigc/tasks',
    method: 'post',
    data: data
  })
}

// 修改AI任务
export function updateTasks(data) {
  return request({
    url: '/aigc/tasks',
    method: 'put',
    data: data
  })
}

// 删除AI任务
export function delTasks(id) {
  return request({
    url: '/aigc/tasks/' + id,
    method: 'delete'
  })
}
