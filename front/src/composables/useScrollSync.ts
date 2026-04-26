import { ref, onMounted, onUnmounted } from 'vue'

interface TocItem {
  id: string
  title: string
  level?: number
}

interface UseScrollSyncOptions {
  offsetTop?: number
  onActiveIndexChange?: (index: number) => void
}

export function useScrollSync(tocItems: { value: TocItem[] }, options: UseScrollSyncOptions = {}) {
  const { offsetTop = 100, onActiveIndexChange } = options

  const activeTocIndex = ref(0)
  let scrollHandler: (() => void) | null = null

  function scrollToSection(id: string, index: number): void {
    const element = document.getElementById(id)
    if (element) {
      element.scrollIntoView({ behavior: 'smooth' })
      activeTocIndex.value = index
      if (onActiveIndexChange) {
        onActiveIndexChange(index)
      }
    }
  }

  function handleScroll(): void {
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

  function startScrollListener(): void {
    if (scrollHandler) {
      window.removeEventListener('scroll', scrollHandler)
    }
    scrollHandler = handleScroll
    window.addEventListener('scroll', scrollHandler, { passive: true })
  }

  function stopScrollListener(): void {
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