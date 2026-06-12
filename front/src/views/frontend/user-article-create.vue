<template>
    <div class="min-h-screen bg-background-secondary pb-12">
        <!-- 顶部工具栏 -->
        <div class="sticky top-0 z-40 bg-background-primary/95 backdrop-blur-sm border-b border-border-color">
            <div class="max-w-screen-xl mx-auto px-4 py-3">
                <div class="flex items-center justify-between">
                    <div class="flex items-center gap-4">
                        <button 
                            class="p-2 rounded-lg hover:bg-background-tertiary transition-colors"
                            title="返回"
                            @click="handleBack"
                        >
                            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 19l-7-7m0 0l7-7m-7 7h18"/>
                            </svg>
                        </button>
                        <h1 class="text-lg font-semibold text-text-primary">发布文章</h1>
                    </div>
                    
                    <!-- 自动保存状态 -->
                    <div class="flex items-center gap-3">
                        <div class="flex items-center gap-2 text-sm">
                            <span v-if="saveStatus === 'saving'" class="flex items-center gap-1.5 text-text-tertiary">
                                <svg class="animate-spin w-4 h-4" fill="none" viewBox="0 0 24 24">
                                    <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"/>
                                    <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.196A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"/>
                                </svg>
                                保存中...
                            </span>
                            <span v-else-if="saveStatus === 'saved'" class="flex items-center gap-1.5 text-success-color">
                                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"/>
                                </svg>
                                已保存
                            </span>
                            <span v-else-if="lastSaved" class="text-text-tertiary">
                                上次保存: {{ formatTime(lastSaved) }}
                            </span>
                        </div>
                        
                        <div class="flex items-center gap-2">
                            <button 
                                class="px-4 py-2 text-sm font-medium text-text-secondary hover:text-text-primary hover:bg-background-tertiary rounded-lg transition-colors"
                                :disabled="savingDraft"
                                @click="saveDraftManual"
                            >
                                {{ savingDraft ? '保存中...' : '存草稿' }}
                            </button>
                            <button 
                                class="px-4 py-2 text-sm font-medium text-white bg-primary-color hover:bg-primary-hover rounded-lg transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                                :disabled="!canPublish || publishing"
                                @click="publishArticle"
                            >
                                <span v-if="publishing" class="flex items-center gap-2">
                                    <svg class="animate-spin w-4 h-4" fill="none" viewBox="0 0 24 24">
                                        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"/>
                                        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.196A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"/>
                                    </svg>
                                    发布中...
                                </span>
                                <span v-else>发布文章</span>
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <!-- 主要内容区 -->
        <div class="max-w-screen-xl mx-auto px-4 py-6">
            <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
                <!-- 左侧编辑区 -->
                <div class="lg:col-span-2 space-y-6">
                    <!-- 标题输入 -->
                    <div class="bg-background-primary rounded-xl border border-border-color p-6">
                        <div class="space-y-4">
                            <div>
                                <label class="block text-sm font-medium text-text-secondary mb-2">
                                    文章标题 <span class="text-error-color">*</span>
                                </label>
                                <input
                                    v-model="form.title"
                                    type="text"
                                    class="w-full text-2xl font-bold text-text-primary placeholder-text-tertiary bg-transparent border-0 border-b-2 border-border-color focus:border-primary-color focus:ring-0 outline-none transition-colors pb-2"
                                    placeholder="请输入文章标题"
                                    @blur="validateField('title')"
                                >
                                <div class="flex justify-between mt-2">
                                    <span v-if="errors.title" class="text-sm text-error-color">{{ errors.title }}</span>
                                    <span v-else class="text-sm text-text-tertiary">一个好的标题能吸引更多读者</span>
                                    <span class="text-sm text-text-tertiary">{{ form.title.length }}/100</span>
                                </div>
                            </div>
                        </div>
                    </div>

                    <!-- 编辑器 -->
                    <div class="bg-background-primary rounded-xl border border-border-color overflow-hidden">
                        <!-- 编辑器工具栏 -->
                        <div class="flex items-center gap-1 px-4 py-2 bg-background-secondary border-b border-border-color flex-wrap">
                            <button 
                                v-for="tool in editorTools" 
                                :key="tool.name"
                                class="p-2 rounded hover:bg-background-tertiary transition-colors"
                                :title="tool.title"
                                @click="insertMarkdown(tool.prefix, tool.suffix)"
                            >
                                <component :is="tool.icon" class="w-4 h-4 text-text-secondary" />
                            </button>
                            <div class="w-px h-6 bg-border-color mx-2"></div>
                            <button 
                                class="px-3 py-1.5 text-sm rounded hover:bg-background-tertiary transition-colors"
                                :class="showPreview ? 'text-primary-color bg-primary-subtle' : 'text-text-secondary'"
                                @click="showPreview = !showPreview"
                            >
                                {{ showPreview ? '隐藏预览' : '显示预览' }}
                            </button>
                        </div>
                        
                        <!-- 编辑区域 -->
                        <div class="grid" :class="showPreview ? 'grid-cols-2' : 'grid-cols-1'">
                            <textarea 
                                ref="contentTextarea"
                                v-model="form.content"
                                class="w-full min-h-[500px] p-4 bg-background-primary text-text-primary resize-none outline-none font-mono text-sm leading-relaxed"
                                placeholder="在此输入文章内容...&#10;&#10;支持 Markdown 语法&#10;&#10;## 标题&#10;- 列表项&#10;**粗体** *斜体*"
                                @blur="validateField('content')"
                            ></textarea>
                            <div v-if="showPreview" class="border-l border-border-color p-4 overflow-auto max-h-[500px]">
                                <div class="prose prose-sm max-w-none" v-html="renderedContent"></div>
                            </div>
                        </div>
                        
                        <!-- 底部统计 -->
                        <div class="flex items-center justify-between px-4 py-2 bg-background-secondary border-t border-border-color text-sm text-text-tertiary">
                            <div class="flex items-center gap-4">
                                <span v-if="errors.content" class="text-error-color">{{ errors.content }}</span>
                            </div>
                            <div class="flex items-center gap-4">
                                <span>{{ contentStats.chars }} 字符</span>
                                <span>{{ contentStats.words }} 词</span>
                                <span>阅读约 {{ contentStats.readTime }} 分钟</span>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- 右侧设置区 -->
                <div class="space-y-6">
                    <!-- 发布设置 -->
                    <div class="bg-background-primary rounded-xl border border-border-color p-6">
                        <h3 class="text-lg font-semibold text-text-primary mb-4">发布设置</h3>
                        
                        <div class="space-y-4">
                            <!-- 分类选择 -->
                            <div>
                                <label class="block text-sm font-medium text-text-secondary mb-2">
                                    文章分类 <span class="text-error-color">*</span>
                                </label>
                                <select 
                                    v-model="form.categoryId"
                                    class="w-full px-3 py-2 bg-background-secondary border border-border-color rounded-lg text-text-primary focus:border-primary-color focus:ring-1 focus:ring-primary-color outline-none transition-colors"
                                    @blur="validateField('categoryId')"
                                >
                                    <option value="">请选择分类</option>
                                    <option v-for="cat in categories" :key="cat.id" :value="cat.id">
                                        {{ cat.name }}
                                    </option>
                                </select>
                                <p v-if="errors.categoryId" class="mt-1 text-sm text-error-color">{{ errors.categoryId }}</p>
                            </div>

                            <!-- 标签输入 -->
                            <div>
                                <label class="block text-sm font-medium text-text-secondary mb-2">
                                    文章标签
                                    <span class="text-text-tertiary font-normal">(最多5个)</span>
                                </label>
                                <div class="flex flex-wrap gap-2 p-2 bg-background-secondary border border-border-color rounded-lg min-h-[42px]" @click="focusTagInput">
                                    <span 
                                        v-for="(tag, index) in form.tags" 
                                        :key="tag"
                                        class="inline-flex items-center gap-1 px-2 py-1 bg-primary-subtle text-primary-color rounded text-sm"
                                    >
                                        {{ tag }}
                                        <button class="hover:text-error-color" @click.stop="removeTag(index)">
                                            <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
                                            </svg>
                                        </button>
                                    </span>
                                    <input
                                        ref="tagInput"
                                        v-model="tagInputValue"
                                        type="text"
                                        class="flex-1 min-w-[80px] bg-transparent outline-none text-text-primary placeholder-text-tertiary"
                                        placeholder="输入标签按回车"
                                        @keydown.enter.prevent="addTag"
                                        @keydown.backspace="handleTagBackspace"
                                    >
                                </div>
                                <p class="mt-1 text-xs text-text-tertiary">按回车添加标签，点击×删除</p>
                            </div>

                            <!-- 摘要 -->
                            <div>
                                <label class="block text-sm font-medium text-text-secondary mb-2">文章摘要</label>
                                <textarea 
                                    v-model="form.summary"
                                    rows="3"
                                    class="w-full px-3 py-2 bg-background-secondary border border-border-color rounded-lg text-text-primary placeholder-text-tertiary focus:border-primary-color focus:ring-1 focus:ring-primary-color outline-none transition-colors resize-none"
                                    placeholder="不填写将自动从正文提取"
                                    maxlength="200"
                                ></textarea>
                                <p class="mt-1 text-xs text-text-tertiary text-right">{{ form.summary.length }}/200</p>
                            </div>

                            <!-- 封面图片 -->
                            <div>
                                <label class="block text-sm font-medium text-text-secondary mb-2">封面图片</label>
                                <div 
                                    class="relative border-2 border-dashed rounded-lg overflow-hidden transition-colors"
                                    :class="isDragging ? 'border-primary-color bg-primary-subtle' : 'border-border-color hover:border-border-hover'"
                                    @dragenter.prevent="isDragging = true"
                                    @dragleave.prevent="isDragging = false"
                                    @dragover.prevent
                                    @drop.prevent="handleDrop"
                                    @click="triggerFileSelect"
                                >
                                    <input 
                                        ref="fileInput"
                                        type="file" 
                                        accept="image/*"
                                        class="hidden"
                                        @change="handleFileChange"
                                    >
                                    
                                    <template v-if="!form.cover">
                                        <div class="p-8 text-center cursor-pointer">
                                            <svg class="mx-auto w-10 h-10 text-text-tertiary mb-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z"/>
                                            </svg>
                                            <p class="text-sm text-text-secondary">点击或拖拽上传封面</p>
                                            <p class="text-xs text-text-tertiary mt-1">支持 JPG、PNG，最大 5MB</p>
                                        </div>
                                    </template>
                                    <template v-else>
                                        <div class="relative aspect-video">
                                            <img :src="form.cover" class="w-full h-full object-cover" alt="封面" loading="lazy" @error="(e) => { e.target.style.display='none'; e.target.nextElementSibling.style.display='flex' }" />
                                            <div class="hidden absolute inset-0 bg-background-tertiary items-center justify-center"></div>
                                            <div class="absolute inset-0 bg-[rgba(10,10,20,0.5)] opacity-0 hover:opacity-100 transition-opacity flex items-center justify-center gap-2">
                                                <button class="p-2 bg-white rounded-full hover:bg-gray-100" @click.stop="triggerFileSelect">
                                                    <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-8l-4-4m0 0L8 8m4-4v12"/>
                                                    </svg>
                                                </button>
                                                <button class="p-2 bg-white rounded-full hover:bg-gray-100 text-error-color" @click.stop="removeCover">
                                                    <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/>
                                                    </svg>
                                                </button>
                                            </div>
                                        </div>
                                    </template>
                                </div>
                            </div>

                            <!-- 发布选项 -->
                            <div>
                                <label class="block text-sm font-medium text-text-secondary mb-2">发布选项</label>
                                <div class="space-y-2">
                                    <label class="flex items-center gap-2 cursor-pointer">
                                        <input 
                                            v-model="form.status" 
                                            type="radio"
                                            value="PUBLISHED"
                                            class="w-4 h-4 text-primary-color"
                                        >
                                        <span class="text-text-primary">立即发布</span>
                                    </label>
                                    <label class="flex items-center gap-2 cursor-pointer">
                                        <input 
                                            v-model="form.status" 
                                            type="radio"
                                            value="DRAFT"
                                            class="w-4 h-4 text-primary-color"
                                        >
                                        <span class="text-text-primary">保存为草稿</span>
                                    </label>
                                </div>
                            </div>
                        </div>
                    </div>

                    <!-- 操作按钮 -->
                    <div class="flex gap-3">
                        <button 
                            class="flex-1 px-4 py-2 text-sm font-medium text-text-secondary hover:text-error-color border border-border-color hover:border-error-color rounded-lg transition-colors"
                            @click="confirmClear"
                        >
                            清空内容
                        </button>
                        <button 
                            class="flex-1 px-4 py-2 text-sm font-medium text-text-secondary hover:text-primary-color border border-border-color hover:border-primary-color rounded-lg transition-colors"
                            @click="showDraftsModal = true"
                        >
                            草稿箱 ({{ draftsCount }})
                        </button>
                    </div>
                </div>
            </div>
        </div>

        <!-- 草稿箱弹窗 -->
        <Teleport to="body">
            <div v-if="showDraftsModal" class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-[rgba(10,10,20,0.5)]" @click="showDraftsModal = false">
                <div class="bg-background-primary rounded-xl max-w-lg w-full max-h-[80vh] overflow-hidden" @click.stop>
                    <div class="flex items-center justify-between p-4 border-b border-border-color">
                        <h3 class="text-lg font-semibold text-text-primary">草稿箱</h3>
                        <button class="p-1 hover:bg-background-tertiary rounded" @click="showDraftsModal = false">
                            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
                            </svg>
                        </button>
                    </div>
                    <div class="p-4 overflow-y-auto max-h-[60vh]">
                        <div v-if="drafts.length === 0" class="text-center py-8 text-text-tertiary">
                            暂无草稿
                        </div>
                        <div v-else class="space-y-3">
                            <div 
                                v-for="draft in drafts" 
                                :key="draft.id"
                                class="p-3 bg-background-secondary rounded-lg hover:bg-background-tertiary cursor-pointer transition-colors"
                                @click="loadDraft(draft)"
                            >
                                <div class="flex items-start justify-between">
                                    <div class="flex-1 min-w-0">
                                        <h4 class="font-medium text-text-primary truncate">{{ draft.title || '无标题' }}</h4>
                                        <p class="text-sm text-text-tertiary mt-1">{{ formatTime(draft.savedAt) }}</p>
                                    </div>
                                    <button 
                                        class="p-1 text-text-tertiary hover:text-error-color ml-2"
                                        @click.stop="deleteDraft(draft.id)"
                                    >
                                        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/>
                                        </svg>
                                    </button>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </Teleport>

        <!-- 离开确认弹窗 -->
        <Teleport to="body">
            <div v-if="showLeaveConfirm" class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-[rgba(10,10,20,0.5)]">
                <div class="bg-background-primary rounded-xl max-w-sm w-full p-6">
                    <h3 class="text-lg font-semibold text-text-primary mb-2">确认离开？</h3>
                    <p class="text-text-secondary mb-4">您有未保存的内容，离开后将丢失。</p>
                    <div class="flex gap-3 justify-end">
                        <button class="px-4 py-2 text-text-secondary hover:text-text-primary" @click="showLeaveConfirm = false">取消</button>
                        <button class="px-4 py-2 bg-error-color text-white rounded-lg hover:bg-error-color/90" @click="confirmLeave">确认离开</button>
                    </div>
                </div>
            </div>
        </Teleport>
    </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRouter, onBeforeRouteLeave } from 'vue-router'
