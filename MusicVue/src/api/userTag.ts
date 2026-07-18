import request from './request'

export interface UserPersonalTag {
  id: number
  userId: number
  name: string
  color: string
  sortOrder: number
  createTime: string
  updateTime: string
}

// 创建标签
export function createPersonalTag(name: string, color?: string) {
  return request.post<UserPersonalTag>('/user/personal-tags', {
    name,
    color: color || '#409EFF'
  })
}

// 更新标签
export function updatePersonalTag(tagId: number, name: string, color: string) {
  return request.put<UserPersonalTag>(`/user/personal-tags/${tagId}`, {
    name,
    color
  })
}

// 删除标签
export function deletePersonalTag(tagId: number) {
  return request.delete<string>(`/user/personal-tags/${tagId}`)
}

// 获取用户的所有标签
export function getUserPersonalTags() {
  return request.get<UserPersonalTag[]>('/user/personal-tags')
}

// 更新标签排序
export function updateTagOrder(tagIds: number[]) {
  return request.put<string>('/user/personal-tags/order', { tagIds })
}

