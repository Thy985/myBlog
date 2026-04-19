/**
 * 主题工具 - 已废弃，请使用 ui.js store 的 dark mode 方法
 * 此文件保留用于向后兼容，但颜色变量设置功能已禁用
 * @deprecated 使用 useUIStore 的 toggleDarkMode/setDarkMode 代替
 */

// 应用主题 - 现在只切换 .dark 类，不设置 inline 样式
export function applyTheme(themeName) {
  const root = document.documentElement

  // 移除所有主题类
  root.classList.remove('dark', 'light', 'blue')

  // 添加当前主题类
  if (themeName === 'dark') {
    root.classList.add('dark')
  }
  // 'light' 和 'blue' 是默认样式，不需要添加类

  // 保存到localStorage
  localStorage.setItem('theme', themeName)
}

// 获取当前主题
export function getCurrentTheme() {
  return localStorage.getItem('theme') || 'light'
}

// 初始化主题
export function initTheme() {
  const savedTheme = getCurrentTheme()
  applyTheme(savedTheme)
}

// 同步 Element Plus 暗色主题
export function watchDarkModeForElementPlus() {
  const isDark = document.documentElement.classList.contains('dark')
  if (isDark) {
    document.documentElement.classList.add('dark-element-plus')
  }

  const observer = new MutationObserver(() => {
    const dark = document.documentElement.classList.contains('dark')
    if (dark) {
      document.documentElement.classList.add('dark-element-plus')
    } else {
      document.documentElement.classList.remove('dark-element-plus')
    }
  })

  observer.observe(document.documentElement, { attributes: true, attributeFilter: ['class'] })
}

export default {
  applyTheme,
  getCurrentTheme,
  initTheme
}
