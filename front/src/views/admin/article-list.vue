<template>
    <div class="container mx-auto px-4 py-6">
        <!-- 页面标题 -->
        <div class="mb-6">
            <h1 class="text-2xl font-bold text-gray-800 mb-2">文章管理</h1>
            <p class="text-gray-600">管理和发布您的博客文章</p>
        </div>

        <!-- 搜索和筛选区域 -->
        <div class="bg-white rounded-lg shadow-sm border border-gray-200 p-4 mb-6">
            <div class="flex flex-col md:flex-row md:items-center gap-4">
                <div class="flex-1">
                    <label class="block text-sm font-medium text-gray-700 mb-1">文章标题</label>
                    <el-input v-model="searchTitle" placeholder="请输入文章标题" class="w-full" clearable />
                </div>
                <div class="flex-1">
                    <label class="block text-sm font-medium text-gray-700 mb-1">发布日期</label>
                    <el-date-picker
v-model="pickDate" type="daterange" range-separator="至" start-placeholder="开始时间"
                        end-placeholder="结束时间" :shortcuts="shortcuts" @change="datepickerChange" />
                </div>
                <div class="flex items-end gap-2">
                    <el-button type="primary" :icon="Search" class="px-4" @click="getTableData">查询</el-button>
                    <el-button :icon="RefreshRight" @click="reset">重置</el-button>
                </div>
            </div>
        </div>

        <!-- 文章列表区域 -->
        <div class="bg-white rounded-lg shadow-sm border border-gray-200">
            <!-- 操作栏 -->
            <div class="p-4 border-b border-gray-200 flex justify-between items-center">
                <h2 class="text-lg font-semibold text-gray-800">文章列表</h2>
                <el-button type="primary" :icon="EditPen" class="px-4" @click="isArticlePublishEditorShow = true">
                    写文章
                </el-button>
            </div>

            <!-- 文章表格 -->
            <el-table v-loading="tableLoading" :data="tableData" style="width: 100%" :row-class-name="tableRowClassName">
                <el-table-column prop="title" label="标题" min-width="300">
                    <template #default="scope">
                        <div class="flex items-center gap-3">
                            <el-image v-if="scope.row.titleImage" :src="scope.row.titleImage" style="width: 60px; height: 40px; object-fit: cover; border-radius: 4px;" :lazy="true" />
                            <span class="font-medium text-gray-800">{{ scope.row.title }}</span>
                        </div>
                    </template>
                </el-table-column>
                <el-table-column prop="createTime" label="发布时间" width="180" />
                <el-table-column label="操作" width="200" fixed="right">
                    <template #default="scope">
                        <div class="flex gap-2">
                            <el-button size="small" :icon="Edit" type="primary" plain @click="showArticleUpdateEditorShow(scope.row)">
                                编辑
                            </el-button>
                            <el-button size="small" :icon="View" type="info" plain @click="previewArticle(scope.row)">
                                预览
                            </el-button>
                            <el-button size="small" :icon="Delete" type="danger" plain @click="deleteArticleSubmit(scope.row)">
                                删除
                            </el-button>
                        </div>
                    </template>
                </el-table-column>
            </el-table>

            <!-- 分页 -->
            <div class="p-4 border-t border-gray-200 flex justify-between items-center">
                <span class="text-sm text-gray-600">共 {{ total }} 条记录</span>
                <el-pagination
