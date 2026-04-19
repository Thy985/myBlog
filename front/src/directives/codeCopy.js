/**
 * v-code-copy 指令
 * 为代码块添加复制功能
 */
import logger from '@/utils/logger'

export const codeCopyDirective = {
  mounted(el, binding) {
    const button = document.createElement('button')
    button.className = 'code-copy-btn'
    button.textContent = '复制'
    button.setAttribute('aria-label', '复制代码')

    button.addEventListener('click', () => {
      const code = el.querySelector('code')?.textContent || ''
      navigator.clipboard.writeText(code).then(() => {
        button.textContent = '已复制'
        button.classList.add('copied')
        setTimeout(() => {
          button.textContent = '复制'
          button.classList.remove('copied')
        }, 2000)
      }).catch(err => {
        logger.error('复制失败:', err)
        button.textContent = '复制失败'
        setTimeout(() => {
          button.textContent = '复制'
        }, 2000)
      })
    })

    el.appendChild(button)
  }
}

export default codeCopyDirective
