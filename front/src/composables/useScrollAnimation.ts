import { ref, onMounted, onUnmounted } from 'vue'

export function useScrollAnimation(options: {
  threshold?: number
  rootMargin?: string
  once?: boolean
} = {}) {
  const {
    threshold = 0.1,
    rootMargin = '0px 0px -50px 0px',
    once = true
  } = options

  const elementRef = ref<HTMLElement | null>(null)
  const isVisible = ref(false)
  let observer: IntersectionObserver | null = null

  onMounted(() => {
    if (!elementRef.value) {return}

    observer = new IntersectionObserver(
      (entries) => {
        entries.forEach(entry => {
          if (entry.isIntersecting) {
            isVisible.value = true
            if (once && observer) {
              observer.disconnect()
            }
          } else if (!once) {
            isVisible.value = false
          }
        })
      },
      { threshold, rootMargin }
    )

    observer.observe(elementRef.value)
  })

  onUnmounted(() => {
    if (observer) {
      observer.disconnect()
    }
  })

  return {
    elementRef,
    isVisible
  }
}

export function useStaggerAnimation(itemCount: number, baseDelay = 100) {
  const getDelay = (index: number): string => `${index * baseDelay}ms`

  return { getDelay }
}