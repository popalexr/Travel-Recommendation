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

const uniqueLocations = computed(() => {
  const seen = new Set()
  const results = []
  for (const entry of props.locations || []) {
    const value = String(entry ?? "").trim()
    if (!value) continue
    const key = value.toLowerCase()
    if (seen.has(key)) continue
    seen.add(key)
    results.push(value)
  }
  return results.slice(0, 8)
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

async function updateMarkers() {
  error.value = ""
  if (!map) return
  clearMarkers()

  if (!uniqueLocations.value.length) {
    setMapView()
    return
  }

  isLoading.value = true
  try {
    await fetchGeocodes(uniqueLocations.value)

    uniqueLocations.value.forEach((query) => {
      const result = geocodeCache.get(query)
      if (!result || typeof result.lat !== "number" || typeof result.lng !== "number") {
        return
      }
      const marker = L.marker([result.lat, result.lng])
      marker.bindPopup(result.displayName || query)
      marker.addTo(map)
      markers.push(marker)
    })
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

watch(uniqueLocations, () => {
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

    <p v-if="error" class="mt-3 text-xs text-destructive">
      {{ error }}
    </p>
  </div>
</template>