v-model:current-page="current" v-model:page-size="size" :page-sizes="[10, 20, 50]"
                    background layout="sizes, prev, pager, next" :total="total"
                    @size-change="handleSizeChange" @current-change="getTableData" />
            </div>
        </div>

        <!-- 写博客 -->
        <el-dialog v-model="isArticlePublishEditorShow" fullscreen :show-close="false" :modal="false" custom-class="article-editor-dialog">
            <template #header>
                <div class="bg-white border-b border-gray-200 p-4 fixed top-0 left-0 right-0 z-10">
                    <div class="flex justify-between items-center">
                        <h4 class="text-lg font-bold text-gray-800">写文章</h4>
                        <div class="flex gap-2">
                            <el-button @click="isArticlePublishEditorShow = false">取消</el-button>
                            <el-button type="primary" :icon="Promotion" @click="onSubmit">
                                发布
                            </el-button>
                        </div>
                    </div>
                </div>
            </template>
            <div class="pt-20">
                <el-form ref="publishArticleFormRef" :model="form" label-position="top" :size="large" :rules="rules" class="p-6">
                    <el-form-item label="标题" prop="title" class="mb-6">
                        <el-input v-model="form.title" autocomplete="off" size="large" maxlength="40" show-word-limit clearable placeholder="请输入文章标题" />
                    </el-form-item>
                    <div class="grid grid-cols-1 md:grid-cols-3 gap-6 mb-6">
                        <el-form-item label="分类" prop="categoryId" class="md:col-span-1">
                            <el-select v-model="form.categoryId" clearable placeholder="请选择分类" size="large" class="w-full">
                                <el-option v-for="item in categories" :key="item.value" :label="item.label" :value="item.value" />
                            </el-select>
                        </el-form-item>
                        <el-form-item label="标签" prop="tags" class="md:col-span-2">
                            <el-select
v-model="form.tags" multiple filterable remote reserve-keyword placeholder="请选择或输入标签"
                                remote-show-suffix :remote-method="remoteMethod" allow-create default-first-option
                                :loading="tagSelectLoading" size="large" class="w-full">
                                <el-option v-for="item in options" :key="item.value" :label="item.label" :value="item.value" />
                            </el-select>
                        </el-form-item>
                    </div>
                    <el-form-item label="摘要" prop="description" class="mb-6">
                        <el-input v-model="form.description" :rows="3" type="textarea" placeholder="请输入文章摘要" show-word-limit maxlength="200" />
                    </el-form-item>
                    <el-form-item label="封面" prop="titleImage" class="mb-6">
                        <el-upload
class="avatar-uploader border-2 border-dashed border-gray-300 rounded-lg p-4 text-center hover:border-primary transition-colors" action="#" :on-change="handleTitleImageChange" :auto-upload="false"
                            :show-file-list="false" :on-success="handleAvatarSuccess">
                            <img v-if="form.titleImage" :src="form.titleImage" class="avatar max-w-full h-auto rounded" loading="lazy" @error="(e) => e.target.src='data:image/svg+xml,%3Csvg xmlns=%22http://www.w3.org/2000/svg%22 viewBox=%220 0 200 120%22%3E%3Crect fill=%22%23f3f4f6%22 width=%22200%22 height=%22120%22/%3E%3Ctext x=%2250%25%22 y=%2250%25%22 dominant-baseline=%22middle%22 text-anchor=%22middle%22 fill=%22%239ca3af%22 font-family=%22sans-serif%22 font-size=%2214%22%3E加载失败%3C/text%3E%3C/svg%3E'" />
                            <div v-else class="flex flex-col items-center justify-center py-8">
                                <el-icon class="text-gray-400 mb-2" style="font-size: 32px;">
                                    <Plus />
                                </el-icon>
                                <span class="text-gray-500">点击上传封面图片</span>
                                <span class="text-xs text-gray-400 mt-1">建议尺寸：1200x630px</span>
                            </div>
                        </el-upload>
                    </el-form-item>
                    <el-form-item label="内容" prop="content" class="mb-6">
                        <MdEditor v-model="form.content" editor-id="publishArticleEditor" @onUploadImg="onUploadImg" />
                    </el-form-item>
                </el-form>
            </div>
        </el-dialog>

        <!-- 编辑博客 -->
        <el-dialog v-model="isArticleUpdateEditorShow" fullscreen :show-close="false" :modal="false" custom-class="article-editor-dialog">
            <template #header>
                <div class="bg-white border-b border-gray-200 p-4 fixed top-0 left-0 right-0 z-10">
                    <div class="flex justify-between items-center">
                        <h4 class="text-lg font-bold text-gray-800">编辑文章</h4>
                        <div class="flex gap-2">
                            <el-button @click="hideArticleUpdateEditor">取消</el-button>
                            <el-button type="primary" :icon="Promotion" @click="updateSubmit">
                                提交
                            </el-button>
                        </div>
                    </div>
                </div>
            </template>
            <div class="pt-20">
                <el-form ref="updateArticleFormRef" :model="form" label-position="top" :size="large" :rules="rules" class="p-6">
                    <el-form-item label="标题" prop="title" class="mb-6">
                        <el-input v-model="form.title" autocomplete="off" size="large" maxlength="40" show-word-limit clearable placeholder="请输入文章标题" />
                    </el-form-item>
                    <div class="grid grid-cols-1 md:grid-cols-3 gap-6 mb-6">
                        <el-form-item label="分类" prop="categoryId" class="md:col-span-1">
                            <el-select v-model="form.categoryId" clearable placeholder="请选择分类" size="large" class="w-full">
                                <el-option v-for="item in categories" :key="item.value" :label="item.label" :value="item.value" />
                            </el-select>
                        </el-form-item>
                        <el-form-item label="标签" prop="tags" class="md:col-span-2">
                            <el-select
