<template>
  <!-- 桌面端导航 -->
  <div class="hidden md:flex md:items-center md:justify-center py-3 space-x-6 lg:space-x-10">
    <router-link
      to="/"
      class="nav-link"
      active-class="nav-link-active"
      :aria-current="$route.path === '/' ? 'page' : undefined"
    >
      首页
    </router-link>
    <router-link
      to="/category"
      class="nav-link"
      active-class="nav-link-active"
      :aria-current="$route.path === '/category' ? 'page' : undefined"
    >
      分类
    </router-link>
    <router-link
      to="/tag"
      class="nav-link"
      active-class="nav-link-active"
      :aria-current="$route.path === '/tag' ? 'page' : undefined"
    >
      标签
    </router-link>
    <router-link
      to="/archive"
      class="nav-link"
      active-class="nav-link-active"
      :aria-current="$route.path === '/archive' ? 'page' : undefined"
    >
      归档
    </router-link>
  </div>
</template>

<script setup>
</script>

<style scoped>
.nav-link {
  position: relative;
  padding: 0.25rem 0.5rem;
  color: var(--text-secondary);
  font-weight: 500;
  transition: color 0.2s ease-in-out;
}

.nav-link:hover {
  color: var(--primary-color);
}

.nav-link-active {
  color: var(--primary-color);
}

.nav-link-active::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 1.5rem;
  height: 0.125rem;
  background-color: var(--primary-color);
  border-radius: 9999px;
  transition: width 0.3s ease, background-color 0.3s ease;
}
</style>
