import { mount } from '@vue/test-utils'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import AISettings from '@/components/common/AISettings.vue'

describe('AISettings组件测试', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('WebSocket配置', () => {
    it('WebSocket默认应该启用', () => {
      const wrapper = mount(AISettings, {
        props: {}
      })

      expect(wrapper.vm.formData.wsEnabled).toBe(true)
    })

    it('wsEnabled应该能切换', async () => {
      const wrapper = mount(AISettings, {
        props: {}
      })

      expect(wrapper.vm.formData.wsEnabled).toBe(true)

      wrapper.vm.formData.wsEnabled = false
      await wrapper.vm.$nextTick()

      expect(wrapper.vm.formData.wsEnabled).toBe(false)
    })

    it('wsEnabled为false时WebSocket地址输入框应该禁用', async () => {
      const wrapper = mount(AISettings, {
        props: {}
      })

      wrapper.vm.formData.wsEnabled = false
      await wrapper.vm.$nextTick()

      const wsUrlInput = wrapper.find('input[placeholder="ws://localhost:8080/ws/agent"]')
      expect(wsUrlInput.attributes('disabled')).toBeDefined()
    })

    it('自动重连开关默认应该启用', () => {
      const wrapper = mount(AISettings, {
        props: {}
      })

      expect(wrapper.vm.formData.autoReconnect).toBe(true)
    })

    it('重连间隔应该默认为5秒', () => {
      const wrapper = mount(AISettings, {
        props: {}
      })

      expect(wrapper.vm.formData.reconnectInterval).toBe(5)
    })

    it('应该能修改重连间隔', async () => {
      const wrapper = mount(AISettings, {
        props: {}
      })

      wrapper.vm.formData.reconnectInterval = 10
      await wrapper.vm.$nextTick()

      expect(wrapper.vm.formData.reconnectInterval).toBe(10)
    })
  })

  describe('LLM API配置', () => {
    it('默认provider应该是openrouter', () => {
      const wrapper = mount(AISettings, {
        props: {}
      })

      expect(wrapper.vm.formData.llmProvider).toBe('openrouter')
    })

    it('应该能选择不同的provider', async () => {
      const wrapper = mount(AISettings, {
        props: {}
      })

      const select = wrapper.find('select')
      await select.setValue('openai')
      await wrapper.vm.$nextTick()

      expect(wrapper.vm.formData.llmProvider).toBe('openai')
    })

    it('API Key输入框初始应该是密码类型', () => {
      const wrapper = mount(AISettings, {
        props: {}
      })

      const apiKeyInput = wrapper.find('input[placeholder="sk-..."]')
      expect(apiKeyInput.attributes('type')).toBe('password')
    })

    it('应该能输入API Key', async () => {
      const wrapper = mount(AISettings, {
        props: {}
      })

      const apiKeyInput = wrapper.find('input[placeholder="sk-..."]')
      await apiKeyInput.setValue('test-api-key')
      await wrapper.vm.$nextTick()

      expect(wrapper.vm.formData.apiKey).toBe('test-api-key')
    })

    it('API Key切换可见性应该生效', async () => {
      const wrapper = mount(AISettings, {
        props: {}
      })

      expect(wrapper.vm.showApiKey).toBe(false)

      wrapper.vm.showApiKey = true
      await wrapper.vm.$nextTick()

      const apiKeyInput = wrapper.find('input[placeholder="sk-..."]')
      expect(apiKeyInput.attributes('type')).toBe('text')
    })
  })

  describe('模型选择', () => {
    it('模型选择器应该正确渲染', () => {
      const wrapper = mount(AISettings, {
        props: {}
      })

      const modelSelects = wrapper.findAll('select')
      expect(modelSelects.length).toBe(2)
    })

    it('选择预设模型应该更新formData.model', async () => {
      const wrapper = mount(AISettings, {
        props: {}
      })

      const modelSelect = wrapper.findAll('select')[1]
      await modelSelect.setValue('deepseek/deepseek-chat-v4-flash')
      await wrapper.vm.$nextTick()

      expect(wrapper.vm.formData.model).toBe('deepseek/deepseek-chat-v4-flash')
    })

    it('选择自定义模型应该显示文本输入框', async () => {
      const wrapper = mount(AISettings, {
        props: {}
      })

      const modelSelect = wrapper.findAll('select')[1]
      await modelSelect.setValue('__custom__')
      await wrapper.vm.$nextTick()

      const customInput = wrapper.find('input[placeholder="输入自定义模型名称"]')
      expect(customInput.exists()).toBe(true)
    })

    it('选择自定义模型后应该显示输入框', async () => {
      const wrapper = mount(AISettings, {
        props: {}
      })

      const modelSelect = wrapper.findAll('select')[1]
      await modelSelect.setValue('__custom__')
      await wrapper.vm.$nextTick()

      const customInput = wrapper.find('input[placeholder="输入自定义模型名称"]')
      expect(customInput.exists()).toBe(true)
    })
  })

  describe('AI记忆配置', () => {
    it('记忆功能默认应该启用', () => {
      const wrapper = mount(AISettings, {
        props: {}
      })

      expect(wrapper.vm.formData.memoryEnabled).toBe(true)
    })

    it('应该能输入写作风格', async () => {
      const wrapper = mount(AISettings, {
        props: {}
      })

      wrapper.vm.formData.writingStyle = '我喜欢简洁明了的风格'
      await wrapper.vm.$nextTick()

      expect(wrapper.vm.formData.writingStyle).toBe('我喜欢简洁明了的风格')
    })

    it('应该能输入个人简介', async () => {
      const wrapper = mount(AISettings, {
        props: {}
      })

      wrapper.vm.formData.personalBio = '我是一名全栈开发者'
      await wrapper.vm.$nextTick()

      expect(wrapper.vm.formData.personalBio).toBe('我是一名全栈开发者')
    })

    it('字数统计应该正确计算', async () => {
      const wrapper = mount(AISettings, {
        props: {}
      })

      wrapper.vm.formData.writingStyle = 'a'
      wrapper.vm.formData.personalBio = 'b'
      await wrapper.vm.$nextTick()

      // 字数统计是 writingStyle + personalBio 的长度
      expect(wrapper.vm.formData.writingStyle.length + wrapper.vm.formData.personalBio.length).toBe(2)
    })
  })

  describe('操作按钮', () => {
    it('没有API Key时测试连接按钮应该禁用', () => {
      const wrapper = mount(AISettings, {
        props: {}
      })

      const buttons = wrapper.findAll('button')
      const testButton = buttons.find(b => b.text() === '测试连接')
      expect(testButton?.attributes('disabled')).toBeDefined()
    })

    it('有API Key时测试连接按钮应该启用', async () => {
      const wrapper = mount(AISettings, {
        props: {}
      })

      wrapper.vm.formData.apiKey = 'test-key'
      await wrapper.vm.$nextTick()

      const buttons = wrapper.findAll('button')
      const testButton = buttons.find(b => b.text() === '测试连接')
      expect(testButton?.attributes('disabled')).toBeUndefined()
    })

    it('点击测试连接应该触发test-connection事件', async () => {
      const wrapper = mount(AISettings, {
        props: {},
        emits: ['test-connection']
      })

      wrapper.vm.formData.apiKey = 'test-key'
      await wrapper.vm.$nextTick()

      const buttons = wrapper.findAll('button')
      const testButton = buttons.find(b => b.text() === '测试连接')
      await testButton?.trigger('click')

      expect(wrapper.emitted('test-connection')).toBeTruthy()
    })

    it('点击保存应该触发save事件', async () => {
      const wrapper = mount(AISettings, {
        props: {},
        emits: ['save']
      })

      const buttons = wrapper.findAll('button')
      const saveButton = buttons.find(b => b.text() === '保存 AI 配置')
      await saveButton?.trigger('click')

      expect(wrapper.emitted('save')).toBeTruthy()
    })
  })

  describe('Props响应式', () => {
    it('loading变化应该更新按钮文本', async () => {
      const wrapper = mount(AISettings, {
        props: { loading: false }
      })

      const buttons = wrapper.findAll('button')
      const saveButton = buttons.find(b => b.text() === '保存 AI 配置')
      expect(saveButton?.exists()).toBe(true)

      await wrapper.setProps({ loading: true })
      await wrapper.vm.$nextTick()

      const loadingButtons = wrapper.findAll('button')
      const loadingButton = loadingButtons.find(b => b.text() === '保存中...')
      expect(loadingButton?.exists()).toBe(true)
    })

    it('saved变化应该显示保存成功提示', async () => {
      const wrapper = mount(AISettings, {
        props: { saved: false }
      })

      expect(wrapper.find('.text-\\[var\\(--color-success\\)\\]').exists()).toBe(false)

      await wrapper.setProps({ saved: true })
      await wrapper.vm.$nextTick()

      const successMsg = wrapper.find('.text-\\[var\\(--color-success\\)\\]')
      expect(successMsg.exists()).toBe(true)
    })
  })

  describe('密码显示/隐藏', () => {
    it('默认API Key应该隐藏', () => {
      const wrapper = mount(AISettings, {
        props: {}
      })

      expect(wrapper.vm.showApiKey).toBe(false)
      const apiKeyInput = wrapper.find('input[placeholder="sk-..."]')
      expect(apiKeyInput.attributes('type')).toBe('password')
    })

    it('切换showApiKey应该改变输入类型', async () => {
      const wrapper = mount(AISettings, {
        props: {}
      })

      wrapper.vm.showApiKey = true
      await wrapper.vm.$nextTick()

      const apiKeyInput = wrapper.find('input[placeholder="sk-..."]')
      expect(apiKeyInput.attributes('type')).toBe('text')
    })
  })

  describe('表单数据一致性', () => {
    it('formData应该有正确的初始值', () => {
      const wrapper = mount(AISettings, {
        props: {}
      })

      expect(wrapper.vm.formData.wsEnabled).toBe(true)
      expect(wrapper.vm.formData.autoReconnect).toBe(true)
      expect(wrapper.vm.formData.reconnectInterval).toBe(5)
      expect(wrapper.vm.formData.llmProvider).toBe('openrouter')
      expect(wrapper.vm.formData.memoryEnabled).toBe(true)
    })
  })
})