import { marked } from 'marked'
import { sanitizeHtml } from '@/utils/xss'
import { showMessage, showModel } from '@/utils'
import { getCategories } from '@/api/frontend/category'
import logger from '@/utils/logger'

const router = useRouter()

// 编辑器工具图标
const BoldIcon = {
    render: () => h('svg', { class: 'w-4 h-4', fill: 'none', stroke: 'currentColor', viewBox: '0 0 24 24' }, [
        h('path', { 'stroke-linecap': 'round', 'stroke-linejoin': 'round', 'stroke-width': '2', d: 'M6 12h8a4 4 0 100-8H6v8zm0 0h10a4 4 0 110 8H6v-8z' })
    ])
}
const ItalicIcon = {
    render: () => h('svg', { class: 'w-4 h-4', fill: 'none', stroke: 'currentColor', viewBox: '0 0 24 24' }, [
        h('path', { 'stroke-linecap': 'round', 'stroke-linejoin': 'round', 'stroke-width': '2', d: 'M10 20l4-16m4 4l4 4-4 4M6 16l-4-4 4-4' })
    ])
}
const HeadingIcon = {
    render: () => h('svg', { class: 'w-4 h-4', fill: 'none', stroke: 'currentColor', viewBox: '0 0 24 24' }, [
        h('path', { 'stroke-linecap': 'round', 'stroke-linejoin': 'round', 'stroke-width': '2', d: 'M4 6h16M4 12h16M4 18h7' })
    ])
}
const ListIcon = {
    render: () => h('svg', { class: 'w-4 h-4', fill: 'none', stroke: 'currentColor', viewBox: '0 0 24 24' }, [
        h('path', { 'stroke-linecap': 'round', 'stroke-linejoin': 'round', 'stroke-width': '2', d: 'M4 6h16M4 12h16M4 18h16' })
    ])
}
const QuoteIcon = {
    render: () => h('svg', { class: 'w-4 h-4', fill: 'none', stroke: 'currentColor', viewBox: '0 0 24 24' }, [
        h('path', { 'stroke-linecap': 'round', 'stroke-linejoin': 'round', 'stroke-width': '2', d: 'M7 8h10M7 12h4m1 8l-4-4H5a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v8a2 2 0 01-2 2h-3l-4 4z' })
    ])
}
const CodeIcon = {
    render: () => h('svg', { class: 'w-4 h-4', fill: 'none', stroke: 'currentColor', viewBox: '0 0 24 24' }, [
        h('path', { 'stroke-linecap': 'round', 'stroke-linejoin': 'round', 'stroke-width': '2', d: 'M10 20l4-16m4 4l4 4-4 4M6 16l-4-4 4-4' })
    ])
}
const LinkIcon = {
    render: () => h('svg', { class: 'w-4 h-4', fill: 'none', stroke: 'currentColor', viewBox: '0 0 24 24' }, [
        h('path', { 'stroke-linecap': 'round', 'stroke-linejoin': 'round', 'stroke-width': '2', d: 'M13.828 10.172a4 4 0 00-5.656 0l-4 4a4 4 0 105.656 5.656l1.102-1.101m-.758-4.899a4 4 0 005.656 0l4-4a4 4 0 00-5.656-5.656l-1.1 1.1' })
    ])
}
const ImageIcon = {
    render: () => h('svg', { class: 'w-4 h-4', fill: 'none', stroke: 'currentColor', viewBox: '0 0 24 24' }, [
        h('path', { 'stroke-linecap': 'round', 'stroke-linejoin': 'round', 'stroke-width': '2', d: 'M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z' })
    ])
}

