<template>
    <div id="editor">
        <mavonEditor
            v-model="myContent"
            font-size="18px"
            style="height: 100%;"
            @change="handleChange"
        />
    </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { mavonEditor } from 'mavon-editor'
import 'mavon-editor/dist/css/index.css'

const props = defineProps({
    content: {
        type: String,
        default: ''
    }
})

const emit = defineEmits(['event'])

const myContent = ref('')

const handleChange = (value) => {
    emit('event', value)
}

watch(() => props.content, (newVal) => {
    if (newVal !== myContent.value) {
        myContent.value = newVal || ''
    }
}, { immediate: true })
</script>

<style>
#editor {
    margin: auto;
    width: 100%;
    height: 580px;
}
</style>
