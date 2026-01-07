<script setup>
import { ref, computed, watch, onMounted, nextTick } from "vue"
import { Button } from "@/components/ui/button"
import UploadAttachmentModal from "@/components/UploadAttachmentModal.vue"
import TicketUploadModal from "@/components/TicketUploadModal.vue"
import AccommodationUploadModal from "@/components/AccommodationUploadModal.vue"
import OtherUploadModal from "@/components/OtherUploadModal.vue"
import AttachmentOptionsModal from "@/components/AttachmentOptionsModal.vue"
import RecommendationMap from "@/components/RecommendationMap.vue"

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
const isRegenerating = ref(false)
const isEditingMessage = ref(false)
const editingMessageId = ref(null)
const editDraft = ref("")
const isProfileLoading = ref(false)
const isProfileSaving = ref(false)
const profileError = ref("")
const profileSuccess = ref("")
const profileForm = ref({
  destination: "",
  startDate: "",
  endDate: "",
  budget: "",
  travelers: "",
  interests: "",
  constraints: "",
})
const isActionModalOpen = ref(false)
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
const isPreferencesModalOpen = ref(false)
const isMapModalOpen = ref(false)
const mapModalLocations = ref([])
const mapModalHotel = ref("")
const mapModalTitle = ref("Recommended locations")
const dataError = ref("")
const isLoadingData = ref(true)
const hasCompletedUploadStep = ref(false)
const hasStartedChat = ref(false)
const pendingMessages = ref([])
const ticketUploaded = ref(false)
const accommodationUploaded = ref(false)
const canSendMessage = computed(() =>
  userInput.value.trim().length > 0
  && !isSendingMessage.value
  && !isRegenerating.value
  && !isEditingMessage.value
  && editingMessageId.value === null
)
const messageInputRef = ref(null)
const messagesScrollRef = ref(null)
const messageItemsRef = ref([])
const chatHasMessages = computed(() => Array.isArray(chatMessages.value) && chatMessages.value.length > 0)
const isComposerLocked = computed(() => !hasStartedChat.value)
const shouldShowUploadPrompt = computed(
  () => !isLoadingData.value && !hasStartedChat.value && !chatHasMessages.value
)
const messagePlaceholder = computed(() =>
  isComposerLocked.value
    ? "Upload documents, then press Start chatting to begin..."
    : "Describe your trip and what you need help with..."
)
const canStartChatting = computed(() => hasCompletedUploadStep.value || chatHasMessages.value)
const activeChatTitle = computed(() => {
  const id = activeThread.value ?? currentChatId.value
  if (!id) return ""
  const rec = previousRecommendations.value.find((item) => item.id === id)
  if (!rec || !rec.title) return ""
  const title = String(rec.title).trim()
  return title.length ? title : ""
})

function isUploadMessage(content) {
  if (!content) return false
  const normalized = String(content).trim().toLowerCase()
  return normalized.startsWith("uploaded airplane ticket:")
    || normalized.startsWith("uploaded accommodation invoice:")
    || normalized.startsWith("uploaded document:")
}

const lastUserMessage = computed(() => {
  const messages = chatMessages.value
  if (!Array.isArray(messages)) return null
  for (let i = messages.length - 1; i >= 0; i -= 1) {
    const message = messages[i]
    if (message && message.role === "user") {
      return message
    }
  }
  return null
})

const canRegenerate = computed(() => {
  const message = lastUserMessage.value
  if (!message || !currentChatId.value) return false
  if (isUploadMessage(message.content)) return false
  if (isSendingMessage.value || isRegenerating.value || isEditingMessage.value) return false
  return true
})

function normalizeLocationText(value) {
  if (!value) return null
  const text = String(value).replace(/\s+/g, " ").trim()
  if (!text) return null
  if (text.toLowerCase().includes("not provided")) return null
  return text
}

