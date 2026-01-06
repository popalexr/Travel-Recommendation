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

function normalizeQuery(value) {
  if (!value) return null
  const text = String(value).replace(/\s+/g, " ").trim()
  return text || null
}

const uniqueLocations = computed(() => {
  const seen = new Set()
  const results = []
  const hotelValue = normalizeQuery(props.hotelLocation)
  for (const entry of props.locations || []) {
    const value = String(entry ?? "").trim()
    if (!value) continue
    const key = value.toLowerCase()
    if (hotelValue && key === hotelValue.toLowerCase()) continue
    if (seen.has(key)) continue
    seen.add(key)
    results.push(value)
  }
  return results.slice(0, 8)
})

const normalizedHotel = computed(() => normalizeQuery(props.hotelLocation))

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

async function updateMarkers() {
  error.value = ""
  if (!map) return
  clearMarkers()

  if (!uniqueLocations.value.length && !normalizedHotel.value) {
    setMapView()
    return
  }

  isLoading.value = true
  try {
    const queries = normalizedHotel.value
      ? [...uniqueLocations.value, normalizedHotel.value]
      : [...uniqueLocations.value]
    await fetchGeocodes(queries)

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

watch([uniqueLocations, normalizedHotel], () => {
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