import { h } from 'vue'

// 状态
const publishing = ref(false)
const savingDraft = ref(false)
const saveStatus = ref('') // '', 'saving', 'saved'
const lastSaved = ref(null)
const showPreview = ref(false)
const isDragging = ref(false)
const showDraftsModal = ref(false)
const showLeaveConfirm = ref(false)
const leaveCallback = ref(null)

// 表单数据
const form = reactive({
    title: '',
    content: '',
    summary: '',
    categoryId: '',
    tags: [],
    cover: '',
    status: 'PUBLISHED'
})

// 错误信息
const errors = reactive({
    title: '',
    content: '',
    categoryId: ''
})

// 标签输入
const tagInputValue = ref('')
const tagInput = ref(null)

// 分类列表
const categories = ref([])

// 草稿列表
const drafts = ref([])
const draftsCount = computed(() => drafts.value.length)

// 编辑器工具
const editorTools = [
    { name: 'bold', title: '粗体', icon: BoldIcon, prefix: '**', suffix: '**' },
    { name: 'italic', title: '斜体', icon: ItalicIcon, prefix: '*', suffix: '*' },
    { name: 'heading', title: '标题', icon: HeadingIcon, prefix: '## ', suffix: '' },
    { name: 'list', title: '列表', icon: ListIcon, prefix: '- ', suffix: '' },
    { name: 'quote', title: '引用', icon: QuoteIcon, prefix: '> ', suffix: '' },
    { name: 'code', title: '代码', icon: CodeIcon, prefix: '```\n', suffix: '\n```' },
    { name: 'link', title: '链接', icon: LinkIcon, prefix: '[', suffix: '](url)' },
    { name: 'image', title: '图片', icon: ImageIcon, prefix: '![', suffix: '](url)' }
]