function getTextLinesFromContent(content) {
  if (!content) return []
  const raw = String(content)
  const withBreaks = raw
    .replace(/<br\s*\/?>/gi, "\n")
    .replace(/<\/(p|div|li|h1|h2|h3|h4|h5|h6|ul|ol)>/gi, "\n")
    .replace(/<li[^>]*>/gi, "- ")
    .replace(/<[^>]+>/g, "")
    .replace(/&nbsp;/gi, " ")
    .replace(/&amp;/gi, "&")
    .replace(/&quot;/gi, "\"")
    .replace(/&#39;/gi, "'")
    .replace(/&lt;/gi, "<")
    .replace(/&gt;/gi, ">")

  return withBreaks
    .split(/\r?\n/)
    .map((line) => line.replace(/\s+/g, " ").trim())
    .filter(Boolean)
}

function extractLocationsFromMessage(content) {
  if (!content || typeof content !== "string") return []
  if (typeof window === "undefined") return []

  if (typeof DOMParser !== "undefined") {
    try {
      const doc = new DOMParser().parseFromString(content, "text/html")
      const headings = Array.from(doc.querySelectorAll("h1, h2, h3, strong"))
      const heading = headings.find((node) =>
        /recommended locations|locations/i.test(node.textContent || "")
      )

      if (heading) {
        let items = []
        let sibling = heading.nextElementSibling
        while (sibling && sibling.tagName !== "UL" && sibling.tagName !== "OL") {
          sibling = sibling.nextElementSibling
        }
        if (sibling) {
          items = Array.from(sibling.querySelectorAll("li")).map((item) => item.textContent)
        }

        if (items.length) {
          const seen = new Set()
          return items
            .map(normalizeLocationText)
            .filter(Boolean)
            .filter((item) => {
              const key = item.toLowerCase()
              if (seen.has(key)) return false
              seen.add(key)
              return true
            })
        }
      }
    } catch (err) {
      // fall through to text parsing
    }
  }

  const lines = getTextLinesFromContent(content)
  const startIndex = lines.findIndex((line) => /^recommended locations\b/i.test(line))
  if (startIndex < 0) {
    return []
  }

  const items = []
  for (let i = startIndex + 1; i < lines.length; i += 1) {
    const line = lines[i]
    if (/^itinerary\b/i.test(line)) break
    if (/^day\s*\d+/i.test(line)) break
    const cleaned = line.replace(/^[-*]\s*/, "").trim()
    if (cleaned) {
      items.push(cleaned)
    }
  }

  const seen = new Set()
  return items
    .map(normalizeLocationText)
    .filter(Boolean)
    .filter((item) => {
      const key = item.toLowerCase()
      if (seen.has(key)) return false
      seen.add(key)
      return true
    })
}

function extractDayLabel(text) {
  const match = text.match(/\bDay\s*\d+(?:\s*\([^)]*\))?/i)
  if (!match) return null
  return match[0].trim()
}

function parseDayIndex(label) {
  if (!label) return null
  const match = String(label).match(/\d+/)
  if (!match) return null
  const value = Number.parseInt(match[0], 10)
  return Number.isNaN(value) ? null : value
}

function normalizeMatchText(value) {
  if (!value) return ""
  return String(value)
    .toLowerCase()
    .replace(/[^a-z0-9\s]/g, " ")
    .replace(/\s+/g, " ")
    .trim()
}

function extractLocationName(value) {
  if (!value) return ""
  const text = String(value).trim()
  if (!text) return ""
  const parts = text.split(" - ")
  return parts[0] ? parts[0].trim() : text
}

function extractItineraryDaysFromMessage(content) {
  if (!content || typeof content !== "string") return []
  if (typeof window === "undefined") return []

  const lines = getTextLinesFromContent(content)
  if (!lines.length) {
    return []
  }

  const itineraryIndex = lines.findIndex((line) => /^itinerary\b/i.test(line))
  const relevantLines = itineraryIndex >= 0 ? lines.slice(itineraryIndex) : lines
  const dayRegex = /^Day\s*\d+(?:\s*\([^)]*\))?\s*[:\-]?/i

  const days = []
  let currentDay = null

  const ensureDay = (label) => {
    if (!label) return null
    const existing = days.find((day) => day.dayLabel.toLowerCase() === label.toLowerCase())
    if (existing) {
      return existing
    }
    const day = {
      dayLabel: label,
      dayIndex: parseDayIndex(label),
      items: [],
    }
    days.push(day)
    return day
  }

  for (const line of relevantLines) {
    if (!line) continue
    if (/^itinerary\b/i.test(line) && !dayRegex.test(line)) continue
    if (/^recommended locations\b/i.test(line)) {
      if (currentDay) break
      continue
    }

    const segments = dayRegex.test(line)
      ? line.split(/(?=Day\s*\d+)/i).map((segment) => segment.trim()).filter(Boolean)
      : [line]

    for (const segment of segments) {
      const match = segment.match(dayRegex)
      if (match) {
        const label = extractDayLabel(segment)
        currentDay = ensureDay(label)
        const remainder = segment.slice(match[0].length).trim()
        if (remainder) {
          currentDay.items.push(remainder)
        }
        continue
      }

      if (currentDay) {
        currentDay.items.push(segment)
      }
    }
  }

  return days.filter((day) => day.items.length > 0)
}

function normalizeItineraryDays(rawDays) {
  if (!Array.isArray(rawDays)) return []
  return rawDays
    .map((day) => {
      if (!day || typeof day !== "object") return null
      const dayLabel = String(day.dayLabel ?? day.label ?? day.day ?? "").trim()
      if (!dayLabel) return null
      const dayIndex = typeof day.dayIndex === "number" ? day.dayIndex : parseDayIndex(dayLabel)
      const items = Array.isArray(day.items) ? day.items.map((item) => String(item).trim()).filter(Boolean) : []
      return { dayLabel, dayIndex, items }
    })
    .filter(Boolean)
}

function parseItineraryPayload(payload) {
  if (!payload) return []
  if (Array.isArray(payload)) {
    return normalizeItineraryDays(payload)
  }
  if (typeof payload === "object") {
    return normalizeItineraryDays(payload.days ?? payload.itinerary ?? [])
  }
  if (typeof payload === "string") {
    try {
      const parsed = JSON.parse(payload)
      return parseItineraryPayload(parsed)
    } catch (err) {
      return []
    }
  }
  return []
}

