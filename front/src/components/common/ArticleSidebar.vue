<template>
    <div class="sticky top-24 space-y-6">
        <UserInfoCard></UserInfoCard>

        <!-- 文章分类 -->
        <CategoryList
            :categories="categories"
            :show-count="true"
            @category-click="$emit('category-click', $event)"
        />

        <!-- 文章标签 -->
        <TagCloud
            :tags="tags"
            :cloud-mode="true"
            @tag-click="$emit('tag-click', $event)"
        />

        <!-- 相关推荐 -->
        <RelatedArticles
            :articles="relatedArticles"
            :loading="loadingRelated"
            @article-click="$emit('article-click', $event)"
        />
    </div>
</template>

<script setup>
import UserInfoCard from './UserInfoCard.vue'
import CategoryList from './CategoryList.vue'
import TagCloud from './TagCloud.vue'
import RelatedArticles from './RelatedArticles.vue'

defineProps({
    categories: {
        type: Array,
        default: () => []
    },
    tags: {
        type: Array,
        default: () => []
    },
    relatedArticles: {
        type: Array,
        default: () => []
    },
    loadingRelated: {
        type: Boolean,
        default: false
    }
})

defineEmits(['category-click', 'tag-click', 'article-click'])
</script>