// 内容统计
const contentStats = computed(() => {
    const text = form.content || ''
    const chars = text.length
    const words = text.trim() ? text.trim().split(/\s+/).length : 0
    const readTime = Math.max(1, Math.ceil(words / 300))
    return { chars, words, readTime }
})

// Markdown 渲染
const renderedContent = computed(() => {
    return sanitizeHtml(marked(form.content || '', { breaks: true }))
})

// 是否可以发布
const canPublish = computed(() => {
    return form.title.trim() && form.content.trim() && form.categoryId
})

// 是否有未保存的内容
const hasUnsavedContent = computed(() => {
    return form.title.trim() || form.content.trim()
})

// 验证字段
const validateField = (field) => {
    errors[field] = ''
    
    switch (field) {
        case 'title':
            if (!form.title.trim()) {
                errors.title = '请输入文章标题'
            } else if (form.title.trim().length < 2) {
                errors.title = '标题至少需要2个字符'
            }
            break
        case 'content':
            if (!form.content.trim()) {
                errors.content = '请输入文章内容'
            } else if (form.content.trim().length < 10) {
                errors.content = '内容至少需要10个字符'
            }
            break
        case 'categoryId':
            if (!form.categoryId) {
                errors.categoryId = '请选择文章分类'
            }
            break
    }
    
    return !errors[field]
}