function buildLocationSchedule(locations, itineraryDays) {
  if (!Array.isArray(locations) || !locations.length) return []
  if (!Array.isArray(itineraryDays) || !itineraryDays.length) {
    return locations.map((location) => ({
      label: location,
      dayLabel: null,
      dayIndex: null,
      dayItems: [],
    }))
  }

  return locations.map((location) => {
    const label = String(location ?? "").trim()
    const nameMatch = normalizeMatchText(extractLocationName(label))
    const fullMatch = normalizeMatchText(label)
    let matchedDay = null

    for (const day of itineraryDays) {
      const items = Array.isArray(day.items) ? day.items : []
      const hit = items.some((item) => {
        const normalized = normalizeMatchText(item)
        if (!normalized) return false
        if (nameMatch && normalized.includes(nameMatch)) return true
        if (fullMatch && normalized.includes(fullMatch)) return true
        return false
      })
      if (hit) {
        matchedDay = day
        break
      }
    }

    return {
      label,
      dayLabel: matchedDay?.dayLabel ?? null,
      dayIndex: matchedDay?.dayIndex ?? null,
      dayItems: matchedDay?.items ?? [],
    }
  })
}

const locationCache = new Map()

function parseLabeledValue(text, labelRegex) {
  const match = text.match(/^\s*([^:|-]+)\s*[:\-]\s*(.+)$/)
  if (!match) return null
  const label = match[1]?.trim()
  if (!label || !labelRegex.test(label)) return null
  const value = match[2]?.trim()
  return value || null
}

function extractAccommodationLocation(messages) {
  if (!Array.isArray(messages)) return null
  if (typeof window === "undefined" || typeof DOMParser === "undefined") return null

  let lastIndex = -1
  for (let i = 0; i < messages.length; i += 1) {
    const message = messages[i]
    if (message?.role === "user" && typeof message.content === "string") {
      if (message.content.toLowerCase().startsWith("uploaded accommodation invoice:")) {
        lastIndex = i
      }
    }
  }
  if (lastIndex < 0) return null

  let assistant = null
  for (let i = lastIndex + 1; i < messages.length; i += 1) {
    const message = messages[i]
    if (message?.role === "assistant") {
      assistant = message
      break
    }
  }
  if (!assistant || typeof assistant.content !== "string") return null

  try {
    const doc = new DOMParser().parseFromString(assistant.content, "text/html")
    const items = Array.from(doc.querySelectorAll("li, p"))
      .map((item) => item.textContent)
      .filter(Boolean)

    let address = null
    let property = null

    for (const item of items) {
      if (!address) {
        address = parseLabeledValue(item, /address/i)
      }
      if (!property) {
        property = parseLabeledValue(item, /(property|hotel|accommodation)\s*name/i)
      }
      if (address && property) break
    }

    if (address && property) {
      return `${property}, ${address}`
    }
    return address || property || null
  } catch (err) {
    return null
  }
}

const hotelLocation = computed(() => extractAccommodationLocation(chatMessages.value))

function getLocationsForMessage(message) {
  if (!message || message.role !== "assistant") return []
  const key = message.id ?? message.timestamp ?? message.content
  if (locationCache.has(key)) {
    return locationCache.get(key)
  }
  const locations = extractLocationsFromMessage(message.content)
  const itinerary = message.itinerary
    ? parseItineraryPayload(message.itinerary)
    : extractItineraryDaysFromMessage(message.content)
  const scheduled = buildLocationSchedule(locations, itinerary)
  locationCache.set(key, scheduled)
  return scheduled
}

function openMapModal(locations) {
  mapModalLocations.value = Array.isArray(locations) ? locations : []
  mapModalHotel.value = hotelLocation.value ?? ""
  isMapModalOpen.value = mapModalLocations.value.length > 0 || Boolean(mapModalHotel.value)
}

function closeMapModal() {
  isMapModalOpen.value = false
  mapModalLocations.value = []
  mapModalHotel.value = ""
  mapModalTitle.value = "Recommended locations"
}

function resetProfileForm() {
  profileForm.value = {
    destination: "",
    startDate: "",
    endDate: "",
    budget: "",
    travelers: "",
    interests: "",
    constraints: "",
  }
}

function applyProfilePayload(payload) {
  const profile = payload ?? {}
  profileForm.value = {
    destination: profile.destination ?? "",
    startDate: profile.startDate ?? "",
    endDate: profile.endDate ?? "",
    budget: profile.budget ?? "",
    travelers: profile.travelers ?? "",
    interests: profile.interests ?? "",
    constraints: profile.constraints ?? "",
  }
}

async function loadProfile(chatId) {
  profileError.value = ""
  profileSuccess.value = ""

  if (!chatId) {
    resetProfileForm()
    return
  }

  isProfileLoading.value = true
  try {
    const response = await fetch(`/api/chat/${chatId}/profile`, {
      credentials: "same-origin",
    })
    const payload = await response.json().catch(() => ({}))
    if (!response.ok) {
      profileError.value = payload?.error ?? "Unable to load trip preferences."
      resetProfileForm()
      return
    }
    applyProfilePayload(payload.profile)
  } catch (err) {
    profileError.value = "Unexpected error loading preferences."
    resetProfileForm()
  } finally {
    isProfileLoading.value = false
  }
}

