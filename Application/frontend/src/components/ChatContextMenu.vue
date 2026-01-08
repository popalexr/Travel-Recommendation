<script setup>
import { ref, watch, nextTick } from "vue"

const props = defineProps({
  open: {
    type: Boolean,
    default: false,
  },
  chat: {
    type: Object,
    default: null,
  },
  position: {
    type: Object,
    default: () => ({ x: 0, y: 0 }),
  },
  isDeleting: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(["delete"])
const menuRef = ref(null)
const menuStyle = ref({ left: "0px", top: "0px" })

function clampPosition() {
  const padding = 8
  const pos = props.position ?? { x: 0, y: 0 }
  const rect = menuRef.value?.getBoundingClientRect()
  const width = rect?.width ?? 0
  const height = rect?.height ?? 0
  const maxX = window.innerWidth - width - padding
  const maxY = window.innerHeight - height - padding

  const clampedX = Math.max(padding, Math.min(pos.x ?? 0, maxX))
  const clampedY = Math.max(padding, Math.min(pos.y ?? 0, maxY))

  menuStyle.value = {
    left: `${clampedX}px`,
    top: `${clampedY}px`,
  }
}

watch(
  () => [props.open, props.position?.x, props.position?.y],
  () => {
    if (!props.open) return
    nextTick(() => {
      clampPosition()
    })
  },
  { immediate: true }
)

function handleDelete() {
  emit("delete", props.chat)
}
</script>

<template>
  <div
    v-if="open"
    ref="menuRef"
    class="fixed z-50 w-44 rounded-xl border border-border/50 bg-background/95 shadow-lg backdrop-blur"
    :style="menuStyle"
    @click.stop
    @contextmenu.prevent
  >
    <button
      class="w-full px-4 py-2 text-left text-sm text-destructive hover:bg-destructive/10 disabled:cursor-not-allowed disabled:opacity-60"
      :disabled="isDeleting"
      @click="handleDelete"
    >
      {{ isDeleting ? "Deleting..." : "Delete" }}
    </button>
  </div>
</template>
