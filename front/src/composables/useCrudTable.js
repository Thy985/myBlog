/**
 * CRUD表格Composable
 * 封装管理后台列表页的通用CRUD逻辑
 */
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { API_STATUS } from '@/composables/api'
import logger from '@/utils/logger'

export function useCrudTable(options = {}) {
  const {
    api // CRUD API对象 { getList, add, update, delete }
  } = options

  // 表格状态
  const tableData = ref([])
  const tableLoading = ref(false)
  const total = ref(0)
  const current = ref(1)
  const size = ref(10)

  // 搜索状态
  const searchKeyword = ref('')
  const pickDate = ref('')
  const startDate = reactive({ value: null })
  const endDate = reactive({ value: null })

  // 表单状态
  const form = reactive({})
  const formRef = ref(null)
  const submitLoading = ref(false)

  // 对话框状态
  const dialogVisible = ref(false)
  const dialogTitle = ref('')

  // 日期快捷选项
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

  // 重置搜索
  function reset() {
    pickDate.value = ''
    startDate.value = null
    endDate.value = null
    searchKeyword.value = ''
  }

  // 日期选择变化
  function datepickerChange(e) {
    if (e) {
      startDate.value = e[0]
      endDate.value = e[1]
    } else {
      startDate.value = null
      endDate.value = null
    }
  }

  // 请求ID，用于防止异步竞态
  let currentRequestId = 0

  // 获取列表数据
  async function fetchData() {
    if (!api?.getList) {return}

    tableLoading.value = true
    const requestId = ++currentRequestId
    try {
      const res = await api.getList({
        current: current.value,
        size: size.value,
        startDate: startDate.value,
        endDate: endDate.value,
        keyword: searchKeyword.value
      })

      // 如果这是旧的请求，忽略结果
      if (requestId !== currentRequestId) {return}

      if (res && res.code === API_STATUS.SUCCESS) {
        tableData.value = res.data?.records || res.data || []
        total.value = res.data?.total || 0
        current.value = res.data?.current || 1
        size.value = res.data?.size || 10
      }
    } catch (err) {
      if (requestId !== currentRequestId) {return}
      logger.error('获取数据失败:', err)
      ElMessage.error('获取数据失败，请稍后重试')
    } finally {
      if (requestId === currentRequestId) {
        tableLoading.value = false
      }
    }
  }

  // 提交表单（新增或更新）
  async function submitForm(submitApi, successMessage) {
    if (!formRef.value) {return false}

    try {
      await formRef.value.validate()
    } catch (err) {
      return false
    }

    submitLoading.value = true
    try {
      const res = await submitApi({ ...form })
      if (res && res.code === API_STATUS.SUCCESS) {
        ElMessage.success(successMessage)
        dialogVisible.value = false
        fetchData()
        return true
      } else {
        ElMessage.warning(res?.message || '操作失败')
        return false
      }
    } catch (err) {
      logger.error('提交失败:', err)
      ElMessage.error('操作失败，请稍后重试')
      return false
    } finally {
      submitLoading.value = false
    }
  }

  // 删除操作
  async function deleteItem(deleteApi, id, name, message) {
    const res = await deleteApi(id)
    if (res && res.code === API_STATUS.SUCCESS) {
      ElMessage.success(message || '删除成功')
      fetchData()
      return true
    } else {
      ElMessage.warning(res?.message || '删除失败')
      return false
    }
  }

  // 打开新增对话框
  function openAddDialog(title = '新增') {
    dialogTitle.value = title
    Object.keys(form).forEach(key => delete form[key])
    if (formRef.value) {
      formRef.value.resetFields()
    }
    dialogVisible.value = true
  }

  // 打开编辑对话框
  function openEditDialog(title, row, formData) {
    dialogTitle.value = title
    Object.assign(form, formData || row)
    if (formRef.value) {
      formRef.value.resetFields()
    }
    dialogVisible.value = true
  }

  // 分页大小变化
  function handleSizeChange(val) {
    size.value = val
    fetchData()
  }

  // 分页页码变化
  function handleCurrentChange() {
    fetchData()
  }

  return {
    // 状态
    tableData,
    tableLoading,
    total,
    current,
    size,
    searchKeyword,
    pickDate,
    startDate,
    endDate,
    form,
    formRef,
    submitLoading,
    dialogVisible,
    dialogTitle,
    shortcuts,
    // 方法
    reset,
    datepickerChange,
    fetchData,
    submitForm,
    deleteItem,
    openAddDialog,
    openEditDialog,
    handleSizeChange,
    handleCurrentChange
  }
}
