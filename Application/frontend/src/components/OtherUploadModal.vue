<script setup>
import { ref } from "vue"

const props = defineProps({
  chatId: {
    type: [Number, String],
    default: null,
  },
})

const emit = defineEmits(["close", "uploaded"])

const isOtherDragActive = ref(false)
const otherFiles = ref([])
const otherFileInput = ref(null)
const uploadError = ref("")
const isUploading = ref(false)

function openOtherFileDialog() {
  if (otherFileInput.value) {
    otherFileInput.value.click()
  }
}

function addOtherFiles(files) {
  if (!files || !files.length) return
  const accepted = Array.from(files).filter((file) => {
    if (!file || !file.type) return false
    return file.type.startsWith("image/") || file.type === "application/pdf"
  })
  if (!accepted.length) return
  otherFiles.value = [...otherFiles.value, ...accepted]
}

function handleOtherFileChange(event) {
  const files = event?.target?.files
  addOtherFiles(files)
}

function handleOtherDrop(event) {
  const files = event?.dataTransfer?.files
  addOtherFiles(files)
  isOtherDragActive.value = false
}

function removeOtherFile(index) {
  if (index < 0 || index >= otherFiles.value.length) return
  otherFiles.value.splice(index, 1)
}

async function submitOtherDocument() {
  if (!otherFiles.value.length || isUploading.value) return

  uploadError.value = ""
  isUploading.value = true

  const formData = new FormData()
  formData.append("file", otherFiles.value[0])
  if (props.chatId) {
    formData.append("chatId", props.chatId)
  }

  try {
    const response = await fetch("/api/chat/upload-document", {
      method: "POST",
      body: formData,
      credentials: "same-origin",
    })

    const payload = await response.json().catch(() => ({}))
    if (!response.ok) {
      uploadError.value = payload?.error ?? "Unable to process document."
      return
    }

    emit("uploaded", payload)
  } catch (err) {
    uploadError.value = "Unexpected error while uploading. Please try again."
  } finally {
    isUploading.value = false
  }
}
</script>

<template>
  <div class="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm">
    <div
      class="w-full max-w-lg rounded-3xl border border-border bg-gradient-to-b from-background/95 to-muted/80 p-6 shadow-2xl"
    >
      <div class="flex items-start justify-between gap-3">
        <div>
          <h2 class="text-base font-semibold text-foreground">Upload other document</h2>
          <p class="mt-1 text-xs text-muted-foreground">
            Drag and drop your document here, or browse files from your device.
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

      <div class="mt-5 space-y-4">
        <div
          class="relative flex cursor-pointer flex-col items-center justify-center rounded-2xl border-2 border-dashed px-6 py-10 text-center transition"
          :class="[
            isOtherDragActive
              ? 'border-primary bg-primary/5'
              : 'border-border/70 bg-muted/40 hover:border-primary/60 hover:bg-muted/60'
          ]"
          @click="openOtherFileDialog"
          @dragover.prevent="isOtherDragActive = true"
          @dragleave.prevent="isOtherDragActive = false"
          @drop.prevent="handleOtherDrop"
        >
          <div class="mb-3 flex items-center justify-center rounded-full bg-primary/10 p-3 text-primary">
            <svg viewBox="0 0 24 24" fill="none" class="h-6 w-6">
              <path
                d="M7 10.5L12 5.5L17 10.5M12 6V16.5"
                stroke="currentColor"
                stroke-width="1.5"
                stroke-linecap="round"
                stroke-linejoin="round"
              />
              <path
                d="M6 18.5C4.067 18.5 2.5 16.933 2.5 15C2.5 13.067 4.067 11.5 6 11.5H7M18 11.5C19.933 11.5 21.5 13.067 21.5 15C21.5 16.933 19.933 18.5 18 18.5H13"
                stroke="currentColor"
                stroke-width="1.5"
                stroke-linecap="round"
                stroke-linejoin="round"
              />
            </svg>
          </div>
          <p class="text-sm font-medium text-foreground">Drop your document PDF or image here</p>
          <p class="mt-1 text-xs text-muted-foreground">
            Supports PDF and image files. Max 10MB per file.
          </p>
          <button
            type="button"
            class="mt-4 rounded-full bg-primary px-4 py-1.5 text-xs font-medium text-primary-foreground shadow hover:bg-primary/90"
          >
            Browse files
          </button>
          <input
            ref="otherFileInput"
            type="file"
            class="hidden"
            multiple
            accept="image/*,application/pdf"
            @change="handleOtherFileChange"
          />
        </div>

        <div
          v-if="otherFiles.length"
          class="max-h-40 space-y-2 overflow-y-auto rounded-xl border border-border/70 bg-background/70 px-3 py-2"
        >
          <p class="mb-1 text-[11px] font-medium uppercase tracking-[0.2em] text-muted-foreground">
            Selected files
          </p>
          <div
            v-for="(file, index) in otherFiles"
            :key="index"
            class="flex items-center justify-between rounded-lg bg-muted/60 px-3 py-2 text-xs text-foreground"
          >
            <div class="flex min-w-0 flex-1 items-center gap-2">
              <div
                class="flex size-6 items-center justify-center rounded-full bg-background/80 text-[10px] text-muted-foreground"
              >
                {{ index + 1 }}
              </div>
              <div class="min-w-0">
                <p class="truncate font-medium">{{ file.name }}</p>
                <p class="text-[11px] text-muted-foreground">
                  {{ (file.size / 1024 / 1024).toFixed(2) }} MB
                </p>
              </div>
            </div>
            <button
              type="button"
              class="ml-3 rounded-full p-1 text-[10px] text-muted-foreground hover:bg-background/70"
              @click="removeOtherFile(index)"
              aria-label="Remove file"
            >
              ?
            </button>
          </div>
        </div>
        <div v-else class="rounded-xl bg-muted/40 px-3 py-2 text-[11px] text-muted-foreground">
          No files selected yet.
        </div>
      </div>

      <p class="mt-3 text-[11px] text-muted-foreground">
        The first selected file will be analyzed. Supports PDFs or images up to 10MB.
      </p>

      <p v-if="uploadError" class="mt-2 text-xs text-destructive">
        {{ uploadError }}
      </p>

      <div class="mt-5 flex items-center justify-end gap-3">
        <button
          class="rounded-full px-3 py-1.5 text-xs font-medium text-muted-foreground hover:bg-muted"
          @click="emit('close')"
        >
          Cancel
        </button>
        <button
          class="rounded-full bg-primary px-4 py-1.5 text-xs font-medium text-primary-foreground shadow hover:bg-primary/90 disabled:cursor-not-allowed disabled:opacity-60"
          :disabled="!otherFiles.length || isUploading"
          @click="submitOtherDocument"
        >
          {{ isUploading ? "Sending..." : "Analyze document" }}
        </button>
      </div>
    </div>
  </div>
</template>
