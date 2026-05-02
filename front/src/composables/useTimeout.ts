import { ref, onUnmounted } from 'vue'

export function useTimeout() {
  const timers = ref<number[]>([])

  const setTimeout = (callback: () => void, delay: number) => {
    const id = window.setTimeout(callback, delay)
    timers.value.push(id)
    return id
  }

  const clearTimeout = (id: number) => {
    window.clearTimeout(id)
    timers.value = timers.value.filter(t => t !== id)
  }

  const clearAll = () => {
    timers.value.forEach(id => window.clearTimeout(id))
    timers.value = []
  }

  const withSuccessFeedback = (
    setter: (value: boolean) => void,
    trueDelay = 100,
    falseDelay = 3000
  ) => {
    setter(true)
    setTimeout(() => setter(false), falseDelay)
  }

  onUnmounted(() => {
    clearAll()
  })

  return {
    setTimeout,
    clearTimeout,
    clearAll,
    withSuccessFeedback
  }
}