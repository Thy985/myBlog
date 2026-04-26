import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getBlogSettingDetail } from '@/api/admin/blogsetting'
import logger from '@/utils/logger'
import { API_STATUS } from '@/composables/api'

const CACHE_DURATION = 5 * 60 * 1000

export interface BlogSetting {
  [key: string]: any
}

export const useSettingsStore = defineStore('settings', () => {
  const setting = ref<BlogSetting>({})
  let lastFetchTime = 0

  function hasValidCache(): boolean {
    if (Object.keys(setting.value).length === 0) {
      const cachedSetting = localStorage.getItem('blogSetting')
      if (cachedSetting) {
        const cacheTime = localStorage.getItem('blogSettingTime')
        if (cacheTime && (Date.now() - parseInt(cacheTime)) < CACHE_DURATION) {
          setting.value = JSON.parse(cachedSetting)
          lastFetchTime = parseInt(cacheTime)
          return true
        }
      }
      return false
    }
    return (Date.now() - lastFetchTime) < CACHE_DURATION
  }

  async function getBlogSetting(forceRefresh = false): Promise<BlogSetting> {
    if (!forceRefresh && hasValidCache()) {
      return setting.value
    }

    try {
      const res = await getBlogSettingDetail()
      if (res && res.code === API_STATUS.SUCCESS && res.data) {
        setting.value = res.data
        lastFetchTime = Date.now()
        localStorage.setItem('blogSetting', JSON.stringify(res.data))
        localStorage.setItem('blogSettingTime', lastFetchTime.toString())
        return res.data
      } else {
        logger.error('获取博客设置信息失败：响应格式错误')
        throw new Error('获取博客设置信息失败：响应格式错误')
      }
    } catch (err: any) {
      logger.error('获取博客设置信息失败:', err.message)
      throw err
    }
  }

  function clearSettingCache(): void {
    setting.value = {}
    localStorage.removeItem('blogSetting')
    localStorage.removeItem('blogSettingTime')
    lastFetchTime = 0
  }

  return {
    setting,
    getBlogSetting,
    hasValidCache,
    clearSettingCache
  }
})