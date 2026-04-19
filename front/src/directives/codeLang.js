/**
 * v-code-lang 指令
 * 为代码块添加语言标签
 */
export const codeLangDirective = {
  mounted(el, binding) {
    const code = el.querySelector('code')
    if (!code) {return}

    const classList = Array.from(code.classList)
    const langClass = classList.find(c => c.startsWith('language-'))

    if (langClass && !el.classList.contains('mermaid')) {
      const lang = langClass.replace('language-', '')
      if (lang && lang !== 'plaintext') {
        if (!el.querySelector('.code-lang-label')) {
          const label = document.createElement('span')
          label.className = 'code-lang-label'
          label.textContent = lang
          el.appendChild(label)
        }
      }
    }
  }
}

export default codeLangDirective
