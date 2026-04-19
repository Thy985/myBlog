<template>
    <el-dropdown @command="handleThemeChange">
        <el-button :icon="Sunny" circle />
        <template #dropdown>
            <el-dropdown-menu>
                <el-dropdown-item 
                    v-for="(theme, key) in themes" 
                    :key="key"
                    :command="key"
                    :class="{ 'is-active': currentTheme === key }"
                >
                    <el-icon v-if="key === 'light'"><Sunny /></el-icon>
                    <el-icon v-else-if="key === 'dark'"><Moon /></el-icon>
                    <el-icon v-else><Brush /></el-icon>
                    <span class="ml-2">{{ theme.name }}</span>
                </el-dropdown-item>
            </el-dropdown-menu>
        </template>
    </el-dropdown>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Sunny, Moon, Brush } from '@element-plus/icons-vue'
import { themes, applyTheme, getCurrentTheme } from '@/utils/theme'

const currentTheme = ref('light')

const handleThemeChange = (themeName) => {
    applyTheme(themeName)
    currentTheme.value = themeName
}

onMounted(() => {
    currentTheme.value = getCurrentTheme()
})
</script>

<style scoped>
.ml-2 {
    margin-left: 8px;
}

.is-active {
    color: var(--el-color-primary);
    font-weight: 600;
}
</style>
