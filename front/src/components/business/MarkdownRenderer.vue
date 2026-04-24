<template>
  <div ref="contentRef" v-viewer="viewerOptions" class="article-content prose prose-slate dark:prose-invert max-w-none">
    <!-- eslint-disable-next-line vue/no-v-html -->
    <!-- v-html: 内容已经过 DOMPurify 两次 sanitize 处理，安全 -->
    <div ref="renderedContentRef" v-html="processedContent"></div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted, onUnmounted, nextTick, defineExpose } from 'vue'
import { marked } from 'marked'
import hljs from 'highlight.js'
import katex from 'katex'
import 'katex/dist/katex.min.css'
import 'highlight.js/styles/tokyo-night-dark.css'
import { markedHighlight } from 'marked-highlight'
import DOMPurify from 'dompurify'
import logger from '@/utils/logger'

// Mermaid lazy load
let mermaid = null

const props = defineProps({
  content: {
    type: String,
    default: ''
  },
  viewerOptions: {
    type: Object,
    default: () => ({
      inline: false,
      button: true,
      navbar: true,
      title: true,
      toolbar: {
        zoomIn: true,
        zoomOut: true,
        oneToOne: true,
        reset: true,
        rotateLeft: true,
        rotateRight: true,
        flipHorizontal: true,
        flipVertical: true
      },
      tooltip: true,
      movable: true,
      zoomable: true,
      rotatable: true,
      scalable: true,
      transition: true,
      fullscreen: true,
      keyboard: true,
      url: 'src'
    })
  }
})

const emit = defineEmits(['rendered', 'mermaid-rendered'])

const contentRef = ref(null)
const renderedContentRef = ref(null)
const processedContent = ref('')

// Initialize mermaid
async function initMermaid() {
  if (!mermaid) {
    const module = await import('mermaid')
    mermaid = module.default
    mermaid.initialize({
      startOnLoad: false,
      theme: 'default',
      securityLevel: 'strict',
      fontFamily: 'Arial, sans-serif'
    })
  }
  return mermaid
}

// Configure marked with highlight.js
marked.use({
  renderer: {
    image({ href, title, text }) {
      const safeTitle = title ? ` title="${title}"` : ''
      return `<img src="${href}" alt="${text}" loading="lazy" decoding="async"${safeTitle} />`
    }
  }
})

marked.use(markedHighlight({
  langPrefix: 'hljs language-',
  highlight(code, lang) {
    const language = hljs.getLanguage(lang) ? lang : 'plaintext'
    return hljs.highlight(code, { language }).value
  }
}))

marked.setOptions({
  breaks: true,
  gfm: true
})

// Render math formulas (KaTeX)
function renderMath(content) {
  // Render inline formulas $...$
  content = content.replace(/\$([^$]+)\$/g, (match, tex) => {
    try {
      return katex.renderToString(tex.trim(), {
        displayMode: false,
        throwOnError: false
      })
    } catch (e) {
      return match
    }
  })
  // Render block formulas $$...$$
  content = content.replace(/\$\$([^$]+)\$\$/g, (match, tex) => {
    try {
      return `<div class="math-block">${katex.renderToString(tex.trim(), {
        displayMode: true,
        throwOnError: false
      })}</div>`
    } catch (e) {
      return match
    }
  })
  return content
}

// Pre-process mermaid blocks before markdown parsing
async function preprocessMermaid(content) {
  // Match mermaid code blocks: ```mermaid ... ```
  const mermaidRegex = /```mermaid\n([\s\S]*?)```/g

  const mm = await initMermaid()
  const mermaidBlocks = []

  // Find all mermaid blocks and collect them
  let match
  while ((match = mermaidRegex.exec(content)) !== null) {
    const fullMatch = match[0]
    const code = match[1].trim()
    const index = match.index

    mermaidBlocks.push({ fullMatch, code, index, id: `mermaid-${Date.now()}-${Math.random().toString(36).slice(2, 11)}` })
  }

  // Render each mermaid block
  for (const block of mermaidBlocks) {
    try {
      const { svg } = await mm.render(block.id, block.code)
      // Replace the mermaid block with a div containing the SVG
      const svgWrapper = `<div class="mermaid-block" data-mermaid-id="${block.id}">${svg}</div>`
      content = content.replace(block.fullMatch, svgWrapper)
    } catch (e) {
      logger.error('Mermaid render error:', e)
      // Keep original block on error
    }
  }

  return content
}

// Process content: sanitize, render math, parse markdown, sanitize
async function processContent() {
  if (!props.content) {
    processedContent.value = ''
    return
  }

  try {
    // 1. Sanitize original content
    let sanitized = DOMPurify.sanitize(props.content)

    // 2. Render math formulas (synchronous)
    sanitized = renderMath(sanitized)

    // 3. Pre-process mermaid blocks (async)
    sanitized = await preprocessMermaid(sanitized)

    // 4. Parse markdown to HTML
    const html = marked.parse(sanitized)

    // 5. Final sanitize
    processedContent.value = DOMPurify.sanitize(html)

    emit('mermaid-rendered')
  } catch (e) {
    logger.error('Markdown render error:', e)
    processedContent.value = DOMPurify.sanitize(props.content)
  }
}

// Post-render processing
let renderTimer = null

async function postRender() {
  await nextTick()
  await nextTick()
  renderTimer = setTimeout(() => {
    emit('rendered')
  }, 100)
}

// Watch for content changes
watch(() => props.content, async () => {
  await processContent()
  await postRender()
}, { immediate: true })

onMounted(() => {
  postRender()
})

