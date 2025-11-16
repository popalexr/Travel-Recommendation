<script setup>
import { ref, computed, watch, onMounted, nextTick } from "vue"
import { Button } from "@/components/ui/button"
import UploadAttachmentModal from "@/components/UploadAttachmentModal.vue"
import TicketUploadModal from "@/components/TicketUploadModal.vue"
import AccommodationUploadModal from "@/components/AccommodationUploadModal.vue"
import OtherUploadModal from "@/components/OtherUploadModal.vue"

const props = defineProps({
  user: {
    type: Object,
    default: () => ({}),
  },
})

const isLoggingOut = ref(false)
const error = ref("")
const accountMenuOpen = ref(false)
const activeThread = ref(null)
const previousRecommendations = ref([])
const chatMessages = ref([])
const currentChatId = ref(null)
const userInput = ref("")
const isSendingMessage = ref(false)
const isUploadModalOpen = ref(false)
const isTicketUploadModalOpen = ref(false)
const isTicketDragActive = ref(false)
const ticketFiles = ref([])
const ticketFileInput = ref(null)
const isAccommodationUploadModalOpen = ref(false)
const isAccommodationDragActive = ref(false)
const accommodationFiles = ref([])
const accommodationFileInput = ref(null)
const isOtherUploadModalOpen = ref(false)
const isOtherDragActive = ref(false)
const otherFiles = ref([])
const otherFileInput = ref(null)
const dataError = ref("")
const isLoadingData = ref(true)
const canSendMessage = computed(() => userInput.value.trim().length > 0 && !isSendingMessage.value)
const messageInputRef = ref(null)
const messagesScrollRef = ref(null)
const messageItemsRef = ref([])
const activeChatTitle = computed(() => {
  const id = activeThread.value ?? currentChatId.value
  if (!id) return ""
  const rec = previousRecommendations.value.find((item) => item.id === id)
  if (!rec || !rec.title) return ""
  const title = String(rec.title).trim()
  return title.length ? title : ""
})

const displayName = computed(() => {
  const { firstName, lastName, email } = props.user ?? {}
  if (firstName || lastName) {
    const full = [firstName, lastName].filter(Boolean).join(" ").trim()
    return full || "Traveler"
  }
  return email ?? "Traveler"
})

watch(
  previousRecommendations,
  (items) => {
    if (!items || items.length === 0) {
      activeThread.value = null
      return
    }
    const match = items.find((item) => item.id === activeThread.value)
    if (!match) {
      activeThread.value = items[0]?.id ?? null
    }
  },
  { immediate: true }
)

async function loadDashboardData() {
  isLoadingData.value = true
  dataError.value = ""
  try {
    const response = await fetch("/api/dashboard", {
      credentials: "same-origin",
    })
    if (!response.ok) {
      dataError.value = "Unable to fetch dashboard data."
      return
    }
    const payload = await response.json().catch(() => ({}))
    const recs = Array.isArray(payload.previousRecommendations) ? payload.previousRecommendations : []
    previousRecommendations.value = recs
    chatMessages.value = Array.isArray(payload.chatMessages) ? payload.chatMessages : []
    if (recs.length > 0 && !activeThread.value) {
      activeThread.value = recs[0].id
      currentChatId.value = recs[0].id
    }
  } catch (err) {
    dataError.value = "Unexpected error loading dashboard."
  } finally {
    isLoadingData.value = false
  }
}

onMounted(() => {
  loadDashboardData()
  nextTick(() => {
    if (messageInputRef.value) {
      autoResizeMessage()
    }
  })
})

function toggleAccountMenu() {
  accountMenuOpen.value = !accountMenuOpen.value
}

function closeAccountMenu() {
  accountMenuOpen.value = false
}

function goToSettings() {
  closeAccountMenu()
  window.location.href = "/settings"
}