v-model="form.tags" multiple filterable remote reserve-keyword placeholder="请选择或输入标签"
                                remote-show-suffix :remote-method="remoteMethod" allow-create default-first-option
                                :loading="tagSelectLoading" size="large" class="w-full">
                                <el-option v-for="item in options" :key="item.value" :label="item.label" :value="item.value" />
                            </el-select>
                        </el-form-item>
                    </div>
                    <el-form-item label="摘要" prop="description" class="mb-6">
                        <el-input v-model="form.description" :rows="3" type="textarea" placeholder="请输入文章摘要" show-word-limit maxlength="200" />
                    </el-form-item>
                    <el-form-item label="封面" prop="titleImage" class="mb-6">
                        <el-upload
class="avatar-uploader border-2 border-dashed border-gray-300 rounded-lg p-4 text-center hover:border-primary transition-colors" action="#" :on-change="handleTitleImageChange" :auto-upload="false"
                            :show-file-list="false" :on-success="handleAvatarSuccess">
                            <img v-if="form.titleImage" :src="form.titleImage" class="avatar max-w-full h-auto rounded" loading="lazy" @error="(e) => e.target.src='data:image/svg+xml,%3Csvg xmlns=%22http://www.w3.org/2000/svg%22 viewBox=%220 0 200 120%22%3E%3Crect fill=%22%23f3f4f6%22 width=%22200%22 height=%22120%22/%3E%3Ctext x=%2250%25%22 y=%2250%25%22 dominant-baseline=%22middle%22 text-anchor=%22middle%22 fill=%22%239ca3af%22 font-family=%22sans-serif%22 font-size=%2214%22%3E加载失败%3C/text%3E%3C/svg%3E'" />
                            <div v-else class="flex flex-col items-center justify-center py-8">
                                <el-icon class="text-gray-400 mb-2" style="font-size: 32px;">
                                    <Plus />
                                </el-icon>
                                <span class="text-gray-500">点击上传封面图片</span>
                                <span class="text-xs text-gray-400 mt-1">建议尺寸：1200x630px</span>
                            </div>
                        </el-upload>
                    </el-form-item>
                    <el-form-item label="内容" prop="content" class="mb-6">
                        <MdEditor v-model="form.content" editor-id="updateArticleEditor" @onUploadImg="onUploadImg" />
                    </el-form-item>
                </el-form>
            </div>
        </el-dialog>
    </div>
</template>

<script setup>
import { ElMessage, ElMessageBox } from 'element-plus'
import { ref, reactive, onUnmounted } from 'vue'
import { createArticle, getArticles, deleteArticle, getArticle, updateArticle } from '@/api/modules/article'
import { uploadFile } from '@/api/admin/file'
import { MdEditor } from 'md-editor-v3'
import 'md-editor-v3/lib/style.css'
import { showMessage } from '@/utils'
import { useRouter } from 'vue-router'
import { getCategorySelect } from '@/api/admin/category'
import { selectTags, getTagSelect } from '@/api/admin/tag'
import dayjs from '@/utils/dayjs'
import { Search, RefreshRight, Edit, View, Delete, EditPen, Promotion, Plus } from '@element-plus/icons-vue'
import logger from '@/utils/logger'
import { API_STATUS } from '@/composables/api'
const router = useRouter()