onUnmounted(() => {
  if (renderTimer) {
    clearTimeout(renderTimer)
  }
})

defineExpose({
  contentRef,
  renderedContentRef
})
</script>

<style scoped>
:deep(.article-content p) {
  letter-spacing: 0.3px;
  margin: 0 0 1.25rem 0;
  line-height: 1.75;
  color: var(--text-secondary);
  font-weight: 400;
  word-break: normal;
  word-wrap: break-word;
  font-family: -apple-system, BlinkMacSystemFont, PingFang SC, Hiragino Sans GB, Microsoft Yahei, Arial, sans-serif;
}

:deep(.article-content h2),
:deep(.article-content h3),
:deep(.article-content h4) {
  margin: 2rem 0 1rem 0;
  color: var(--text-primary);
  line-height: 1.5;
  font-family: PingFang SC, Helvetica Neue, Helvetica, Hiragino Sans GB, Microsoft YaheI, "\5FAE\8F6F\96C5\9ED1", Arial, sans-serif;
}

:deep(.article-content h2) {
  font-size: 1.5rem;
  font-weight: 600;
  border-bottom: 1px solid var(--border-color);
  padding-bottom: 0.5rem;
}

:deep(.article-content h3) {
  font-size: 1.25rem;
  font-weight: 600;
}

:deep(.article-content h4) {
  font-size: 1.125rem;
  font-weight: 600;
}

:deep(pre) {
  background: var(--code-bg, #1E293B);
  color: var(--code-text, #F8FAFC);
  border-radius: 0.5rem;
  padding: 1rem;
  font-size: 0.875rem;
  overflow-x: auto;
  margin: 1.5rem 0;
  position: relative;
}

:deep(.code-copy-btn) {
  position: absolute;
  top: 0.5rem;
  right: 0.5rem;
  background: rgba(255, 255, 255, 0.1);
  color: var(--code-text, #F8FAFC);
  border: none;
  border-radius: 0.25rem;
  padding: 0.25rem 0.5rem;
  font-size: 0.75rem;
  cursor: pointer;
  transition: background-color $1s ease, border-color $1s ease, color $1s ease, box-shadow $1s ease$2
}

:deep(.code-copy-btn:hover) {
  background: rgba(255, 255, 255, 0.2);
}

:deep(.code-copy-btn.copied) {
  background: var(--color-success);
  color: var(--bg-card);
}

:deep(pre code) {
  background: transparent;
  color: inherit;
  font-family: 'Fira Code', 'Courier New', monospace;
}

/* Code language label */
:deep(.code-lang-label) {
  position: absolute;
  top: 0;
  left: 0;
  background: var(--primary-color, #409EFF);
  color: white;
  padding: 0.25rem 0.75rem;
  font-size: 0.75rem;
  font-weight: 500;
  border-radius: 0.5rem 0 0.5rem 0;
  text-transform: capitalize;
}

/* Mermaid container */
:deep(.mermaid-block) {
  background: var(--bg-card);
  border-radius: 0.5rem;
  padding: 1rem;
  margin: 1.5rem 0;
  overflow-x: auto;
}

:deep(.mermaid-block svg) {
  max-width: 100%;
  height: auto;
}

/* Math block */
:deep(.math-block) {
  background: var(--background-secondary, #f5f5f5);
  border-radius: 0.5rem;
  padding: 1rem;
  margin: 1.5rem 0;
  overflow-x: auto;
  text-align: center;
}

:deep(.katex) {
  font-size: 1.1em;
}

:deep(code:not(pre code)) {
  padding: 0.125rem 0.25rem;
  margin: 0 0.125rem;
  font-size: 0.875rem;
  border-radius: 0.25rem;
  color: var(--primary-color);
  background-color: var(--primary-subtle, #EFF6FF);
  font-family: 'Fira Code', 'Courier New', monospace;
}

:deep(blockquote) {
  border-left: 0.25rem solid var(--primary-color);
  quotes: none;
  background: var(--primary-subtle, #EFF6FF);
  color: var(--text-secondary);
  font-size: 1rem;
  margin: 1.5rem 0;
  padding: 1rem 1.5rem;
  position: relative;
}

:deep(blockquote p:last-child) {
  margin-bottom: 0;
}

:deep(table) {
  border-collapse: collapse;
  margin: 1rem 0;
  width: 100%;
}

:deep(table th) {
  padding: 0.5rem 0.75rem;
  border: 1px solid var(--border-color);
  background-color: var(--background-secondary);
  font-weight: 600;
  text-align: left;
}

:deep(table td) {
  padding: 0.5rem 0.75rem;
  border: 1px solid var(--border-color);
}

:deep(table tr:nth-child(2n)) {
  background-color: var(--bg-secondary);
}

:deep(.article-content a) {
  color: var(--primary-color);
  text-decoration: none;
}

:deep(.article-content a:hover) {
  text-decoration: underline;
}

:deep(.article-content img) {
  position: relative;
  max-width: 100%;
  overflow: hidden;
  display: block;
  margin: 1.5rem auto;
  border-radius: 0.375rem;
  box-shadow: 0 1px 3px 0 rgba(0, 0, 0, 0.1), 0 1px 2px 0 rgba(0, 0, 0, 0.06);
}

:deep(strong) {
  color: var(--text-primary);
  font-weight: 600;
}

/* Responsive adjustments */
@media (max-width: 768px) {
  :deep(.article-content p) {
    line-height: 1.6;
  }

  :deep(.article-content h2) {
    font-size: 1.375rem;
  }

  :deep(.article-content h3) {
    font-size: 1.125rem;
  }
}
</style>
