<script setup>
const props = defineProps({
  title: {
    type: String,
    default: "Travel Recommendation",
  },
});
</script>

<template>
  <div class="min-h-screen bg-background text-foreground">
    <slot />
  </div>
</template>
