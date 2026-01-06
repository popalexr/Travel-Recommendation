<script setup>
import { Button } from "@/components/ui/button"

const emit = defineEmits(["close", "upload", "preferences"])
</script>

<template>
  <div class="fixed inset-0 z-50 flex items-center justify-center bg-black/40 backdrop-blur-sm">
    <div class="w-full max-w-sm rounded-2xl border border-border bg-background p-6 shadow-xl">
      <div class="flex items-start justify-between gap-3">
        <div>
          <h2 class="text-base font-semibold text-foreground">Add to this trip</h2>
          <p class="mt-1 text-xs text-muted-foreground">
            Choose what you want to update for this chat.
          </p>
        </div>
        <button
          class="rounded-full p-1 text-muted-foreground hover:bg-muted"
          @click="emit('close')"
          aria-label="Close"
        >
          <svg viewBox="0 0 20 20" fill="currentColor" class="size-4">
            <path
              fill-rule="evenodd"
              d="M5.22 5.22a.75.75 0 011.06 0L10 8.94l3.72-3.72a.75.75 0 111.06 1.06L11.06 10l3.72 3.72a.75.75 0 11-1.06 1.06L10 11.06l-3.72 3.72a.75.75 0 11-1.06-1.06L8.94 10 5.22 6.28a.75.75 0 010-1.06z"
              clip-rule="evenodd"
            />
          </svg>
        </button>
      </div>

      <div class="mt-4 space-y-2">
        <Button variant="outline" class="w-full justify-start" @click="emit('upload')">
          Upload documents
        </Button>
        <Button variant="outline" class="w-full justify-start" @click="emit('preferences')">
          Trip preferences
        </Button>
      </div>
    </div>
  </div>
</template>
