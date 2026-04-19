<template>
    <div v-if="plan" class="space-y-2">
        <div class="flex items-center justify-between text-xs">
            <span class="text-[var(--agent-text-muted)]">执行计划</span>
            <span class="text-[var(--agent-primary)] font-mono">{{ progress.current }}/{{ progress.total }}</span>
        </div>
        <div class="agent-progress">
            <div class="agent-progress__bar" :style="{ width: `${progress.percentage}%` }"></div>
        </div>
        <div class="space-y-1">
            <div
                v-for="(step, idx) in plan.steps"
                :key="step.id || idx"
                class="flex items-center gap-2 text-xs py-1"
            >
                <span :class="stepDotClass(idx)"></span>
                <span :class="stepTextClass(idx)">{{ step.name }}</span>
                <span v-if="step.description" class="text-[var(--agent-text-muted)] truncate">
                    - {{ step.description }}
                </span>
            </div>
        </div>
    </div>
</template>

<script setup>
const props = defineProps({
    plan: { type: Object, required: true },
    progress: { type: Object, default: () => ({ current: 0, total: 0, percentage: 0 }) }
})

function stepDotClass(idx) {
    if (idx < props.progress.current) {return 'w-1.5 h-1.5 rounded-full bg-[var(--agent-success)]'}
    if (idx === props.progress.current) {return 'w-1.5 h-1.5 rounded-full bg-[var(--agent-primary)] animate-pulse'}
    return 'w-1.5 h-1.5 rounded-full bg-[var(--agent-border)]'
}

function stepTextClass(idx) {
    if (idx < props.progress.current) {return 'text-[var(--agent-success)]'}
    if (idx === props.progress.current) {return 'text-[var(--agent-primary)] font-medium'}
    return 'text-[var(--agent-text-muted)]'
}
</script>
