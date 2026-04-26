import { ref, reactive } from 'vue'
import type { FormInstance } from 'element-plus'
import { ElMessage } from 'element-plus'
import { API_STATUS } from '@/composables/api'
import logger from '@/utils/logger'

export interface CrudApi {
  getList?: (params: any) => Promise<any>
  add?: (data: any) => Promise<any>
  update?: (data: any) => Promise<any>
  delete?: (id: number) => Promise<any>
}

export interface UseCrudTableOptions {
  api: CrudApi
}

export function useCrudTable(options: UseCrudTableOptions) {
  const { api } = options

  const tableData = ref<any[]>([])
  const tableLoading = ref(false)
  const total = ref(0)
  const current = ref(1)
  const size = ref(10)

  const searchKeyword = ref('')
  const pickDate = ref('')
  const startDate = reactive({ value: Date | null })
  const endDate = reactive({ value: Date | null })

  const form = reactive<Record<string, any>>({})
  const formRef = ref<FormInstance | null>(null)
  const submitLoading = ref(false)

  const dialogVisible = ref(false)
  const dialogTitle = ref('')

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

  function reset(): void {
    pickDate.value = ''
    startDate.value = null
    endDate.value = null
    searchKeyword.value = ''
  }

  function datepickerChange(e: Date[] | null): void {
    if (e && e.length === 2) {
      startDate.value = e[0]
      endDate.value = e[1]
    } else {
      startDate.value = null
      endDate.value = null
    }
  }

  let currentRequestId = 0

  async function fetchData(): Promise<void> {
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

      if (requestId !== currentRequestId) {return}

      if (res && res.code === API_STATUS.SUCCESS) {
        tableData.value = res.data?.records || res.data || []
        total.value = res.data?.total || 0
        current.value = res.data?.current || 1
        size.value = res.data?.size || 10
      }
    } catch (err: any) {
      if (requestId !== currentRequestId) {return}
      logger.error('获取数据失败:', err)
      ElMessage.error('获取数据失败，请稍后重试')
    } finally {
      if (requestId === currentRequestId) {
        tableLoading.value = false
      }
    }
  }

  async function submitForm(submitApi: (data: any) => Promise<any>, successMessage: string): Promise<boolean> {
    if (!formRef.value) {return false}

    try {
      await formRef.value.validate()
    } catch {
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
    } catch (err: any) {
      logger.error('提交失败:', err)
      ElMessage.error('操作失败，请稍后重试')
      return false
    } finally {
      submitLoading.value = false
    }
  }

  async function deleteItem(deleteApi: (id: number) => Promise<any>, id: number, _name?: string, message?: string): Promise<boolean> {
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

  function openAddDialog(title = '新增'): void {
    dialogTitle.value = title
    Object.keys(form).forEach(key => delete form[key])
    if (formRef.value) {
      formRef.value.resetFields()
    }
    dialogVisible.value = true
  }

  function openEditDialog(title: string, row: any, formData?: Record<string, any>): void {
    dialogTitle.value = title
    Object.assign(form, formData || row)
    if (formRef.value) {
      formRef.value.resetFields()
    }
    dialogVisible.value = true
  }

  function handleSizeChange(val: number): void {
    size.value = val
    fetchData()
  }

  function handleCurrentChange(): void {
    fetchData()
  }

  return {
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