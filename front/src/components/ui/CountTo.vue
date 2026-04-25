<template>
    <div class="font-bold text-xl">{{ d.num.toFixed(0) }}</div>
</template>

<script setup>
import { reactive, watch, onUnmounted } from 'vue'
import gsap from 'gsap'

const props = defineProps({
    value: {
        type: Number,
        default: 0
    }
})

const d = reactive({
    num: 0
})

let animationRef = null

function animateToValue() {
    if (animationRef) {
        animationRef.kill()
    }
    animationRef = gsap.to(d, {
        duration: 0.5,
        num: props.value
    })
}
animateToValue()

watch(() => props.value, () => animateToValue())

onUnmounted(() => {
    if (animationRef) {
        animationRef.kill()
    }
})
</script>
