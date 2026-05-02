<template>
  <div class="space-y-6">
    <!-- 模式切换 -->
    <div class="bg-[var(--bg-card)] rounded-xl p-4 shadow-sm">
      <div class="flex items-center justify-between">
        <div>
          <h2 class="text-lg font-bold text-text-primary">AI 配置</h2>
          <p class="text-sm text-text-secondary mt-1">配置 AI 助手的行为和连接方式</p>
        </div>
        <div class="flex items-center gap-3 bg-[var(--background-primary)] rounded-lg p-1">
          <button
            type="button"
            class="px-4 py-1.5 rounded-md text-sm font-medium transition-all duration-200"
            :class="!isExpertMode ? 'bg-[var(--color-primary)] text-white shadow-md' : 'text-text-secondary hover:text-text-primary'"
            @click="isExpertMode = false"
          >
            快速模式
          </button>
          <button
            type="button"
            class="px-4 py-1.5 rounded-md text-sm font-medium transition-all duration-200"
            :class="isExpertMode ? 'bg-[var(--color-primary)] text-white shadow-md' : 'text-text-secondary hover:text-text-primary'"
            @click="isExpertMode = true"
          >
            专家模式
          </button>
        </div>
      </div>
    </div>

    <!-- WebSocket 配置（专家模式） -->
    <div v-if="isExpertMode" class="bg-[var(--bg-card)] rounded-xl p-6 shadow-sm">
      <h2 class="text-lg font-bold text-text-primary mb-4">WebSocket 实时连接</h2>
      <p class="text-text-secondary mb-4">配置 WebSocket 连接以接收 AI 助手的实时响应和通知。</p>

      <div class="space-y-4">
        <div class="flex items-center justify-between">
          <div>
            <h3 :id="'label-wsEnabled'" class="font-medium text-text-primary">启用 WebSocket</h3>
            <p class="text-sm text-text-secondary mt-1">开启后可在 AI 对话时接收实时响应</p>
          </div>
          <label for="wsEnabled" class="relative inline-flex items-center cursor-pointer">
            <input
              id="wsEnabled"
              v-model="formData.wsEnabled"
              type="checkbox"
              class="sr-only peer"
              aria-labelledby="label-wsEnabled"
            >
            <div class="w-11 h-6 bg-[var(--toggle-track)] peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-[var(--toggle-border)] after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-[var(--color-primary)]"></div>
          </label>
        </div>

        <div>
          <label class="block text-sm font-medium text-text-secondary mb-2">WebSocket 地址</label>
          <input
            v-model="formData.wsUrl"
            type="text"
            :disabled="!formData.wsEnabled"
            class="w-full px-4 py-2 border border-border-color rounded-lg bg-background-primary focus:border-[var(--color-primary)] focus:ring-2 focus:ring-[var(--color-primary)]/20 outline-none transition-all duration-200 disabled:opacity-50"
            placeholder="ws://localhost:8080/ws/agent"
          >
          <p class="text-xs text-text-tertiary mt-1">留空将使用默认地址</p>
        </div>

        <div class="flex items-center justify-between">
          <div>
            <h3 :id="'label-autoReconnect'" class="font-medium text-text-primary">自动重连</h3>
            <p class="text-sm text-text-secondary mt-1">连接断开时自动尝试重新连接</p>
          </div>
          <label for="autoReconnect" class="relative inline-flex items-center cursor-pointer">
            <input
              id="autoReconnect"
              v-model="formData.autoReconnect"
              type="checkbox"
              :disabled="!formData.wsEnabled"
              class="sr-only peer"
              aria-labelledby="label-autoReconnect"
            >
            <div class="w-11 h-6 bg-[var(--toggle-track)] peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-[var(--toggle-border)] after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-[var(--color-primary)] disabled:opacity-50"></div>
          </label>
        </div>

        <div>
          <label class="block text-sm font-medium text-text-secondary mb-2">重连间隔（秒）</label>
          <input
            v-model.number="formData.reconnectInterval"
            type="number"
            :disabled="!formData.wsEnabled || !formData.autoReconnect"
            min="1"
            max="60"
            class="w-full px-4 py-2 border border-border-color rounded-lg bg-background-primary focus:border-[var(--color-primary)] focus:ring-2 focus:ring-[var(--color-primary)]/20 outline-none transition-all duration-200 disabled:opacity-50"
          >
        </div>
      </div>
    </div>

    <!-- LLM API Key 配置（专家模式） -->
    <div v-if="isExpertMode" class="bg-[var(--bg-card)] rounded-xl p-6 shadow-sm">
      <h2 class="text-lg font-bold text-text-primary mb-4">LLM API Key 配置</h2>
      <p class="text-text-secondary mb-4">配置您自己的 LLM API Key 以获得更好的 AI 响应质量。</p>

      <div class="space-y-4">
        <div>
          <label class="block text-sm font-medium text-text-secondary mb-2">Provider</label>
          <select
            v-model="formData.llmProvider"
            class="w-full px-4 py-2 border border-border-color rounded-lg bg-background-primary focus:border-[var(--color-primary)] focus:ring-2 focus:ring-[var(--color-primary)]/20 outline-none transition-all duration-200"
          >
            <option value="deepseek">DeepSeek</option>
            <option value="openrouter">OpenRouter (支持 DeepSeek 等多模型)</option>
            <option value="openai">OpenAI</option>
            <option value="anthropic">Anthropic (Claude)</option>
            <option value="zhipu">智谱 GLM</option>
            <option value="baidu">百度文心</option>
            <option value="azure">Azure OpenAI</option>
            <option value="custom">自定义</option>
          </select>
        </div>

        <div>
          <label class="block text-sm font-medium text-text-secondary mb-2">API Key</label>
          <div class="relative">
            <input
              v-model="formData.apiKey"
              :type="showApiKey ? 'text' : 'password'"
              class="w-full px-4 py-2 pr-10 border border-border-color rounded-lg bg-background-primary focus:border-[var(--color-primary)] focus:ring-2 focus:ring-[var(--color-primary)]/20 outline-none transition-all duration-200"
              placeholder="sk-..."
            >
            <button
              type="button"
              class="absolute right-3 top-1/2 -translate-y-1/2 text-text-tertiary hover:text-text-primary"
              @click="showApiKey = !showApiKey"
            >
              <svg v-if="!showApiKey" class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
              </svg>
              <svg v-else class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.88 9.88l-3.29-3.29m7.532 7.532l3.29 3.29M3 3l3.59 3.59m0 0A9.953 9.953 0 0112 5c4.478 0 8.268 2.943 9.543 7a10.025 10.025 0 01-4.132 5.411m0 0L21 21" />
              </svg>
            </button>
          </div>
          <p class="text-xs text-text-tertiary mt-1">API Key 将被加密存储</p>
        </div>

        <div>
          <label class="block text-sm font-medium text-text-secondary mb-2">模型</label>
          <div class="space-y-2">
            <select
              v-model="selectedModelPreset"
              class="w-full px-4 py-2 border border-border-color rounded-lg bg-background-primary focus:border-[var(--color-primary)] focus:ring-2 focus:ring-[var(--color-primary)]/20 outline-none transition-all duration-200"
            >
              <option value="">-- 选择常用模型 --</option>
              <optgroup label="DeepSeek（最新旗舰：V4-Pro / V4-Flash）">
                <option value="deepseek-chat">DeepSeek V3（当前使用）</option>
                <option value="deepseek/deepseek-chat-v4-pro">DeepSeek V4-Pro（上下文1M token）</option>
                <option value="deepseek/deepseek-chat-v4-flash">DeepSeek V4-Flash</option>
                <option value="deepseek-reasoner">DeepSeek R1（推理模型）</option>
              </optgroup>
              <optgroup label="OpenAI（最新旗舰：GPT-5.4 / GPT-5.5）">
                <option value="gpt-4o">GPT-4o（当前使用）</option>
                <option value="gpt-4o-mini">GPT-4o Mini</option>
                <option value="gpt-4-turbo">GPT-4-Turbo</option>
                <option value="gpt-5.4">GPT-5.4（最新旗舰）</option>
                <option value="gpt-5.5">GPT-5.5</option>
              </optgroup>
              <optgroup label="Anthropic（最新旗舰：Claude 4.7 Opus）">
                <option value="claude-3-5-sonnet">Claude 3.5 Sonnet（当前使用）</option>
                <option value="claude-3-5-haiku">Claude 3.5 Haiku</option>
                <option value="claude-4.7-opus">Claude 4.7 Opus（最新旗舰，1.5M上下文）</option>
                <option value="claude-4.7-sonnet">Claude 4.7 Sonnet</option>
              </optgroup>
              <optgroup label="智谱 GLM（最新旗舰：GLM-5系列）">
                <option value="glm-4-plus">GLM-4 Plus（当前使用）</option>
                <option value="glm-4-flash">GLM-4 Flash</option>
                <option value="glm-5-plus">GLM-5 Plus（最新旗舰）</option>
                <option value="glm-5-flash">GLM-5 Flash</option>
              </optgroup>
              <optgroup label="OpenRouter 热门模型">
                <option value="openrouter/auto">OpenRouter Auto（智能选择最优模型）</option>
                <option value="x-ai/grok-4.1-fast">Grok 4.1 Fast</option>
                <option value="google/gemini-2.5-pro">Gemini 2.5 Pro</option>
                <option value="google/gemini-2.5-flash">Gemini 2.5 Flash</option>
              </optgroup>
              <option value="__custom__">自定义模型...</option>
            </select>
            <input
              v-if="selectedModelPreset === '__custom__'"
              v-model="formData.model"
              type="text"
              class="w-full px-4 py-2 border border-border-color rounded-lg bg-background-primary focus:border-[var(--color-primary)] focus:ring-2 focus:ring-[var(--color-primary)]/20 outline-none transition-all duration-200"
              placeholder="输入自定义模型名称"
            >
            <input
              v-else-if="selectedModelPreset"
              v-model="formData.model"
              type="text"
              readonly
              class="w-full px-4 py-2 border border-border-color rounded-lg bg-[var(--bg-secondary)] cursor-not-allowed"
            >
            <input
              v-else
              v-model="formData.model"
              type="text"
              class="w-full px-4 py-2 border border-border-color rounded-lg bg-background-primary focus:border-[var(--color-primary)] focus:ring-2 focus:ring-[var(--color-primary)]/20 outline-none transition-all duration-200"
              placeholder="或直接输入模型名称"
            >
          </div>
        </div>

        <button
          class="btn btn-outline px-4 py-2 text-sm"
          :disabled="loading || !formData.apiKey"
          @click="$emit('test-connection', formData)"
        >
          {{ loading ? '测试中...' : '测试连接' }}
        </button>
      </div>
    </div>

    <!-- 记忆配置 -->
    <div class="bg-[var(--bg-card)] rounded-xl p-6 shadow-sm">
      <h2 class="text-lg font-bold text-text-primary mb-4">AI 记忆配置</h2>
      <p class="text-text-secondary mb-4">配置您的写作风格和个人偏好，AI 将根据这些信息提供更个性化的服务。</p>

      <div class="space-y-4">
        <div>
          <label class="block text-sm font-medium text-text-secondary mb-2">写作风格</label>
          <textarea
            v-model="formData.writingStyle"
            rows="3"
            class="w-full px-4 py-2 border border-border-color rounded-lg bg-background-primary focus:border-[var(--color-primary)] focus:ring-2 focus:ring-[var(--color-primary)]/20 outline-none transition-all duration-200"
            placeholder="例如：我喜欢简洁明了的风格，避免过多专业术语..."
          ></textarea>
        </div>

        <div>
          <label class="block text-sm font-medium text-text-secondary mb-2">个人简介</label>
          <textarea
            v-model="formData.personalBio"
            rows="3"
            class="w-full px-4 py-2 border border-border-color rounded-lg bg-background-primary focus:border-[var(--color-primary)] focus:ring-2 focus:ring-[var(--color-primary)]/20 outline-none transition-all duration-200"
            placeholder="介绍一下你自己，让 AI 更好地理解你的背景和需求..."
          ></textarea>
        </div>

        <div class="flex items-center justify-between">
          <div>
            <h3 :id="'label-memoryEnabled'" class="font-medium text-text-primary">启用记忆</h3>
            <p class="text-sm text-text-secondary mt-1">AI 会在对话中参考你的配置信息</p>
          </div>
          <label for="memoryEnabled" class="relative inline-flex items-center cursor-pointer">
            <input
              id="memoryEnabled"
              v-model="formData.memoryEnabled"
              type="checkbox"
              class="sr-only peer"
              aria-labelledby="label-memoryEnabled"
            >
            <div class="w-11 h-6 bg-[var(--toggle-track)] peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-[var(--toggle-border)] after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-[var(--color-primary)]"></div>
          </label>
        </div>

        <div class="text-xs text-text-tertiary">
          字数：{{ (formData.writingStyle + formData.personalBio).length }} / 500
        </div>
      </div>
    </div>

    <!-- 保存按钮 -->
    <div class="flex items-center gap-3">
      <button
        class="btn btn-primary px-6 py-2 text-sm font-medium transition-all duration-300 hover:scale-105"
        :disabled="loading"
        @click="$emit('save', formData)"
      >
        {{ loading ? '保存中...' : '保存 AI 配置' }}
      </button>
      <span v-if="saved" class="text-[var(--color-success)] text-sm flex items-center gap-1">
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
        </svg>
        保存成功
      </span>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'

defineProps({
  loading: {
    type: Boolean,
    default: false
  },
  saved: {
    type: Boolean,
    default: false
  }
})

defineEmits(['save', 'test-connection'])

const formData = ref({
  wsEnabled: true,
  wsUrl: '',
  autoReconnect: true,
  reconnectInterval: 5,
  llmProvider: 'deepseek',
  apiKey: '',
  model: '',
  writingStyle: '',
  personalBio: '',
  memoryEnabled: true
})

const selectedModelPreset = ref('')
const showApiKey = ref(false)
const isExpertMode = ref(false)

// 同步预设模型选择
watch(selectedModelPreset, (preset) => {
  if (preset && preset !== '__custom__') {
    formData.value.model = preset
  }
})
</script>
