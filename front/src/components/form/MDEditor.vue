<template>
    <div id="editor">
        <mavonEditor v-model="myContent" font-size="18px"  style="height: 100%;" @change="mdChange"></mavonEditor>
    </div>
</template>
<script>
// Local Registration
import { mavonEditor } from 'mavon-editor'
import { reactive, toRefs } from 'vue'
import 'mavon-editor/dist/css/index.css'
export default {
    name: 'Editor',
    components: {
        mavonEditor
        // or 'mavon-editor': mavonEditor
    },
    props: ['content'],
    setup(props, { emit }) {
        const state = reactive({
            myContent: ''
        })

        const mdChange = () => {
            emit('event', state.myContent)
        }

        return {
            ...toRefs(state),
            mdChange
        }
    }
}

</script>
<style>
#editor {
    margin: auto;
    width: 100%;
    height: 580px;
}
</style>
