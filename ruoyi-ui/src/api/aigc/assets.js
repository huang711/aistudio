import request from '@/utils/request'

// 查询资产列表
export function listAssets(query) {
  return request({
    url: '/aigc/assets/list',
    method: 'get',
    params: query
  })
}

// 上传并新增资产
export function uploadAsset(data) {
  return request({
    url: '/aigc/assets/upload',
    method: 'post',
    data: data,
    headers: {
      'repeatSubmit': false, // 必须禁用，防止若依拦截器读取流
      'Content-Type': undefined // 必须设为 undefined，让浏览器自动补全 boundary 分隔符
    }
  })
}

// 删除资产
export function delAsset(id) {
  return request({
    url: '/aigc/assets/' + id,
    method: 'delete'
  })
}
// 查询存储统计
export function getStorageStat() {
  return request({
    url: '/aigc/assets/storage/stat',
    method: 'get'
  })
}