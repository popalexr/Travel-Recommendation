<script setup>
import { ref, computed, watch, onMounted } from "vue"
import { Button } from "@/components/ui/button"

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
const isUploadModalOpen = ref(false)
const dataError = ref("")
const isLoadingData = ref(true)

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
    previousRecommendations.value = Array.isArray(payload.previousRecommendations)
      ? payload.previousRecommendations
      : []
    chatMessages.value = Array.isArray(payload.chatMessages) ? payload.chatMessages : []
  } catch (err) {
    dataError.value = "Unexpected error loading dashboard."
  } finally {
    isLoadingData.value = false
  }
}

onMounted(() => {
  loadDashboardData()
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
</script>

<template>
  <div class="flex h-screen bg-background text-foreground">
    <aside class="hidden h-full w-72 flex-col border-r border-border/60 bg-muted/30 px-4 py-6 md:flex">
      <div>
        <p class="text-xs uppercase tracking-[0.4em] text-muted-foreground">Trips</p>
        <h2 class="mt-2 font-semibold text-foreground">Previous recommendations</h2>
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
            @click="activeThread = rec.id"
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
      <header class="border-b border-border/60 bg-background/80 px-6 py-5 backdrop-blur lg:px-10">
        <div class="flex flex-col gap-1 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h1 class="text-xl font-semibold tracking-tight sm:text-2xl">
              Travel Recommendation Dashboard
            </h1>
            <p class="text-sm text-muted-foreground">
              Overview of the Travel Recommendation project and generated trips.
            </p>
          </div>
          <div class="flex items-center gap-2 text-xs text-muted-foreground">
            <span class="rounded-full bg-emerald-500/10 px-2 py-1 text-emerald-500">Online</span>
            {{ new Date().toLocaleDateString() }}
          </div>
        </div>
        <p v-if="dataError" class="mt-3 text-xs text-destructive">{{ dataError }}</p>
      </header>

      <section class="relative flex flex-1 flex-col overflow-hidden">
        <div class="flex-1 overflow-y-auto px-6 py-6 lg:px-10">
          <div class="mx-auto w-full max-w-4xl space-y-6">
            <template v-if="isLoadingData">
              <div class="h-28 animate-pulse rounded-3xl bg-muted/50"></div>
              <div class="h-24 animate-pulse rounded-3xl bg-muted/50"></div>
            </template>
            <template v-else>
              <template v-if="chatMessages.length">
                <div
                  v-for="message in chatMessages"
                  :key="message.id || message.timestamp"
                  :class="[
                    'rounded-2xl border px-4 py-4 shadow-sm',
                    message.role === 'assistant'
                      ? 'border-border/70 bg-background/90'
                      : 'border-transparent bg-primary/5 text-primary'
                  ]"
                >
                  <p class="text-xs uppercase tracking-[0.3em] text-muted-foreground/80">
                    {{ message.role === 'assistant' ? 'Recommendation engine' : 'You' }}
                    <span class="ml-2 text-muted-foreground/60">{{ message.timestamp ?? '' }}</span>
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
                      <p class="text-muted-foreground">{{ message.content }}</p>
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
              @click="isUploadModalOpen = true"
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
              class="h-20 flex-1 resize-none bg-transparent text-sm text-foreground placeholder:text-muted-foreground focus:outline-none"
            />
            <Button variant="outline">Coming soon</Button>
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

        <div
          v-if="isUploadModalOpen"
          class="fixed inset-0 z-40 flex items-center justify-center bg-black/40 backdrop-blur-sm"
        >
          <div class="w-full max-w-sm rounded-2xl border border-border bg-background p-6 shadow-xl">
            <div class="flex items-start justify-between gap-3">
              <div>
                <h2 class="text-base font-semibold text-foreground">Attach travel documents</h2>
                <p class="mt-1 text-xs text-muted-foreground">
                  Upload tickets or invoices related to this trip.
                </p>
              </div>
              <button
                class="rounded-full p-1 text-muted-foreground hover:bg-muted"
                @click="isUploadModalOpen = false"
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
              <Button
                variant="outline"
                class="w-full justify-start"
                @click="isUploadModalOpen = false"
              >
                Upload airplane ticket
              </Button>
              <Button
                variant="outline"
                class="w-full justify-start"
                @click="isUploadModalOpen = false"
              >
                Upload accomodation invoice
              </Button>
              <Button
                variant="outline"
                class="w-full justify-start"
                @click="isUploadModalOpen = false"
              >
                Upload other document
              </Button>
            </div>
            <p class="mt-3 text-[11px] text-muted-foreground">
              Uploading is not yet implemented in this prototype.
            </p>
          </div>
        </div>
      </section>
    </main>
  </div>
</template>
