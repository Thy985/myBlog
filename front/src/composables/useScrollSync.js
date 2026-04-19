/**
 * 滚动同步 Composable
 * 封装目录滚动和页面滚动同步逻辑
 */
import { ref, onMounted, onUnmounted } from 'vue'

export function useScrollSync(tocItems, options = {}) {
  const { offsetTop = 100, onActiveIndexChange } = options

  const activeTocIndex = ref(0)
  let scrollHandler = null

  /**
   * 滚动到指定章节
   */
  function scrollToSection(id, index) {
    const element = document.getElementById(id)
    if (element) {
      element.scrollIntoView({ behavior: 'smooth' })
      activeTocIndex.value = index
      if (onActiveIndexChange) {
        onActiveIndexChange(index)
      }
    }
  }

  /**
   * 处理页面滚动，更新活跃目录项
   */
  function handleScroll() {
    const scrollPosition = window.scrollY + offsetTop

    for (let i = tocItems.value.length - 1; i >= 0; i--) {
      const element = document.getElementById(tocItems.value[i].id)
      if (element && element.offsetTop <= scrollPosition) {
        if (activeTocIndex.value !== i) {
          activeTocIndex.value = i
          if (onActiveIndexChange) {
            onActiveIndexChange(i)
          }
        }
        break
      }
    }
  }

  /**
   * 启动滚动监听
   */
  function startScrollListener() {
    if (scrollHandler) {
      window.removeEventListener('scroll', scrollHandler)
    }
    scrollHandler = handleScroll
    window.addEventListener('scroll', scrollHandler, { passive: true })
  }

  /**
   * 停止滚动监听
   */
  function stopScrollListener() {
    if (scrollHandler) {
      window.removeEventListener('scroll', scrollHandler)
      scrollHandler = null
    }
  }

  onMounted(() => {
    startScrollListener()
  })

  onUnmounted(() => {
    stopScrollListener()
  })

  return {
    activeTocIndex,
    scrollToSection,
    startScrollListener,
    stopScrollListener
  }
}
