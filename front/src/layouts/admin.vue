<template>
    <el-container>
        <!-- 桌面端侧边栏 -->
        <el-aside v-if="!isMobile" :width="store.menuWidth" class="hidden md:block">
            <AdminMenu ref="adminMenuRef"></AdminMenu>
        </el-aside>

        <!-- 移动端侧边栏（通过汉堡菜单触发） -->
        <AdminMenu
            v-else
            ref="adminMenuRef"
            @mobile-menu-toggle="isMobileMenuOpen = !isMobileMenuOpen"
        ></AdminMenu>

        <el-container>
            <el-header class="flex items-center">
                <!-- 移动端汉堡菜单按钮 -->
                <button
                    v-if="isMobile"
                    class="mr-4 p-2 rounded-lg text-gray-600 hover:text-gray-900 hover:bg-gray-100 transition-all duration-200"
                    @click="toggleMobileMenu"
                >
                    <Bars3Icon class="w-6 h-6" />
                </button>
                <AdminHeader></AdminHeader>
            </el-header>

            <el-main>
                <AdminTagList></AdminTagList>
                <router-view v-slot="{ Component }">
                    <keep-alive :max="10">
                        <component :is="Component"></component>
                    </keep-alive>
                </router-view>
            </el-main>

            <el-footer>
                <AdminFooter></AdminFooter>
            </el-footer>
        </el-container>
    </el-container>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useMainStore } from '@/stores'
import { Bars3Icon } from '@heroicons/vue/24/outline'
import AdminHeader from '@/layouts/components/AdminHeader.vue'
import AdminMenu from '@/layouts/components/AdminMenu.vue'
import AdminTagList from '@/layouts/components/AdminTagList.vue'
import AdminFooter from '@/layouts/components/AdminFooter.vue'

const store = useMainStore()
const adminMenuRef = ref(null)
const isMobile = ref(false)
const isMobileMenuOpen = ref(false)

// 检测是否为移动端
const checkMobile = () => {
    isMobile.value = window.innerWidth <= 768
}

// 切换移动端菜单
const toggleMobileMenu = () => {
    if (adminMenuRef.value) {
        adminMenuRef.value.openMobileMenu()
    }
}

onMounted(() => {
    checkMobile()
    window.addEventListener('resize', checkMobile)
})

onUnmounted(() => {
    window.removeEventListener('resize', checkMobile)
})
</script>

<style scoped>
.el-aside {
    transition: all 0.3s;
}

.el-header {
    padding: 0;
}

.el-footer {
  padding: 0 !important;
}

/* 移动端适配 */
@media (max-width: 768px) {
    .el-aside {
        display: none;
    }
}
</style>
