<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from "vue"
import L from "leaflet"
import "leaflet/dist/leaflet.css"
import iconUrl from "leaflet/dist/images/marker-icon.png"
import iconRetinaUrl from "leaflet/dist/images/marker-icon-2x.png"
import shadowUrl from "leaflet/dist/images/marker-shadow.png"

L.Icon.Default.mergeOptions({
  iconUrl,
  iconRetinaUrl,
  shadowUrl,
})

const props = defineProps({
  locations: {
    type: Array,
    default: () => [],
  },
  hotelLocation: {
    type: String,
    default: "",
  },
  showHeader: {
    type: Boolean,
    default: true,
  },
})

const mapContainer = ref(null)
const isLoading = ref(false)
const error = ref("")
const geocodeCache = new Map()
let map = null
let markers = []

const dayPalette = [
  { fill: "#2563eb", stroke: "#1d4ed8" },
  { fill: "#16a34a", stroke: "#15803d" },
  { fill: "#f97316", stroke: "#ea580c" },
  { fill: "#dc2626", stroke: "#b91c1c" },
  { fill: "#0d9488", stroke: "#0f766e" },
  { fill: "#f59e0b", stroke: "#d97706" },
  { fill: "#475569", stroke: "#334155" },
]
const defaultMarkerColor = { fill: "#334155", stroke: "#1f2937" }

function normalizeQuery(value) {
  if (!value) return null
  const text = String(value).replace(/\s+/g, " ").trim()
  return text || null
}

function extractDayIndex(value) {
  if (!value) return null
  const match = String(value).match(/\d+/)
  if (!match) return null
  const parsed = Number.parseInt(match[0], 10)
  return Number.isNaN(parsed) ? null : parsed
}

function getDayColor(dayIndex) {
  if (typeof dayIndex !== "number" || Number.isNaN(dayIndex)) {
    return defaultMarkerColor
  }
  const index = Math.max(dayIndex - 1, 0) % dayPalette.length
  return dayPalette[index]
}

function normalizeLocationEntry(entry) {
  if (entry == null) return null
  if (typeof entry === "string") {
    const label = entry.trim()
    if (!label) return null
    return {
      label,
      dayLabel: null,
      dayIndex: null,
      dayItems: [],
    }
  }
  if (typeof entry !== "object") return null
  const label = String(entry.label ?? entry.location ?? entry.name ?? "").trim()
  if (!label) return null
  const dayLabel = entry.dayLabel ?? entry.day ?? null
  const dayIndex = typeof entry.dayIndex === "number"
    ? entry.dayIndex
    : extractDayIndex(dayLabel)
  const dayItems = Array.isArray(entry.dayItems) ? entry.dayItems : []
  return {
    label,
    dayLabel,
    dayIndex,
    dayItems,
  }
}

function escapeHtml(value) {
  return String(value)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;")
}

const normalizedLocations = computed(() => {
  const seen = new Set()
  const results = []
  const hotelValue = normalizeQuery(props.hotelLocation)
  for (const entry of props.locations || []) {
    const normalized = normalizeLocationEntry(entry)
    if (!normalized) continue
    const key = normalized.label.toLowerCase()
    if (hotelValue && key === hotelValue.toLowerCase()) continue
    if (seen.has(key)) continue
    seen.add(key)
    results.push(normalized)
  }
  return results.slice(0, 8)
})

const normalizedHotel = computed(() => normalizeQuery(props.hotelLocation))

const dayLegend = computed(() => {
  const seen = new Set()
  const legend = []
  for (const entry of normalizedLocations.value) {
    if (!entry.dayLabel) continue
    const key = entry.dayLabel.toLowerCase()
    if (seen.has(key)) continue
    seen.add(key)
    legend.push({
      label: entry.dayLabel,
      color: getDayColor(entry.dayIndex),
    })
  }
  return legend
})

const hotelIcon = L.icon({
  iconUrl:
    "data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='24' height='36' viewBox='0 0 24 36'><path fill='%23dc2626' d='M12 0C6.477 0 2 4.477 2 10c0 7.732 10 26 10 26s10-18.268 10-26C22 4.477 17.523 0 12 0zm0 15a5 5 0 110-10 5 5 0 010 10z'/></svg>",
  iconSize: [24, 36],
  iconAnchor: [12, 36],
  popupAnchor: [0, -32],
})

function clearMarkers() {
  markers.forEach((marker) => marker.remove())
  markers = []
}

function setMapView() {
  if (!map) return
  if (!markers.length) {
    map.setView([20, 0], 2)
    return
  }
  const bounds = L.latLngBounds(markers.map((marker) => marker.getLatLng()))
  map.fitBounds(bounds, { padding: [30, 30] })
}

