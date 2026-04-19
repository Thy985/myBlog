import { streamAgentChat } from '@/api/frontend/agent'
import { useAgentSessionStore } from '@/stores/agentSession'
import { useAgentExecutionStore } from '@/stores/agentExecution'
import { useAgentUIStore } from '@/stores/agentUI'
import { AgentEventType } from '@/types/agent'
import { ElMessage } from 'element-plus'
import logger from '@/utils/logger'

/**
 * Agent Service - 业务逻辑层
 *
 * 职责：
 * 1. 协调 Session/Execution/UI 三个 Store
 * 2. 封装复杂的业务逻辑
 * 3. 提供简洁的 API 给组件层
 */
class AgentService {
  constructor() {
    this.sessionStore = null
    this.executionStore = null
    this.uiStore = null
    this.streamController = null
  }

  // 初始化（在应用启动时调用）
  init() {
    this.sessionStore = useAgentSessionStore()
    this.executionStore = useAgentExecutionStore()
    this.uiStore = useAgentUIStore()

    // 从存储恢复会话
    this.sessionStore.loadFromStorage()
  }

  // ============ 会话管理 ============

  async ensureSession() {
    if (!this.sessionStore.sessionId) {
      const success = await this.sessionStore.initSession()
      if (!success) {
        throw new Error('会话初始化失败')
      }
    }
    return this.sessionStore.sessionId
  }

  clearSession() {
    this.cancelStream()
    this.sessionStore.clearSession()
    this.executionStore.clearHistory()
    this.uiStore.clearExpandedPanels()
    this.uiStore.clearSelection()
  }

  // ============ 消息发送 ============

  async sendMessage(content) {
    const trimmedContent = content?.trim()
    if (!trimmedContent) {return}

    // 确保会话
    await this.ensureSession()

    // 创建用户消息
    const userMessage = {
      id: generateId(),
      role: 'user',
      content: trimmedContent
    }
    this.sessionStore.addMessage(userMessage)

    // 创建 AI 消息占位
    const aiMessageId = generateId()
    const aiMessage = {
      id: aiMessageId,
      role: 'assistant',
      content: '',
      isStreaming: true
    }
    this.sessionStore.addMessage(aiMessage)

    // 开始执行
    this.executionStore.startExecution(trimmedContent, aiMessageId)

    // 清空输入
    this.uiStore.clearInput()

    // 发送请求
    try {
      this.streamController = streamAgentChat(
        {
          message: trimmedContent,
          sessionId: this.sessionStore.sessionId,
          messageId: aiMessageId
        },
        {
          onEvent: (event) => this.handleEvent(event, aiMessageId),
          onError: (error) => this.handleError(error, aiMessageId),
          onComplete: () => this.handleComplete(aiMessageId),
          onReconnect: (info) => this.handleReconnect(info)
        }
      )
    } catch (err) {
      this.handleError(
        { payload: { message: err.message, recoverable: false } },
        aiMessageId
      )
    }
  }

  // ============ 事件处理 ============

  handleEvent(event, messageId) {
    // 更新执行状态
    this.executionStore.handleEvent(event)

    // 根据事件类型更新消息
    switch (event.type) {
      case AgentEventType.MESSAGE:
        this.appendMessageContent(messageId, event.payload.content)
        break
      case AgentEventType.ERROR:
        this.handleError(event, messageId)
        break
      case AgentEventType.DONE:
        this.handleComplete(messageId)
        break
    }
  }

  appendMessageContent(messageId, content) {
    const message = this.sessionStore.getMessageById(messageId)
    if (message) {
      this.sessionStore.updateMessage(messageId, {
        content: message.content + content
      })
    }
  }

  handleError(error, messageId) {
    logger.error('[AgentService] Error:', error)

    // 更新消息状态
    this.sessionStore.updateMessage(messageId, {
      isError: true,
      isStreaming: false,
      content: error.payload?.message || '发生错误'
    })

    // 结束执行
    this.executionStore.failExecution(error.payload?.message)

    // 显示提示
    if (!error.payload?.recoverable) {
      ElMessage.error(error.payload?.message || '请求失败')
    }

    this.streamController = null
  }

  handleComplete(messageId) {
    // 更新消息状态
    this.sessionStore.updateMessage(messageId, {
      isStreaming: false
    })

    // 结束执行
    this.executionStore.endExecution('completed')

    // 增加未读计数
    this.uiStore.incrementUnread()

    this.streamController = null
  }

  handleReconnect(info) {
    logger.info('[AgentService] Reconnected:', info)
    // 通知用户连接已恢复
    ElMessage.success('连接已恢复')
  }

  // ============ 流控制 ============

  cancelStream() {
    if (this.streamController) {
      this.streamController.abort()
      this.streamController = null
    }

    // 找到正在流式的消息并标记为取消
    const lastMessage = this.sessionStore.lastMessage
    if (lastMessage?.isStreaming) {
      this.sessionStore.updateMessage(lastMessage.id, {
        isStreaming: false,
        content: lastMessage.content + '\n[已取消]'
      })
    }

    this.executionStore.cancelExecution()
  }

  // ============ 快捷指令 ============

  getQuickCommands() {
    return [
      { label: '发博客', icon: '📝', command: '发一篇关于{topic}的博客', topic: 'React' },
      { label: '技术问答', icon: '❓', command: '请解释一下{topic}的原理', topic: 'TypeScript泛型' },
      { label: '代码debug', icon: '🐛', command: '帮我看看这段代码有什么问题：\n```\nconst arr = [1, 2, 3];\nconsole.log(arr.map(i => i * 2));\n```' },
      { label: '内容摘要', icon: '📋', command: '帮我总结一下这篇文章的主要内容：\n' }
    ]
  }

  applyQuickCommand(command) {
    let text = command.command
    if (command.topic) {
      text = text.replace('{topic}', command.topic)
    }
    this.uiStore.setInputText(text)
    return text
  }

  // ============ 重试 ============

  async retryMessage(messageId) {
    const messageIndex = this.sessionStore.messages.findIndex(m => m.id === messageId)
    if (messageIndex <= 0) {return}

    // 找到上一条用户消息
    let userMessageIndex = messageIndex - 1
    while (userMessageIndex >= 0 && this.sessionStore.messages[userMessageIndex].role !== 'user') {
      userMessageIndex--
    }

    if (userMessageIndex < 0) {return}

    const userMessage = this.sessionStore.messages[userMessageIndex]

    // 删除错误消息及其后的所有消息
    this.sessionStore.messages.splice(userMessageIndex + 1)

    // 重新发送
    await this.sendMessage(userMessage.content)
  }

  // ============ 复制消息 ============

  async copyMessage(content) {
    try {
      await navigator.clipboard.writeText(content)
      ElMessage.success('已复制到剪贴板')
    } catch (err) {
      logger.error('[AgentService] Copy failed:', err)
      ElMessage.error('复制失败')
    }
  }
}

function generateId() {
  return `${Date.now()}-${Math.random().toString(36).slice(2, 11)}`
}

// 单例导出
export const agentService = new AgentService()