async function logout() {
  if (isLoggingOut.value) return
  error.value = ""
  isLoggingOut.value = true
  try {
    const response = await fetch("/logout", {
      method: "POST",
      credentials: "same-origin",
    })
    const payload = await response.json().catch(() => ({}))
    if (!response.ok) {
      error.value = payload?.message ?? "Unable to log out."
      return
    }
    window.location.href = "/"
  } catch (err) {
    error.value = "Unexpected error. Please try again."
  } finally {
    isLoggingOut.value = false
  }
}

function autoResizeMessage() {
  const el = messageInputRef.value
  if (!el) return
  const minHeight = 36
  const maxHeight = 128
  el.style.height = "auto"
  const newHeight = Math.min(Math.max(el.scrollHeight, minHeight), maxHeight)
  el.style.height = `${newHeight}px`
}

async function selectChat(rec) {
  if (!rec || !rec.id) return
  activeThread.value = rec.id
  currentChatId.value = rec.id
  dataError.value = ""
  isLoadingData.value = true
  try {
    const response = await fetch(`/api/chat/${rec.id}/messages`, {
      credentials: "same-origin",
    })
    if (!response.ok) {
      dataError.value = "Unable to load chat messages."
      return
    }
    const payload = await response.json().catch(() => ({}))
    chatMessages.value = Array.isArray(payload.messages) ? payload.messages : []
  } catch (err) {
    dataError.value = "Unexpected error loading chat messages."
  } finally {
    isLoadingData.value = false
  }
}

async function sendChatMessage() {
  const text = userInput.value.trim()
  if (!text || isSendingMessage.value) return

  // Clear input immediately after sending
  userInput.value = ""
  nextTick(() => autoResizeMessage())

  error.value = ""
  isSendingMessage.value = true

  const timestamp = new Date().toISOString()
  const localUserMessage = {
    id: `user-${Date.now()}`,
    role: "user",
    content: text,
    timestamp,
  }

  chatMessages.value = [...chatMessages.value, localUserMessage]

  try {
    const response = await fetch("/api/chat", {
      method: "POST",
      credentials: "same-origin",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        message: text,
        chatId: currentChatId.value,
      }),
    })

    const payload = await response.json().catch(() => ({}))

    if (!response.ok) {
      error.value = payload?.error ?? "Unable to get a recommendation."
      return
    }

    if (payload && payload.message) {
      chatMessages.value = [...chatMessages.value, payload.message]
    }

    if (!currentChatId.value && payload && payload.chatId) {
      currentChatId.value = payload.chatId
      activeThread.value = payload.chatId
      previousRecommendations.value = [
        {
          id: payload.chatId,
          title: payload.chatTitle || "Untitled chat",
          subtitle: "AI travel recommendations",
        },
        ...previousRecommendations.value,
      ]
    }
  } catch (err) {
    error.value = "Unexpected error contacting the recommendation engine."
  } finally {
    isSendingMessage.value = false
  }
}

function scrollToBottom() {
  nextTick(() => {
    const container = messagesScrollRef.value
    if (container) {
      container.scrollTop = container.scrollHeight
      return
    }

    const items = messageItemsRef.value
    if (items && items.length) {
      const last = items[items.length - 1]
      if (last && typeof last.scrollIntoView === "function") {
        last.scrollIntoView({ behavior: "smooth", block: "end" })
      }
    }
  })
}

watch(
  chatMessages,
  () => {
    scrollToBottom()
  },
  { deep: true }
)

function formatTimestamp(raw) {
  if (!raw) return ""
  const date = new Date(raw)
  if (Number.isNaN(date.getTime())) return ""
  return date.toLocaleString(undefined, {
    year: "numeric",
    month: "short",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  })
}

function openUploadModal() {
  isUploadModalOpen.value = true
}

function closeUploadModal() {
  isUploadModalOpen.value = false
}

function openTicketUploadModal() {
  isUploadModalOpen.value = false
  isTicketUploadModalOpen.value = true
}