async function fetchGeocodes(queries) {
  const pending = queries.filter((query) => !geocodeCache.has(query))
  if (!pending.length) {
    return
  }

  const response = await fetch("/api/geocode", {
    method: "POST",
    credentials: "same-origin",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify({ locations: pending }),
  })

  const payload = await response.json().catch(() => ({}))
  if (!response.ok) {
    throw new Error(payload?.error ?? "Failed to geocode locations.")
  }

  const results = Array.isArray(payload.results) ? payload.results : []
  results.forEach((result) => {
    if (!result || !result.query) return
    geocodeCache.set(result.query, result)
  })
}

function buildPopupHtml(entry, result) {
  const title = escapeHtml(result?.displayName || entry.label)
  const dayLabel = entry.dayLabel ? `<p style="margin:4px 0 0;"><strong>${escapeHtml(entry.dayLabel)}</strong></p>` : ""
  let items = ""
  if (Array.isArray(entry.dayItems) && entry.dayItems.length) {
    const list = entry.dayItems
      .map((item) => `<li>${escapeHtml(item)}</li>`)
      .join("")
    items = `<ul style="margin:4px 0 0 18px;padding:0;">${list}</ul>`
  }
  return `<div><div style="font-weight:600;">${title}</div>${dayLabel}${items}</div>`
}

async function updateMarkers() {
  error.value = ""
  if (!map) return
  clearMarkers()

  if (!normalizedLocations.value.length && !normalizedHotel.value) {
    setMapView()
    return
  }

  isLoading.value = true
  try {
    const queries = normalizedHotel.value
      ? [...normalizedLocations.value.map((entry) => entry.label), normalizedHotel.value]
      : [...normalizedLocations.value.map((entry) => entry.label)]
    await fetchGeocodes(queries)

    normalizedLocations.value.forEach((entry) => {
      const result = geocodeCache.get(entry.label)
      if (!result || typeof result.lat !== "number" || typeof result.lng !== "number") {
        return
      }
      const color = getDayColor(entry.dayIndex)
      const marker = L.circleMarker([result.lat, result.lng], {
        radius: 7,
        color: color.stroke,
        fillColor: color.fill,
        fillOpacity: 0.9,
        weight: 2,
      })
      marker.bindPopup(buildPopupHtml(entry, result))
      marker.addTo(map)
      markers.push(marker)
    })

    if (normalizedHotel.value) {
      const result = geocodeCache.get(normalizedHotel.value)
      if (result && typeof result.lat === "number" && typeof result.lng === "number") {
        const marker = L.marker([result.lat, result.lng], { icon: hotelIcon })
        marker.bindPopup(result.displayName || normalizedHotel.value)
        marker.addTo(map)
        markers.push(marker)
      }
    }
    setMapView()
  } catch (err) {
    error.value = err?.message ?? "Unable to load map locations."
    setMapView()
  } finally {
    isLoading.value = false
  }
}

onMounted(() => {
  if (!mapContainer.value) return
  map = L.map(mapContainer.value, { zoomControl: true, scrollWheelZoom: false })
  L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
    maxZoom: 18,
    attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',
  }).addTo(map)
  updateMarkers()
})

watch([normalizedLocations, normalizedHotel], () => {
  updateMarkers()
})

onBeforeUnmount(() => {
  clearMarkers()
  if (map) {
    map.remove()
    map = null
  }
})
</script>

<template>
  <div class="rounded-3xl border border-border/70 bg-background/90 px-5 py-5 shadow-sm">
    <div v-if="showHeader" class="flex items-start justify-between gap-4">
      <div>
        <p class="text-xs uppercase tracking-[0.3em] text-muted-foreground/80">Trip map</p>
        <p class="mt-1 text-sm text-muted-foreground">
          Pins are based on locations mentioned in the latest recommendation.
        </p>
      </div>
      <p v-if="isLoading" class="text-xs text-muted-foreground">Loading map...</p>
    </div>
    <p v-else-if="isLoading" class="text-xs text-muted-foreground">Loading map...</p>

    <div :class="showHeader ? 'mt-4' : 'mt-2'" class="overflow-hidden rounded-2xl border border-border/70">
      <div ref="mapContainer" class="h-72 w-full"></div>
    </div>

    <div v-if="dayLegend.length" class="mt-4 flex flex-wrap items-center gap-3 text-xs text-muted-foreground">
      <span class="font-medium text-foreground">Days:</span>
      <span v-for="day in dayLegend" :key="day.label" class="inline-flex items-center gap-2">
        <span
          class="inline-flex size-2.5 rounded-full"
          :style="{ backgroundColor: day.color.fill, border: `1px solid ${day.color.stroke}` }"
        ></span>
        <span>{{ day.label }}</span>
      </span>
    </div>

    <p v-if="error" class="mt-3 text-xs text-destructive">
      {{ error }}
    </p>
  </div>
</template>
