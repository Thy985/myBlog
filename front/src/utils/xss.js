import DOMPurify from 'dompurify'

const ALLOWED_URI_REGEX = /^(?:(?:https?|mailto|tel|data:image\/(png|jpe?g|gif|svg\+xml|webp);base64,[a-zA-Z0-9+/=]+|#|\/|\?|%[0-9a-fA-F]{2}))/i

DOMPurify.addHook('afterSanitizeAttributes', (node) => {
  if (node.hasAttribute('id')) {
    node.setAttribute('id', node.getAttribute('id'))
  }
  if (node.hasAttribute('class')) {
    const classList = node.getAttribute('class').split(' ')
    const allowedClasses = ['hljs', 'language-', 'lang-']
    const filteredClasses = classList.filter(cls =>
      allowedClasses.some(prefix => cls.startsWith(prefix)) ||
      cls.startsWith('hljs-')
    )
    if (filteredClasses.length > 0) {
      node.setAttribute('class', filteredClasses.join(' '))
    } else {
      node.removeAttribute('class')
    }
  }
  if (node.hasAttribute('src')) {
    const src = node.getAttribute('src')
    if (!ALLOWED_URI_REGEX.test(src)) {
      node.removeAttribute('src')
    }
  }
  if (node.hasAttribute('href')) {
    const href = node.getAttribute('href')
    if (!ALLOWED_URI_REGEX.test(href)) {
      node.removeAttribute('href')
    }
  }
})

export function sanitizeHtml(html) {
  if (!html || typeof html !== 'string') {
    return ''
  }
  return DOMPurify.sanitize(html, {
    ADD_ATTR: ['id', 'class', 'target'],
    ALLOWED_TAGS: ['h1', 'h2', 'h3', 'h4', 'h5', 'h6', 'p', 'br', 'hr', 'ul', 'ol', 'li',
      'blockquote', 'pre', 'code', 'a', 'img', 'table', 'thead', 'tbody', 'tr', 'th', 'td',
      'div', 'span', 'strong', 'em', 'del', 'ins', 'sup', 'sub', 'details', 'summary'],
    ALLOW_DATA_ATTR: false,
    FORCE_BODY: false
  })
}

/**
 * 创建Vue自定义指令，用于安全地渲染HTML
 */
export function createSanitizeDirective() {
  return {
    mounted(el, binding) {
      el.innerHTML = sanitizeHtml(binding.value)
    },
    updated(el, binding) {
      if (binding.value !== binding.oldValue) {
        el.innerHTML = sanitizeHtml(binding.value)
      }
    }
  }
}

/**
 * 用于v-html指令的过滤器
 * @param {string} value - 原始HTML字符串
 * @returns {string} - 过滤后的安全HTML字符串
 */
export function htmlFilter(value) {
  return sanitizeHtml(value)
}