function closeTicketUploadModal() {
  isTicketUploadModalOpen.value = false
}

function openAccommodationUploadModal() {
  isUploadModalOpen.value = false
  isAccommodationUploadModalOpen.value = true
}

function closeAccommodationUploadModal() {
  isAccommodationUploadModalOpen.value = false
}

function openOtherUploadModal() {
  isUploadModalOpen.value = false
  isOtherUploadModalOpen.value = true
}

function closeOtherUploadModal() {
  isOtherUploadModalOpen.value = false
}

function openTicketFileDialog() {
  if (ticketFileInput.value) {
    ticketFileInput.value.click()
  }
}

function addTicketFiles(files) {
  if (!files || !files.length) return
  const accepted = Array.from(files).filter((file) => {
    if (!file || !file.type) return false
    return file.type.startsWith("image/") || file.type === "application/pdf"
  })
  if (!accepted.length) return
  ticketFiles.value = [...ticketFiles.value, ...accepted]
}

function handleTicketFileChange(event) {
  const files = event?.target?.files
  addTicketFiles(files)
}

function handleTicketDrop(event) {
  const files = event?.dataTransfer?.files
  addTicketFiles(files)
  isTicketDragActive.value = false
}

function removeTicketFile(index) {
  if (index < 0 || index >= ticketFiles.value.length) return
  ticketFiles.value.splice(index, 1)
}

function openAccommodationFileDialog() {
  if (accommodationFileInput.value) {
    accommodationFileInput.value.click()
  }
}

function addAccommodationFiles(files) {
  if (!files || !files.length) return
  const accepted = Array.from(files).filter((file) => {
    if (!file || !file.type) return false
    return file.type.startsWith("image/") || file.type === "application/pdf"
  })
  if (!accepted.length) return
  accommodationFiles.value = [...accommodationFiles.value, ...accepted]
}

function handleAccommodationFileChange(event) {
  const files = event?.target?.files
  addAccommodationFiles(files)
}

function handleAccommodationDrop(event) {
  const files = event?.dataTransfer?.files
  addAccommodationFiles(files)
  isAccommodationDragActive.value = false
}

function removeAccommodationFile(index) {
  if (index < 0 || index >= accommodationFiles.value.length) return
  accommodationFiles.value.splice(index, 1)
}

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
</script>

