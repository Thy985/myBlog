<template>
  <el-card shadow="never" class="system-status-card border-1 rounded-xl">
    <template #header>
      <div class="flex justify-between items-center">
        <span class="font-semibold">系统状态</span>
        <span class="text-xs text-gray-500">{{ currentDateTime }}</span>
      </div>
    </template>
    <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
      <div class="status-item p-4 bg-gray-50 rounded-lg">
        <div class="flex justify-between items-center mb-2">
          <span class="text-sm font-medium text-gray-700">服务器状态</span>
          <span class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-green-100 text-green-800">
            正常
          </span>
        </div>
        <div class="w-full bg-gray-200 rounded-full h-2.5">
          <div class="bg-green-600 h-2.5 rounded-full" style="width: 25%"></div>
        </div>
        <div class="flex justify-between mt-1 text-xs text-gray-500">
          <span>CPU 使用率: 25%</span>
          <span>内存: 4GB / 16GB</span>
        </div>
      </div>
      <div class="status-item p-4 bg-gray-50 rounded-lg">
        <div class="flex justify-between items-center mb-2">
          <span class="text-sm font-medium text-gray-700">数据库状态</span>
          <span class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-green-100 text-green-800">
            正常
          </span>
        </div>
        <div class="w-full bg-gray-200 rounded-full h-2.5">
          <div class="bg-blue-600 h-2.5 rounded-full" style="width: 45%"></div>
        </div>
        <div class="flex justify-between mt-1 text-xs text-gray-500">
          <span>连接数: 12</span>
          <span>存储空间: 15GB / 50GB</span>
        </div>
      </div>
      <div class="status-item p-4 bg-gray-50 rounded-lg">
        <div class="flex justify-between items-center mb-2">
          <span class="text-sm font-medium text-gray-700">API 响应</span>
          <span class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-green-100 text-green-800">
            正常
          </span>
        </div>
        <div class="w-full bg-gray-200 rounded-full h-2.5">
          <div class="bg-purple-600 h-2.5 rounded-full" style="width: 15%"></div>
        </div>
        <div class="flex justify-between mt-1 text-xs text-gray-500">
          <span>平均响应: 120ms</span>
          <span>请求数: 456/小时</span>
        </div>
      </div>
    </div>
  </el-card>
</template>

<script setup>
defineProps({
  currentDateTime: {
    type: String,
    default: ''
  }
})
</script>

<style scoped>
.system-status-card {
  background-color: white;
}

.status-item {
  background-color: #f9fafb;
  border-radius: 8px;
  transition: background-color $1s ease, border-color $1s ease, color $1s ease, box-shadow $1s ease$2
}

.status-item:hover {
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.05);
}

@media (max-width: 768px) {
  .status-item {
    margin-bottom: 16px;
  }
}

.dark .system-status-card {
  background-color: #1e293b;
  border-color: #334155;
}

.dark .status-item {
  background-color: #334155;
}

.dark .status-item .text-gray-700 {
  color: #e2e8f0;
}

.dark .status-item .text-gray-500 {
  color: #94a3b8;
}

.dark .status-item .bg-gray-50 {
  background-color: #334155;
}

.dark .status-item .bg-gray-200 {
  background-color: #475569;
}
</style>