async function saveProfile() {
  profileError.value = ""
  profileSuccess.value = ""

  if (!currentChatId.value) {
    profileError.value = "Start a chat before saving preferences."
    return
  }

  if (isProfileSaving.value) return
  isProfileSaving.value = true

  try {
    const response = await fetch(`/api/chat/${currentChatId.value}/profile`, {
      method: "POST",
      credentials: "same-origin",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        destination: profileForm.value.destination,
        startDate: profileForm.value.startDate,
        endDate: profileForm.value.endDate,
        budget: profileForm.value.budget,
        travelers: profileForm.value.travelers,
        interests: profileForm.value.interests,
        constraints: profileForm.value.constraints,
      }),
    })

    const payload = await response.json().catch(() => ({}))
    if (!response.ok) {
      profileError.value = payload?.error ?? "Unable to save trip preferences."
      return
    }

    applyProfilePayload(payload.profile)
    profileSuccess.value = "Preferences saved."
  } catch (err) {
    profileError.value = "Unexpected error saving preferences."
  } finally {
    isProfileSaving.value = false
  }
}

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
    hasCompletedUploadStep.value = chatMessages.value.length > 0
    hasStartedChat.value = chatMessages.value.length > 0
    pendingMessages.value = []
    ticketUploaded.value = false
    accommodationUploaded.value = false
    cancelEditing()
    isRegenerating.value = false
    isEditingMessage.value = false
    if (recs.length > 0 && !activeThread.value) {
      activeThread.value = recs[0].id
      currentChatId.value = recs[0].id
    }
    await loadProfile(currentChatId.value)
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

function startNewChat() {
  activeThread.value = null
  currentChatId.value = null
  chatMessages.value = []
  pendingMessages.value = []
  userInput.value = ""
  cancelEditing()
  closeMapModal()
  isRegenerating.value = false
  isEditingMessage.value = false
  profileError.value = ""
  profileSuccess.value = ""
  resetProfileForm()
  hasCompletedUploadStep.value = false
  hasStartedChat.value = false
  ticketUploaded.value = false
  accommodationUploaded.value = false
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

function canEditMessage(message) {
  if (!message || message.role !== "user") return false
  if (!currentChatId.value) return false
  if (typeof message.id !== "number") return false
  if (isUploadMessage(message.content)) return false
  if (isSendingMessage.value || isRegenerating.value || isEditingMessage.value) return false
  const lastMessage = lastUserMessage.value
  return Boolean(lastMessage && lastMessage.id === message.id)
}

function startEditing(message) {
  if (!canEditMessage(message)) return
  editingMessageId.value = message.id
  editDraft.value = message.content ?? ""
}

function cancelEditing() {
  editingMessageId.value = null
  editDraft.value = ""
}

async function selectChat(rec) {
  if (!rec || !rec.id) return
  activeThread.value = rec.id
  currentChatId.value = rec.id
  ticketUploaded.value = false
  accommodationUploaded.value = false
  cancelEditing()
  closeMapModal()
  isRegenerating.value = false
  isEditingMessage.value = false
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
    pendingMessages.value = []
    hasCompletedUploadStep.value = chatMessages.value.length > 0
    hasStartedChat.value = chatMessages.value.length > 0
    await loadProfile(rec.id)
  } catch (err) {
    dataError.value = "Unexpected error loading chat messages."
  } finally {
    isLoadingData.value = false
  }
}

async function sendChatMessage() {
  if (isComposerLocked.value) return

  const text = userInput.value.trim()
  if (!text || isSendingMessage.value) return
  cancelEditing()

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

    if (payload && Array.isArray(payload.messages) && payload.messages.length) {
      const updatedMessages = [...chatMessages.value]
      const serverUser = payload.messages.find((msg) => msg.role === "user") ?? payload.messages[0]
      const serverAssistant = payload.messages.find((msg) => msg.role === "assistant") ?? payload.message

      if (serverUser) {
        const localIndex = updatedMessages.findIndex((msg) => msg.id === localUserMessage.id)
        const mergedUser = {
          ...serverUser,
          timestamp,
        }
        if (localIndex >= 0) {
          updatedMessages.splice(localIndex, 1, mergedUser)
        } else {
          updatedMessages.push(mergedUser)
        }
      }

      if (serverAssistant) {
        updatedMessages.push(serverAssistant)
      }

      chatMessages.value = updatedMessages
    } else if (payload && payload.message) {
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

async function saveEditedMessage(message) {
  if (!message || !currentChatId.value || isEditingMessage.value) return
  const trimmed = editDraft.value.trim()
  if (!trimmed) {
    error.value = "Message cannot be empty."
    return
  }

  error.value = ""
  isEditingMessage.value = true

  try {
    const response = await fetch("/api/chat/edit-latest", {
      method: "POST",
      credentials: "same-origin",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        chatId: currentChatId.value,
        messageId: message.id,
        message: trimmed,
      }),
    })

    const payload = await response.json().catch(() => ({}))
    if (!response.ok) {
      error.value = payload?.error ?? "Unable to update the message."
      return
    }

    if (Array.isArray(payload.messages)) {
      chatMessages.value = payload.messages
    }

    cancelEditing()
  } catch (err) {
    error.value = "Unexpected error while updating the message."
  } finally {
    isEditingMessage.value = false
  }
}