// 验证整个表单
const validateForm = () => {
    const fields = ['title', 'content', 'categoryId']
    return fields.every(field => validateField(field))
}

// 添加标签
const addTag = () => {
    const value = tagInputValue.value.trim()
    if (!value) {return}
    
    if (form.tags.length >= 5) {
        showMessage('最多只能添加5个标签', 'warning')
        return
    }
    
    if (form.tags.includes(value)) {
        showMessage('标签已存在', 'warning')
        return
    }
    
    form.tags.push(value)
    tagInputValue.value = ''
}

// 删除标签
const removeTag = (index) => {
    form.tags.splice(index, 1)
}

// 聚焦标签输入
const focusTagInput = () => {
    tagInput.value?.focus()
}

// 处理标签退格
const handleTagBackspace = () => {
    if (tagInputValue.value === '' && form.tags.length > 0) {
        form.tags.pop()
    }
}

// 插入Markdown
const contentTextarea = ref(null)
const insertMarkdown = (prefix, suffix) => {
    const textarea = contentTextarea.value
    if (!textarea) {return}
    
    const start = textarea.selectionStart
    const end = textarea.selectionEnd
    const text = form.content
    const selected = text.substring(start, end)
    
    form.content = text.substring(0, start) + prefix + selected + suffix + text.substring(end)
    
    setTimeout(() => {
        textarea.focus()
        const newCursor = start + prefix.length + selected.length
        textarea.setSelectionRange(newCursor, newCursor)
    }, 0)
}