<template>
  <div class="flex h-screen bg-background text-foreground">
    <aside class="hidden h-full w-72 flex-col border-r border-border/60 bg-muted/30 px-4 py-6 md:flex">
      <div>
        <p class="text-xs uppercase tracking-[0.4em] text-muted-foreground">Trips</p>
        <h2 class="mt-2 font-semibold text-foreground">Previous recommendations</h2>
        <button
          class="mt-3 inline-flex items-center rounded-full border border-border/70 bg-background/80 px-3 py-1 text-xs font-medium text-foreground shadow-sm transition hover:bg-muted"
          @click="() => { activeThread = null; currentChatId = null; chatMessages = []; userInput = '' }"
        >
          + New chat
        </button>
      </div>
      <nav class="mt-6 flex-1 space-y-2 overflow-y-auto pr-2">
        <template v-if="isLoadingData">
          <div class="h-14 animate-pulse rounded-xl bg-muted/60"></div>
          <div class="h-14 animate-pulse rounded-xl bg-muted/60"></div>
          <div class="h-14 animate-pulse rounded-xl bg-muted/60"></div>
        </template>
        <template v-else>
          <button
            v-for="rec in previousRecommendations"
            :key="rec.id || rec.title"
            @click="selectChat(rec)"
            :class="[
              'w-full rounded-xl px-3 py-3 text-left transition-all',
              activeThread === rec.id
                ? 'bg-primary text-primary-foreground shadow-sm'
                : 'bg-background/80 text-muted-foreground hover:bg-muted/60 hover:text-foreground'
            ]"
          >
            <p class="text-sm font-medium leading-tight">{{ rec.title ?? 'Untitled' }}</p>
            <p class="mt-1 text-xs" :class="activeThread === rec.id ? 'text-primary-foreground/80' : 'text-muted-foreground/80'">
              {{ rec.subtitle ?? 'No metadata available' }}
            </p>
          </button>
          <p v-if="!previousRecommendations.length" class="text-xs text-muted-foreground/80">
            No recommendations yet. They will appear here once generated.
          </p>
        </template>
      </nav>
      <div class="border-t border-border/50 pt-4">
        <div class="relative">
          <button
            class="flex w-full items-center justify-between rounded-xl bg-background/80 px-3 py-3 text-sm font-medium text-foreground transition hover:bg-muted"
            @click="toggleAccountMenu"
          >
            <span>{{ displayName }}</span>
            <svg
              class="size-4 text-muted-foreground transition-transform"
              :class="accountMenuOpen ? 'rotate-180' : ''"
              viewBox="0 0 20 20"
              fill="currentColor"
              aria-hidden="true"
            >
              <path
                fill-rule="evenodd"
                d="M5.23 7.21a.75.75 0 011.06.02L10 10.94l3.71-3.71a.75.75 0 111.06 1.06l-4.24 4.24a.75.75 0 01-1.06 0L5.21 8.29a.75.75 0 01.02-1.08z"
                clip-rule="evenodd"
              />
            </svg>
          </button>
          <div
            v-if="accountMenuOpen"
            class="absolute bottom-14 left-0 w-full rounded-xl border border-border/50 bg-background/95 shadow-lg backdrop-blur"
          >
            <button
              class="w-full px-4 py-2 text-left text-sm text-muted-foreground hover:bg-muted/70 hover:text-foreground"
              @click="goToSettings"
            >
              Settings
            </button>
            <button
              class="w-full px-4 py-2 text-left text-sm text-destructive hover:bg-destructive/10"
              @click="logout"
              :disabled="isLoggingOut"
            >
              Sign out
            </button>
          </div>
        </div>
      </div>
    </aside>

    <main class="flex flex-1 flex-col overflow-hidden bg-background">
      <header
        v-if="activeChatTitle"
        class="border-b border-border/60 bg-background/80 px-6 py-5 backdrop-blur lg:px-10"
      >
        <div class="flex flex-col gap-1 sm:flex-row sm:items-center sm:justify-between">
          <h1 class="text-xl font-semibold tracking-tight sm:text-2xl">
            {{ activeChatTitle }}
          </h1>
        </div>
      </header>
      <p v-if="dataError" class="px-6 pt-3 text-xs text-destructive lg:px-10">
        {{ dataError }}
      </p>

      <section class="relative flex flex-1 flex-col overflow-hidden">
        <div ref="messagesScrollRef" class="flex-1 overflow-y-auto px-6 py-6 lg:px-10">
          <div class="mx-auto w-full max-w-4xl space-y-6">
            <template v-if="isLoadingData">
              <div class="h-28 animate-pulse rounded-3xl bg-muted/50"></div>
              <div class="h-24 animate-pulse rounded-3xl bg-muted/50"></div>
            </template>
            <template v-else>
              <template v-if="chatMessages.length">
                <div
                  v-for="(message, index) in chatMessages"
                  :key="message.id || message.timestamp || index"
                  ref="messageItemsRef"
                  :class="[
                    'rounded-2xl border px-4 py-4 shadow-sm',
                    message.role === 'assistant'
                      ? 'border-border/70 bg-background/90'
                      : 'border-transparent bg-primary/5 text-primary'
                  ]"
                >
                  <p class="text-xs uppercase tracking-[0.3em] text-muted-foreground/80">
                    {{ message.role === 'assistant' ? 'Recommendation engine' : 'You' }}
                    <span class="ml-2 text-muted-foreground/60">{{ formatTimestamp(message.timestamp) }}</span>
                  </p>
                  <div class="mt-3 space-y-3 text-sm leading-relaxed">
                    <template v-if="message.title">
                      <p class="text-sm font-semibold text-foreground">{{ message.title }}</p>
                    </template>
                    <template v-if="Array.isArray(message.content)">
                      <ul class="space-y-3">
                        <li
                          v-for="(paragraph, idx) in message.content"
                          :key="idx"
                          class="rounded-xl bg-muted/40 px-3 py-2 text-muted-foreground"
                        >
                          {{ paragraph }}
                        </li>
                      </ul>
                    </template>
                    <template v-else-if="message.content">
                      <div
                        v-if="message.role === 'assistant'"
                        class="text-muted-foreground"
                        v-html="message.content"
                      ></div>
                      <p v-else class="text-muted-foreground">
                        {{ message.content }}
                      </p>
                    </template>
                    <p v-if="!message.content && !Array.isArray(message.content)" class="text-muted-foreground/70">
                      No details recorded.
                    </p>
                  </div>
                </div>
              </template>
              <div v-else class="rounded-2xl border border-dashed border-border/70 px-4 py-10 text-center text-sm text-muted-foreground">
                No project activity yet. Once the recommendation engine generates travel suggestions, they will be summarized here.
              </div>
            </template>
          </div>
        </div>

        <div class="border-t border-border/60 bg-background/95 px-6 py-5 lg:px-10">
          <div class="mx-auto flex w-full max-w-4xl items-center gap-3 rounded-2xl border border-border/70 bg-background/80 px-4 py-3 shadow-sm">
            <Button
              variant="outline"
              size="icon"
              class="shrink-0"
              @click="openUploadModal"
              aria-label="Attach travel documents"
            >
              <svg viewBox="0 0 20 20" fill="currentColor" class="size-4">
                <path
                  fill-rule="evenodd"
                  d="M10 3.25a.75.75 0 01.75.75v5.25H16a.75.75 0 010 1.5h-5.25V16a.75.75 0 01-1.5 0v-5.25H4a.75.75 0 010-1.5h5.25V4a.75.75 0 01.75-.75z"
                  clip-rule="evenodd"
                />
              </svg>
            </Button>
            <textarea
              ref="messageInputRef"
              class="flex-1 resize-none bg-transparent text-sm text-foreground placeholder:text-muted-foreground focus:outline-none"
              rows="1"
              v-model="userInput"
              placeholder="Describe your trip and what you need help with..."
              @input="autoResizeMessage"
              @keydown.enter.exact.prevent="sendChatMessage"
            />
            <Button
              variant="outline"
              :disabled="!canSendMessage"
              @click="sendChatMessage"
            >
              {{ isSendingMessage ? "Thinking..." : "Ask AI" }}
            </Button>
          </div>
          <p class="mt-2 text-center text-xs text-muted-foreground">
            Recommendations are indicative only—always verify travel details (flights, visas, restrictions) before booking.
          </p>
          <div
            v-if="error.value"
            class="mx-auto mt-3 w-full max-w-4xl rounded-md border border-destructive/40 bg-destructive/10 px-3 py-2 text-sm text-destructive"
          >
            {{ error.value }}
          </div>
        </div>

        <UploadAttachmentModal
          v-if="isUploadModalOpen"
          @close="closeUploadModal"
          @ticket="openTicketUploadModal"
          @accommodation="openAccommodationUploadModal"
          @other="openOtherUploadModal"
        />

        <div
          v-if="false && isTicketUploadModalOpen"
          class="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm"
        >
          <div class="w-full max-w-lg rounded-3xl border border-border bg-gradient-to-b from-background/95 to-muted/80 p-6 shadow-2xl">
            <div class="flex items-start justify-between gap-3">
              <div>
                <h2 class="text-base font-semibold text-foreground">Upload airplane ticket</h2>
                <p class="mt-1 text-xs text-muted-foreground">
                  Drag and drop your ticket here, or browse files from your device.
                </p>
              </div>
              <button
                class="rounded-full p-1 text-muted-foreground hover:bg-muted"
                @click="closeTicketUploadModal"
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
                  isTicketDragActive
                    ? 'border-primary bg-primary/5'
                    : 'border-border/70 bg-muted/40 hover:border-primary/60 hover:bg-muted/60'
                ]"
                @click="openTicketFileDialog"
                @dragover.prevent="isTicketDragActive = true"
                @dragleave.prevent="isTicketDragActive = false"
                @drop.prevent="handleTicketDrop"
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
                <p class="text-sm font-medium text-foreground">Drop your ticket PDF or image here</p>
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
                  ref="ticketFileInput"
                  type="file"
                  class="hidden"
                  multiple
                  accept="image/*,application/pdf"
                  @change="handleTicketFileChange"
                />
              </div>

              <div
                v-if="ticketFiles.length"
                class="max-h-40 space-y-2 overflow-y-auto rounded-xl border border-border/70 bg-background/70 px-3 py-2"
              >
                <p class="mb-1 text-[11px] font-medium uppercase tracking-[0.2em] text-muted-foreground">
                  Selected files
                </p>
                <div
                  v-for="(file, index) in ticketFiles"
                  :key="index"
                  class="flex items-center justify-between rounded-lg bg-muted/60 px-3 py-2 text-xs text-foreground"
                >
                  <div class="flex min-w-0 flex-1 items-center gap-2">
                    <div class="flex size-6 items-center justify-center rounded-full bg-background/80 text-[10px] text-muted-foreground">
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
                    @click="removeTicketFile(index)"
                    aria-label="Remove file"
                  >
                    ✕
                  </button>
                </div>
              </div>
              <div v-else class="rounded-xl bg-muted/40 px-3 py-2 text-[11px] text-muted-foreground">
                No files selected yet.
              </div>
            </div>

            <div class="mt-5 flex items-center justify-end gap-3">
              <button
                class="rounded-full px-3 py-1.5 text-xs font-medium text-muted-foreground hover:bg-muted"
                @click="closeTicketUploadModal"
              >
                Cancel
              </button>
              <button
                class="rounded-full bg-primary px-4 py-1.5 text-xs font-medium text-primary-foreground shadow hover:bg-primary/90 disabled:cursor-not-allowed disabled:opacity-60"
                :disabled="!ticketFiles.length"
                @click="closeTicketUploadModal"
              >
                Attach ticket
              </button>
            </div>
          </div>
        </div>

        <div
          v-if="false && isAccommodationUploadModalOpen"
          class="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm"
        >
          <div class="w-full max-w-lg rounded-3xl border border-border bg-gradient-to-b from-background/95 to-muted/80 p-6 shadow-2xl">
            <div class="flex items-start justify-between gap-3">
              <div>
                <h2 class="text-base font-semibold text-foreground">Upload accomodation invoice</h2>
                <p class="mt-1 text-xs text-muted-foreground">
                  Drag and drop your invoice here, or browse files from your device.
                </p>
              </div>
              <button
                class="rounded-full p-1 text-muted-foreground hover:bg-muted"
                @click="closeAccommodationUploadModal"
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
                  isAccommodationDragActive
                    ? 'border-primary bg-primary/5'
                    : 'border-border/70 bg-muted/40 hover:border-primary/60 hover:bg-muted/60'
                ]"
                @click="openAccommodationFileDialog"
                @dragover.prevent="isAccommodationDragActive = true"
                @dragleave.prevent="isAccommodationDragActive = false"
                @drop.prevent="handleAccommodationDrop"
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
                <p class="text-sm font-medium text-foreground">Drop your invoice PDF or image here</p>
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
                  ref="accommodationFileInput"
                  type="file"
                  class="hidden"
                  multiple
                  accept="image/*,application/pdf"
                  @change="handleAccommodationFileChange"
                />
              </div>

              <div
                v-if="accommodationFiles.length"
                class="max-h-40 space-y-2 overflow-y-auto rounded-xl border border-border/70 bg-background/70 px-3 py-2"
              >
                <p class="mb-1 text-[11px] font-medium uppercase tracking-[0.2em] text-muted-foreground">
                  Selected files
                </p>
                <div
                  v-for="(file, index) in accommodationFiles"
                  :key="index"
                  class="flex items-center justify-between rounded-lg bg-muted/60 px-3 py-2 text-xs text-foreground"
                >
                  <div class="flex min-w-0 flex-1 items-center gap-2">
                    <div class="flex size-6 items-center justify-center rounded-full bg-background/80 text-[10px] text-muted-foreground">
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
                    @click="removeAccommodationFile(index)"
                    aria-label="Remove file"
                  >
                    ✕
                  </button>
                </div>
              </div>
              <div v-else class="rounded-xl bg-muted/40 px-3 py-2 text-[11px] text-muted-foreground">
                No files selected yet.
              </div>
            </div>

            <div class="mt-5 flex items-center justify-end gap-3">
              <button
                class="rounded-full px-3 py-1.5 text-xs font-medium text-muted-foreground hover:bg-muted"
                @click="closeAccommodationUploadModal"
              >
                Cancel
              </button>
              <button
                class="rounded-full bg-primary px-4 py-1.5 text-xs font-medium text-primary-foreground shadow hover:bg-primary/90 disabled:cursor-not-allowed disabled:opacity-60"
                :disabled="!accommodationFiles.length"
                @click="closeAccommodationUploadModal"
              >
                Attach invoice
              </button>
            </div>
          </div>
        </div>

        <div
          v-if="false && isOtherUploadModalOpen"
          class="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm"
        >
          <div class="w-full max-w-lg rounded-3xl border border-border bg-gradient-to-b from-background/95 to-muted/80 p-6 shadow-2xl">
            <div class="flex items-start justify-between gap-3">
              <div>
                <h2 class="text-base font-semibold text-foreground">Upload other document</h2>
                <p class="mt-1 text-xs text-muted-foreground">
                  Drag and drop any supporting document here, or browse files from your device.
                </p>
              </div>
              <button
                class="rounded-full p-1 text-muted-foreground hover:bg-muted"
                @click="closeOtherUploadModal"
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
                    <div class="flex size-6 items-center justify-center rounded-full bg-background/80 text-[10px] text-muted-foreground">
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
                    ✕
                  </button>
                </div>
              </div>
              <div v-else class="rounded-xl bg-muted/40 px-3 py-2 text-[11px] text-muted-foreground">
                No files selected yet.
              </div>
            </div>

            <div class="mt-5 flex items-center justify-end gap-3">
              <button
                class="rounded-full px-3 py-1.5 text-xs font-medium text-muted-foreground hover:bg-muted"
                @click="closeOtherUploadModal"
              >
                Cancel
              </button>
              <button
                class="rounded-full bg-primary px-4 py-1.5 text-xs font-medium text-primary-foreground shadow hover:bg-primary/90 disabled:cursor-not-allowed disabled:opacity-60"
                :disabled="!otherFiles.length"
                @click="closeOtherUploadModal"
              >
                Attach document
              </button>
            </div>
          </div>
        </div>

        <TicketUploadModal
          v-if="isTicketUploadModalOpen"
          @close="closeTicketUploadModal"
        />

        <AccommodationUploadModal
          v-if="isAccommodationUploadModalOpen"
          @close="closeAccommodationUploadModal"
        />

        <OtherUploadModal
          v-if="isOtherUploadModalOpen"
          @close="closeOtherUploadModal"
        />
      </section>
    </main>
  </div>
</template>