async function regenerateRecommendation() {
  if (!canRegenerate.value || isRegenerating.value) return
  error.value = ""
  isRegenerating.value = true
  cancelEditing()

  try {
    const response = await fetch("/api/chat/regenerate", {
      method: "POST",
      credentials: "same-origin",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        chatId: currentChatId.value,
      }),
    })

    const payload = await response.json().catch(() => ({}))
    if (!response.ok) {
      error.value = payload?.error ?? "Unable to regenerate the recommendation."
      return
    }

    if (Array.isArray(payload.messages)) {
      chatMessages.value = payload.messages
    }
  } catch (err) {
    error.value = "Unexpected error while regenerating."
  } finally {
    isRegenerating.value = false
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
  (messages) => {
    if (Array.isArray(messages) && messages.length > 0) {
      hasCompletedUploadStep.value = true
    }
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

function skipUploadStep() {
  hasCompletedUploadStep.value = true
}

function startChatting() {
  if (hasStartedChat.value) return
  if (!hasCompletedUploadStep.value && !chatHasMessages.value && !pendingMessages.value.length) return
  hasStartedChat.value = true
  if (pendingMessages.value.length) {
    chatMessages.value = pendingMessages.value
    pendingMessages.value = []
  }
}

function openUploadModal() {
  isUploadModalOpen.value = true
}

function closeUploadModal() {
  isUploadModalOpen.value = false
}

function openActionModal() {
  isActionModalOpen.value = true
}

function closeActionModal() {
  isActionModalOpen.value = false
}

function openPreferencesModal() {
  isPreferencesModalOpen.value = true
  profileError.value = ""
  profileSuccess.value = ""
  if (currentChatId.value) {
    loadProfile(currentChatId.value)
  }
}

function closePreferencesModal() {
  isPreferencesModalOpen.value = false
  profileError.value = ""
  profileSuccess.value = ""
}

function handleActionUpload() {
  closeActionModal()
  openUploadModal()
}

function handleActionPreferences() {
  closeActionModal()
  openPreferencesModal()
}

function openTicketUploadModal() {
  isUploadModalOpen.value = false
  isTicketUploadModalOpen.value = true
}

function closeTicketUploadModal() {
  isTicketUploadModalOpen.value = false
}

function handleTicketUploaded(payload) {
  closeTicketUploadModal()
  if (!payload || typeof payload !== "object") return

  hasCompletedUploadStep.value = true
  ticketUploaded.value = true

  const { chatId, chatTitle, messages } = payload
  const incomingMessages = Array.isArray(messages) ? messages : []
  const switchingChat = Boolean(chatId && currentChatId.value && chatId !== currentChatId.value)

  if (incomingMessages.length) {
    if (hasStartedChat.value || chatHasMessages.value) {
      if (!currentChatId.value || switchingChat) {
        chatMessages.value = incomingMessages
      } else {
        chatMessages.value = [...chatMessages.value, ...incomingMessages]
      }
    } else {
      const sameChat = !switchingChat && chatId && chatId === currentChatId.value
      pendingMessages.value = sameChat
        ? [...pendingMessages.value, ...incomingMessages]
        : incomingMessages
    }
  }

  if (chatId) {
    currentChatId.value = chatId
    activeThread.value = chatId
  }

  if (chatId) {
    const existingIndex = previousRecommendations.value.findIndex((item) => item.id === chatId)
    if (existingIndex >= 0) {
      const existing = previousRecommendations.value[existingIndex]
      previousRecommendations.value.splice(existingIndex, 1, {
        ...existing,
        title: chatTitle || existing.title || "Untitled chat",
      })
    } else {
      previousRecommendations.value = [
        {
          id: chatId,
          title: chatTitle || "Untitled chat",
          subtitle: "AI travel recommendations",
        },
        ...previousRecommendations.value,
      ]
    }
  }

  if (chatId) {
    loadProfile(chatId)
  }
}

function handleOtherUploaded(payload) {
  closeOtherUploadModal()
  if (!payload || typeof payload !== "object") return

  hasCompletedUploadStep.value = true

  const { chatId, chatTitle, messages } = payload
  const incomingMessages = Array.isArray(messages) ? messages : []
  const switchingChat = Boolean(chatId && currentChatId.value && chatId !== currentChatId.value)

  if (incomingMessages.length) {
    if (hasStartedChat.value || chatHasMessages.value) {
      if (!currentChatId.value || switchingChat) {
        chatMessages.value = incomingMessages
      } else {
        chatMessages.value = [...chatMessages.value, ...incomingMessages]
      }
    } else {
      const sameChat = !switchingChat && chatId && chatId === currentChatId.value
      pendingMessages.value = sameChat
        ? [...pendingMessages.value, ...incomingMessages]
        : incomingMessages
    }
  }

  if (chatId) {
    currentChatId.value = chatId
    activeThread.value = chatId
  }

  if (chatId) {
    const existingIndex = previousRecommendations.value.findIndex((item) => item.id === chatId)
    if (existingIndex >= 0) {
      const existing = previousRecommendations.value[existingIndex]
      previousRecommendations.value.splice(existingIndex, 1, {
        ...existing,
        title: chatTitle || existing.title || "Untitled chat",
      })
    } else {
      previousRecommendations.value = [
        {
          id: chatId,
          title: chatTitle || "Untitled chat",
          subtitle: "AI travel recommendations",
        },
        ...previousRecommendations.value,
      ]
    }
  }

  if (chatId) {
    loadProfile(chatId)
  }
}

function openAccommodationUploadModal() {
  isUploadModalOpen.value = false
  isAccommodationUploadModalOpen.value = true
}

function closeAccommodationUploadModal() {
  isAccommodationUploadModalOpen.value = false
}

function handleAccommodationUploaded(payload) {
  closeAccommodationUploadModal()
  if (!payload || typeof payload !== "object") return

  hasCompletedUploadStep.value = true
  accommodationUploaded.value = true

  const { chatId, chatTitle, messages } = payload
  const incomingMessages = Array.isArray(messages) ? messages : []
  const switchingChat = Boolean(chatId && currentChatId.value && chatId !== currentChatId.value)

  if (incomingMessages.length) {
    if (hasStartedChat.value || chatHasMessages.value) {
      if (!currentChatId.value || switchingChat) {
        chatMessages.value = incomingMessages
      } else {
        chatMessages.value = [...chatMessages.value, ...incomingMessages]
      }
    } else {
      const sameChat = !switchingChat && chatId && chatId === currentChatId.value
      pendingMessages.value = sameChat
        ? [...pendingMessages.value, ...incomingMessages]
        : incomingMessages
    }
  }

  if (chatId) {
    currentChatId.value = chatId
    activeThread.value = chatId
  }

  if (chatId) {
    const existingIndex = previousRecommendations.value.findIndex((item) => item.id === chatId)
    if (existingIndex >= 0) {
      const existing = previousRecommendations.value[existingIndex]
      previousRecommendations.value.splice(existingIndex, 1, {
        ...existing,
        title: chatTitle || existing.title || "Untitled chat",
      })
    } else {
      previousRecommendations.value = [
        {
          id: chatId,
          title: chatTitle || "Untitled chat",
          subtitle: "AI travel recommendations",
        },
        ...previousRecommendations.value,
      ]
    }
  }

  if (chatId) {
    loadProfile(chatId)
  }
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
          @click="startNewChat"
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
                  <div class="flex items-start justify-between gap-3">
                    <p class="text-xs uppercase tracking-[0.3em] text-muted-foreground/80">
                      {{ message.role === 'assistant' ? 'Recommendation engine' : 'You' }}
                      <span class="ml-2 text-muted-foreground/60">{{ formatTimestamp(message.timestamp) }}</span>
                    </p>
                    <button
                      v-if="canEditMessage(message) && editingMessageId !== message.id"
                      class="rounded-full border border-border/70 px-2 py-0.5 text-[10px] uppercase tracking-[0.2em] text-muted-foreground transition hover:text-foreground"
                      @click="startEditing(message)"
                    >
                      Edit
                    </button>
                  </div>
                  <div class="mt-3 space-y-3 text-sm leading-relaxed">
                    <template v-if="editingMessageId === message.id">
                      <textarea
                        v-model="editDraft"
                        rows="3"
                        class="w-full resize-none rounded-xl border border-border/70 bg-background/70 px-3 py-2 text-sm text-foreground focus:outline-none"
                      />
                      <div class="flex items-center justify-end gap-2">
                        <Button size="sm" variant="ghost" @click="cancelEditing">
                          Cancel
                        </Button>
                        <Button
                          size="sm"
                          :disabled="!editDraft.trim() || isEditingMessage"
                          @click="saveEditedMessage(message)"
                        >
                          {{ isEditingMessage ? "Saving..." : "Save & regenerate" }}
                        </Button>
                      </div>
                    </template>
                    <template v-else>
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
                        <div
                          v-if="message.role === 'assistant' && getLocationsForMessage(message).length"
                          class="pt-2"
                        >
                          <Button
                            size="sm"
                            variant="outline"
                            @click="openMapModal(getLocationsForMessage(message))"
                          >
                            Open map
                          </Button>
                        </div>
                      </template>
                      <p v-if="!message.content && !Array.isArray(message.content)" class="text-muted-foreground/70">
                        No details recorded.
                      </p>
                    </template>
                  </div>
                </div>
              </template>
              <div
                v-else-if="shouldShowUploadPrompt"
                class="rounded-3xl border border-dashed border-border/70 bg-muted/30 px-6 py-10 text-center shadow-sm"
              >
                <p class="text-sm font-semibold text-foreground">Step 1 · Add your travel documents</p>
                <p class="mt-2 text-sm text-muted-foreground">
                  Upload tickets or invoices so the assistant understands your trip. You can skip if you don't have files yet.
                </p>
                <div class="mt-4 flex flex-wrap items-center justify-center gap-4">
                  <div class="flex flex-col items-center gap-1">
                    <Button variant="outline" @click="openTicketUploadModal">Upload ticket</Button>
                    <div
                      v-if="ticketUploaded"
                      class="inline-flex items-center gap-1 rounded-full bg-green-50 px-2 py-[2px] text-[11px] font-semibold text-green-700"
                    >
                      <svg viewBox="0 0 20 20" fill="currentColor" class="size-3">
                        <path
                          fill-rule="evenodd"
                          d="M16.704 5.29a.75.75 0 010 1.06l-7.25 7.25a.75.75 0 01-1.06 0l-3.25-3.25a.75.75 0 111.06-1.06L8.9 12.44l6.72-6.72a.75.75 0 011.084.57z"
                          clip-rule="evenodd"
                        />
                      </svg>
                      Ticket uploaded
                    </div>
                  </div>
                  <div class="flex flex-col items-center gap-1">
                    <Button variant="outline" @click="openAccommodationUploadModal">Upload invoice</Button>
                    <div
                      v-if="accommodationUploaded"
                      class="inline-flex items-center gap-1 rounded-full bg-green-50 px-2 py-[2px] text-[11px] font-semibold text-green-700"
                    >
                      <svg viewBox="0 0 20 20" fill="currentColor" class="size-3">
                        <path
                          fill-rule="evenodd"
                          d="M16.704 5.29a.75.75 0 010 1.06l-7.25 7.25a.75.75 0 01-1.06 0l-3.25-3.25a.75.75 0 111.06-1.06L8.9 12.44l6.72-6.72a.75.75 0 011.084.57z"
                          clip-rule="evenodd"
                        />
                      </svg>
                      Invoice uploaded
                    </div>
                  </div>
                  <Button variant="ghost" class="text-muted-foreground hover:text-foreground" @click="skipUploadStep">
                    Skip for now
                  </Button>
                </div>
                <div class="mt-8 text-sm">
                  <p class="font-semibold text-foreground">Step 2 · Start chatting</p>
                  <p class="mt-1 text-xs text-muted-foreground">
                    Press start after you've added documents (or skipped) to begin the conversation.
                  </p>
                  <div class="mt-3 flex items-center justify-center">
                    <Button :disabled="!canStartChatting" @click="startChatting">
                      Start chatting
                    </Button>
                  </div>
                </div>
              </div>
              <div
                v-else
                class="rounded-2xl border border-dashed border-border/70 px-4 py-10 text-center text-sm text-muted-foreground"
              >
                No project activity yet. Share your trip details or attach documents to get recommendations.
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
              @click="openActionModal"
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
              :placeholder="messagePlaceholder"
              :disabled="isComposerLocked"
              :class="isComposerLocked ? 'cursor-not-allowed text-muted-foreground' : ''"
              @input="autoResizeMessage"
              @keydown.enter.exact.prevent="sendChatMessage"
            />
            <Button
              variant="outline"
              :disabled="!canSendMessage || isComposerLocked"
              @click="sendChatMessage"
            >
              {{ isSendingMessage ? "Thinking..." : "Ask AI" }}
            </Button>
            <Button
              variant="ghost"
              :disabled="!canRegenerate"
              @click="regenerateRecommendation"
            >
              {{ isRegenerating ? "Regenerating..." : "Regenerate" }}
            </Button>
          </div>
          <p v-if="isComposerLocked" class="mt-2 text-center text-xs text-muted-foreground">
            Add documents or skip, then press “Start chatting” to unlock the chat box.
          </p>
          <div v-if="isComposerLocked" class="mt-3 flex justify-center">
            <Button size="sm" :disabled="!canStartChatting" @click="startChatting">
              Start chatting
            </Button>
          </div>
          <p class="mt-2 text-center text-xs text-muted-foreground">
            Recommendations are indicative only-always verify travel details (flights, visas, restrictions) before booking.
          </p>
          <div
            v-if="error.value"
            class="mx-auto mt-3 w-full max-w-4xl rounded-md border border-destructive/40 bg-destructive/10 px-3 py-2 text-sm text-destructive"
          >
            {{ error.value }}
          </div>
        </div>

        <AttachmentOptionsModal
          v-if="isActionModalOpen"
          @close="closeActionModal"
          @upload="handleActionUpload"
          @preferences="handleActionPreferences"
        />

        <div
          v-if="isPreferencesModalOpen"
          class="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm"
        >
          <div class="w-full max-w-2xl rounded-3xl border border-border bg-background p-6 shadow-2xl">
            <div class="flex items-start justify-between gap-3">
              <div>
                <h2 class="text-base font-semibold text-foreground">Trip preferences</h2>
                <p class="mt-1 text-xs text-muted-foreground">
                  Save details to personalize the itinerary and recommendations.
                </p>
              </div>
              <button
                class="rounded-full p-1 text-muted-foreground hover:bg-muted"
                @click="closePreferencesModal"
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

            <div v-if="isProfileLoading" class="mt-5 space-y-3">
              <div class="h-10 animate-pulse rounded-xl bg-muted/50"></div>
              <div class="h-10 animate-pulse rounded-xl bg-muted/50"></div>
              <div class="h-10 animate-pulse rounded-xl bg-muted/50"></div>
            </div>

            <div v-else class="mt-5 grid gap-4 sm:grid-cols-2">
              <label class="space-y-1 text-xs font-semibold uppercase tracking-[0.2em] text-muted-foreground">
                Destination
                <input
                  v-model="profileForm.destination"
                  :disabled="!currentChatId"
                  class="w-full rounded-xl border border-border/70 bg-background/70 px-3 py-2 text-sm text-foreground focus:outline-none disabled:cursor-not-allowed disabled:opacity-60"
                  placeholder="e.g., Lisbon"
                />
              </label>
              <label class="space-y-1 text-xs font-semibold uppercase tracking-[0.2em] text-muted-foreground">
                Travelers
                <input
                  v-model="profileForm.travelers"
                  :disabled="!currentChatId"
                  class="w-full rounded-xl border border-border/70 bg-background/70 px-3 py-2 text-sm text-foreground focus:outline-none disabled:cursor-not-allowed disabled:opacity-60"
                  placeholder="2 adults, 1 child"
                />
              </label>
              <label class="space-y-1 text-xs font-semibold uppercase tracking-[0.2em] text-muted-foreground">
                Start date
                <input
                  v-model="profileForm.startDate"
                  :disabled="!currentChatId"
                  type="date"
                  class="w-full rounded-xl border border-border/70 bg-background/70 px-3 py-2 text-sm text-foreground focus:outline-none disabled:cursor-not-allowed disabled:opacity-60"
                />
              </label>
              <label class="space-y-1 text-xs font-semibold uppercase tracking-[0.2em] text-muted-foreground">
                End date
                <input
                  v-model="profileForm.endDate"
                  :disabled="!currentChatId"
                  type="date"
                  class="w-full rounded-xl border border-border/70 bg-background/70 px-3 py-2 text-sm text-foreground focus:outline-none disabled:cursor-not-allowed disabled:opacity-60"
                />
              </label>
              <label class="space-y-1 text-xs font-semibold uppercase tracking-[0.2em] text-muted-foreground">
                Budget
                <input
                  v-model="profileForm.budget"
                  :disabled="!currentChatId"
                  class="w-full rounded-xl border border-border/70 bg-background/70 px-3 py-2 text-sm text-foreground focus:outline-none disabled:cursor-not-allowed disabled:opacity-60"
                  placeholder="e.g., 1200 EUR"
                />
              </label>
              <label class="space-y-1 text-xs font-semibold uppercase tracking-[0.2em] text-muted-foreground">
                Interests
                <textarea
                  v-model="profileForm.interests"
                  :disabled="!currentChatId"
                  rows="3"
                  class="w-full resize-none rounded-xl border border-border/70 bg-background/70 px-3 py-2 text-sm text-foreground focus:outline-none disabled:cursor-not-allowed disabled:opacity-60"
                  placeholder="Food, museums, beach days"
                ></textarea>
              </label>
              <label class="space-y-1 text-xs font-semibold uppercase tracking-[0.2em] text-muted-foreground sm:col-span-2">
                Constraints
                <textarea
                  v-model="profileForm.constraints"
                  :disabled="!currentChatId"
                  rows="3"
                  class="w-full resize-none rounded-xl border border-border/70 bg-background/70 px-3 py-2 text-sm text-foreground focus:outline-none disabled:cursor-not-allowed disabled:opacity-60"
                  placeholder="No red-eye flights, vegetarian options, accessible transit"
                ></textarea>
              </label>
            </div>

            <p v-if="!currentChatId" class="mt-3 text-xs text-muted-foreground">
              Start a chat or upload a document to save trip preferences.
            </p>
            <p v-if="profileError" class="mt-3 text-xs text-destructive">
              {{ profileError }}
            </p>
            <p v-if="profileSuccess" class="mt-3 text-xs text-green-600">
              {{ profileSuccess }}
            </p>

            <div class="mt-5 flex items-center justify-end gap-3">
              <Button variant="ghost" size="sm" @click="closePreferencesModal">
                Close
              </Button>
              <Button
                size="sm"
                :disabled="!currentChatId || isProfileSaving || isProfileLoading"
                @click="saveProfile"
              >
                {{ isProfileSaving ? "Saving..." : "Save preferences" }}
              </Button>
            </div>
          </div>
        </div>

        <div
          v-if="isMapModalOpen"
          class="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm"
        >
          <div class="w-full max-w-5xl rounded-3xl border border-border bg-background p-6 shadow-2xl">
            <div class="flex items-start justify-between gap-3">
              <div>
                <h2 class="text-base font-semibold text-foreground">{{ mapModalTitle }}</h2>
                <p class="mt-1 text-xs text-muted-foreground">
                  Pins are based on locations mentioned in the recommendation.
                </p>
              </div>
              <button
                class="rounded-full p-1 text-muted-foreground hover:bg-muted"
                @click="closeMapModal"
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

            <div class="mt-5">
              <RecommendationMap
                :locations="mapModalLocations"
                :hotel-location="mapModalHotel"
                :show-header="false"
              />
            </div>
          </div>
        </div>

        <UploadAttachmentModal
          v-if="isUploadModalOpen"
          :ticket-uploaded="ticketUploaded"
          :accommodation-uploaded="accommodationUploaded"
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
          :chat-id="currentChatId"
          @close="closeTicketUploadModal"
          @uploaded="handleTicketUploaded"
        />

        <AccommodationUploadModal
          v-if="isAccommodationUploadModalOpen"
          :chat-id="currentChatId"
          @close="closeAccommodationUploadModal"
          @uploaded="handleAccommodationUploaded"
        />

        <OtherUploadModal
          v-if="isOtherUploadModalOpen"
          :chat-id="currentChatId"
          @close="closeOtherUploadModal"
          @uploaded="handleOtherUploaded"
        />
      </section>
    </main>
  </div>
</template>