// 文件上传
const fileInput = ref(null)
const triggerFileSelect = () => {
    fileInput.value?.click()
}

const handleFileChange = (e) => {
    const file = e.target.files[0]
    if (file) {processFile(file)}
}

const handleDrop = (e) => {
    isDragging.value = false
    const file = e.dataTransfer.files[0]
    if (file) {processFile(file)}
}

const processFile = (file) => {
    if (!file.type.startsWith('image/')) {
        showMessage('请选择图片文件', 'error')
        return
    }
    
    if (file.size > 5 * 1024 * 1024) {
        showMessage('图片大小不能超过5MB', 'error')
        return
    }
    
    const reader = new FileReader()
    reader.onload = (e) => {
        form.cover = e.target.result
    }
    reader.readAsDataURL(file)
}

const removeCover = () => {
    form.cover = ''
    if (fileInput.value) {fileInput.value.value = ''}
}

// 草稿管理
const DRAFTS_KEY = 'article_drafts'
const CURRENT_DRAFT_KEY = 'article_current_draft'

const loadDraftsFromStorage = () => {
    try {
        const stored = localStorage.getItem(DRAFTS_KEY)
        if (stored) {
            drafts.value = JSON.parse(stored)
        }
    } catch (e) {
        logger.error('加载草稿失败:', e)
    }
}

