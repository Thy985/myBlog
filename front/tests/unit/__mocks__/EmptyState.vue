<template>
  <div class="empty-state"><slot></slot></div>
</template>

<script>
export default {
  name: 'EmptyState',
  props: ['icon', 'title', 'description', 'actionText', 'showTip', 'compact', 'secondaryActionText', 'showAction'],
  emits: ['action', 'secondary-action']
}
</script>