const isArticlePublishEditorShow = ref(false)
const isArticleUpdateEditorShow = ref(false)
const tableLoading = ref(false)

// 表格行样式
const tableRowClassName = ({ rowIndex }) => {
    return rowIndex % 2 === 0 ? 'bg-white' : 'bg-gray-50'
}

const searchTitle = ref('')
const pickDate = ref('')
const startDate = reactive({})
const endDate = reactive({})

const reset = () => {
    pickDate.value = ''
    startDate.value = null
    endDate.value = null
    searchTitle.value = ''
}

const datepickerChange = (e) => {
    startDate.value = dayjs(e[0]).format('YYYY-MM-DD HH:mm:ss')
    endDate.value = dayjs(e[1]).format('YYYY-MM-DD HH:mm:ss')
}

const shortcuts = [
    {
        text: '最近一周',
        value: () => {
            const end = new Date()
            const start = new Date()
            start.setTime(start.getTime() - 3600 * 1000 * 24 * 7)
            return [start, end]
        }
    },
    {
        text: '最近一个月',
        value: () => {
            const end = new Date()
            const start = new Date()
            start.setTime(start.getTime() - 3600 * 1000 * 24 * 30)
            return [start, end]
        }
    },
    {
        text: '最近三个月',
        value: () => {
            const end = new Date()
            const start = new Date()
            start.setTime(start.getTime() - 3600 * 1000 * 24 * 90)
            return [start, end]
        }
    }
]

const handleTitleImageChange = (file) => {
    logger.debug('开始上传文件')
    logger.debug(file)
    const formData = new FormData()
    formData.append('file', file.raw)
    uploadFile(formData).then((e) => {
        if (e.code !== 200) {
            const message = e.message
            showMessage(message, 'warning', 'message')
            return
        }
        form.titleImage = e.data.url
        showMessage('文章题图上传成功', 'success', 'message')
    })
}

const hideArticleUpdateEditor = () => {
    isArticleUpdateEditorShow.value = false
    form.title = ''
    form.content = '请输入内容'
    form.titleImage = ''
    form.categoryId = null
    form.tags = []
}

const showArticleUpdateEditorShow = (row) => {
    isArticleUpdateEditorShow.value = true
    const articleId = row.id
    getArticle(articleId).then((e) => {
        if (e.code === 200) {
            form.id = e.data.id
            form.title = e.data.title
            form.content = e.data.content
            form.titleImage = e.data.titleImage
            form.categoryId = e.data.categoryId
            form.tags = e.data.tagIds
            form.description = e.data.description
        }
    })
}

const onUploadImg = async (files, callback) => {
    await Promise.all(
        files.map((file) => {
            return new Promise(() => {
                logger.debug('==> 开始上传文件...')
                const formData = new FormData()
                formData.append('file', file)
                uploadFile(formData).then((res) => {
                    logger.debug(res)
                    logger.debug('访问路径：' + res.data.url)
                    callback([res.data.url])
                })
            })
        })
    )
}

const previewArticle = (row) => {
    // 打开一个新页面
    const routeData = router.resolve({ name: 'article', params: { id: String(row.id) } })
    window.open(routeData.href, '_blank')
}

const form = reactive({
    id: null,
    title: '',
    content: '请输入内容',
    titleImage: '',
    categoryId: null,
    tags: [],
    description: ''
})

const publishArticleFormRef = ref(null)
const updateArticleFormRef = ref(null)
const rules = {
    title: [
        { required: true, message: '请输入文章标题', trigger: 'blur' },
        { min: 1, max: 40, message: '文章标题要求大于1个字符，小于40个字符', trigger: 'blur' }
    ],
    content: [{ required: true }],
    titleImage: [{ required: true }],
    categoryId: [{ required: true, message: '请选择文章分类', trigger: 'blur' }],
    tags: [{ required: true, message: '请选择文章标签', trigger: 'blur' }],
    description: [{ required: true, message: '请输入文章摘要', trigger: 'blur' }]
}

// const handleMd = (md) => {
//     form.content = md
//     logger.debug('子组件回传过来的数据：' + form.content)
// }

const tableData = ref([])
// 当前页码
const current = ref(1)
const total = ref(0)
const size = ref(10)