const saveDraftsToStorage = () => {
    try {
        localStorage.setItem(DRAFTS_KEY, JSON.stringify(drafts.value))
    } catch (e) {
        logger.error('保存草稿失败:', e)
    }
}

// 自动保存
let autoSaveTimer = null
const autoSave = () => {
    if (!form.title.trim() && !form.content.trim()) {return}
    
    saveStatus.value = 'saving'
    
    const draft = {
        id: Date.now(),
        ...form,
        savedAt: new Date().toISOString()
    }
    
    // 保存到当前草稿
    localStorage.setItem(CURRENT_DRAFT_KEY, JSON.stringify(draft))
    
    saveStatus.value = 'saved'
    lastSaved.value = new Date()
    
    setTimeout(() => {
        if (saveStatus.value === 'saved') {
            saveStatus.value = ''
        }
    }, 2000)
}

// 手动保存草稿
const saveDraftManual = () => {
    if (!form.title.trim() && !form.content.trim()) {
        showMessage('没有内容可保存', 'warning')
        return
    }

    savingDraft.value = true

    const draft = {
        id: Date.now(),
        ...form,
        savedAt: new Date().toISOString()
    }

    drafts.value.unshift(draft)
    if (drafts.value.length > 10) {
        drafts.value = drafts.value.slice(0, 10)
    }

    saveDraftsToStorage()
    savingDraft.value = false
    showMessage('草稿已保存', 'success')
}

// 加载草稿
const loadDraft = (draft) => {
    Object.assign(form, {
        title: draft.title || '',
        content: draft.content || '',
        summary: draft.summary || '',
        categoryId: draft.categoryId || '',
        tags: draft.tags || [],
        cover: draft.cover || '',
        status: draft.status || 'DRAFT'
    })
    showDraftsModal.value = false
    showMessage('草稿已加载', 'success')
}

// 删除草稿
const deleteDraft = (id) => {
    drafts.value = drafts.value.filter(d => d.id !== id)
    saveDraftsToStorage()
}

// 恢复当前草稿
const restoreCurrentDraft = () => {
    try {
        const stored = localStorage.getItem(CURRENT_DRAFT_KEY)
        if (stored) {
            const draft = JSON.parse(stored)
            const savedTime = new Date(draft.savedAt)
            const hoursDiff = (new Date() - savedTime) / (1000 * 60 * 60)
            
            if (hoursDiff < 24) {
                showModel('检测到未保存的内容，是否恢复？', 'info', '恢复').then(() => {
                    Object.assign(form, draft)
                    showMessage('内容已恢复', 'success')
                }).catch(() => {
                    localStorage.removeItem(CURRENT_DRAFT_KEY)
                })
            } else {
                localStorage.removeItem(CURRENT_DRAFT_KEY)
            }
        }
    } catch (e) {
        logger.error('恢复草稿失败:', e)
    }
}

// 获取分类列表清空内容
const confirmClear = async () => {
    if (!form.title.trim() && !form.content.trim()) {
        showMessage('没有内容可清空', 'warning')
        return
    }
    
    try {
        await showModel('确定要清空所有内容吗？此操作不可恢复。', 'warning', '确认清空')
        Object.assign(form, {
            title: '',
            content: '',
            summary: '',
            categoryId: '',
            tags: [],
            cover: '',
            status: 'PUBLISHED'
        })
        localStorage.removeItem(CURRENT_DRAFT_KEY)
        showMessage('内容已清空', 'success')
    } catch (e) {
        // 用户取消
    }
}

// 发布文章
const publishArticle = async () => {
    if (!validateForm()) {
        showMessage('请检查表单填写是否正确', 'warning')
        return
    }
    
    publishing.value = true
    
    try {
        // 模拟API调用
        await new Promise(resolve => setTimeout(resolve, 1500))
        
        // 清除草稿
        localStorage.removeItem(CURRENT_DRAFT_KEY)
        
        showMessage(form.status === 'PUBLISHED' ? '文章发布成功！' : '草稿保存成功！', 'success')
        
        setTimeout(() => {
            router.push('/user')
        }, 1000)
    } catch (error) {
        showMessage('发布失败，请重试', 'error')
    } finally {
        publishing.value = false
    }
}

// 返回
const handleBack = () => {
    if (hasUnsavedContent.value) {
        showLeaveConfirm.value = true
        leaveCallback.value = () => router.push('/user')
    } else {
        router.push('/user')
    }
}

const confirmLeave = () => {
    showLeaveConfirm.value = false
    leaveCallback.value?.()
}

// 获取分类
const getCategoriesList = async () => {
    try {
        const res = await getCategories()
        if (res?.data) {
            categories.value = res.data
        }
    } catch (error) {
        logger.error('获取分类失败:', error)
    }
}

// 格式化时间
const formatTime = (dateStr) => {
    if (!dateStr) {return ''}
    const date = new Date(dateStr)
    const now = new Date()
    const diff = now - date
    
    if (diff < 60000) {return '刚刚'}
    if (diff < 3600000) {return `${Math.floor(diff / 60000)}分钟前`}
    if (diff < 86400000) {return `${Math.floor(diff / 3600000)}小时前`}
    return date.toLocaleDateString('zh-CN')
}

// 监听表单变化，自动保存
watch(() => ({ ...form }), () => {
    if (autoSaveTimer) {clearTimeout(autoSaveTimer)}
    autoSaveTimer = setTimeout(autoSave, 3000)
}, { deep: true })

// 路由守卫
onBeforeRouteLeave((to, from, next) => {
    if (hasUnsavedContent.value) {
        showLeaveConfirm.value = true
        leaveCallback.value = next
    } else {
        next()
    }
})

// 生命周期
onMounted(() => {
    getCategoriesList()
    loadDraftsFromStorage()
    restoreCurrentDraft()
})

onUnmounted(() => {
    if (autoSaveTimer) {clearTimeout(autoSaveTimer)}
})
</script>

<style scoped>
.prose :deep(h1) { font-size: 1.5em; font-weight: bold; margin: 0.5em 0; }
.prose :deep(h2) { font-size: 1.25em; font-weight: bold; margin: 0.5em 0; }
.prose :deep(p) { margin: 0.5em 0; line-height: 1.6; }
.prose :deep(ul) { list-style: disc; padding-left: 1.5em; margin: 0.5em 0; }
.prose :deep(ol) { list-style: decimal; padding-left: 1.5em; margin: 0.5em 0; }
.prose :deep(blockquote) { border-left: 4px solid var(--border-color); padding-left: 1em; margin: 0.5em 0; color: var(--text-secondary); }
.prose :deep(code) { background: var(--bg-tertiary); padding: 0.2em 0.4em; border-radius: 3px; font-family: monospace; }
.prose :deep(pre) { background: var(--bg-tertiary); padding: 1em; border-radius: 6px; overflow-x: auto; }
.prose :deep(pre code) { background: none; padding: 0; }
.prose :deep(a) { color: var(--primary-color); text-decoration: underline; }
.prose :deep(img) { max-width: 100%; height: auto; }
</style>