// 获取分页数据
function getTableData() {
    logger.debug('获取分页数据')
    tableLoading.value = true
    getArticles({ current: current.value, size: size.value, startDate: startDate.value, endDate: endDate.value, searchTitle: searchTitle.value })
        .then((res) => {
            if (res.code === API_STATUS.SUCCESS) {
                tableData.value = res.data.list || []
                current.value = res.data.page
                total.value = res.data.total
                size.value = res.data.size
            }
        }).finally(() => {
            tableLoading.value = false
        })
}
getTableData()

const handleSizeChange = (e) => {
    logger.debug('选择的页码' + e)
    size.value = e
    getTableData()
}

const onSubmit = () => {
    isArticlePublishEditorShow.value = true
    logger.debug('提交内容' + form.content)
    publishArticleFormRef.value.validate((valid) => {
        if (!valid) {
            return false
        }
        createArticle(form).then((e) => {
        logger.debug(e)
        if (e.code !== 200) {
            const message = e.message
            showMessage(message, 'warning', 'message')
            return
        }

        showMessage('发布成功', 'success', 'message')
        isArticlePublishEditorShow.value = false
        location.reload()
    })
    })
}

const updateSubmit = () => {
    isArticleUpdateEditorShow.value = true
    logger.debug('提交内容' + form.content)
    updateArticleFormRef.value.validate((valid) => {
        if (!valid) {
            return false
        }
        updateArticle(form.id, form).then((e) => {
        logger.debug(e)
        if (e.code !== 200) {
            const message = e.message
            showMessage(message, 'warning', 'message')
            return
        }

        showMessage('修改成功', 'success', 'message')
        isArticleUpdateEditorShow.value = false
        location.reload()
    })
    })
}

const deleteArticleSubmit = (row) => {
    logger.debug(row.id)
    ElMessageBox.confirm(
        '是否确认要删除该文章?',
        '提示',
        {
            confirmButtonText: '确认',
            cancelButtonText: '取消',
            type: 'warning'
        }
    )
        .then(() => {
            deleteArticle(row.id).then((e) => {
                if (e.code === 200) {
                    showMessage('删除成功', 'success')
                    location.reload()
                } else {
                    const message = e.message
                    showMessage(message, 'warning')
                }
            })

        })
        .catch(() => {
            ElMessage({
                type: 'info',
                message: '删除失败'
            })
        })
}

// 文章分类
const categories = ref([])
getCategorySelect().then((e) => {
    logger.debug('获取分类数据')
    logger.debug(e)
    categories.value = e.data
})

// 文章标签
const tagSelectLoading = ref(false)
const options = ref([])
getTagSelect().then((e) => {
    logger.debug('获取标签数据')
    logger.debug(e)
    options.value = e.data
})

const remoteMethod = (query) => {
    logger.debug('远程搜索')
    logger.debug(options.value)
    if (query) {
        tagSelectLoading.value = true
        tagSelectTimer = setTimeout(() => {
            tagSelectLoading.value = false
            selectTags(query).then((e) => {
                if (e.code === 200) {
                    options.value = e.data
                }
            })
        }, 200)
    }
}

let tagSelectTimer = null

onUnmounted(() => {
    if (tagSelectTimer) {
        clearTimeout(tagSelectTimer)
        tagSelectTimer = null
    }
})
</script>

<style scoped>
.avatar-uploader .avatar {
    width: 278px;
    display: block;
}

.message {
    z-index: 9999 !important;
}
</style>

<style>
.w-50 {
    width: 12.5rem!important;
}

.mr-3 {
    margin-right: 0.75rem!important;
}

.avatar-uploader .el-upload {
    border: 1px dashed var(--el-border-color);
    border-radius: 6px;
    cursor: pointer;
    position: relative;
    overflow: hidden;
    transition: var(--el-transition-duration-fast);
}

.avatar-uploader .el-upload:hover {
    border-color: var(--el-color-primary);
}

.el-icon.avatar-uploader-icon {
    font-size: 28px;
    color: var(--text-muted);
    width: 178px;
    height: 178px;
    text-align: center;
}

.el-select--large {
    width: 600px;
}

</style>
