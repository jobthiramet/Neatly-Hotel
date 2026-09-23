<script setup lang="ts">
import { computed, ref, useId } from 'vue'

const props = withDefaults(defineProps<{
  labels: string[]
  values: number[]
  max: number
  step: number
  valuePrefix?: string
  valueSuffix?: string
}>(), {
  valuePrefix: '',
  valueSuffix: '',
})

const width = 800
const height = 260
const left = 62
const right = 14
const top = 18
const bottom = 42
const plotWidth = width - left - right
const plotHeight = height - top - bottom
const activeIndex = ref<number | null>(null)
const gradientId = useId()

const points = computed(() => props.values.map((value, index) => ({
  x: left + (index * plotWidth) / Math.max(props.values.length - 1, 1),
  y: top + plotHeight - (value / props.max) * plotHeight,
  value,
  label: props.labels[index],
})))

const polyline = computed(() => points.value.map(point => `${point.x},${point.y}`).join(' '))
const area = computed(() => `${left},${top + plotHeight} ${polyline.value} ${left + plotWidth},${top + plotHeight}`)
const ticks = computed(() => Array.from({ length: Math.floor(props.max / props.step) + 1 }, (_, index) => index * props.step))
const activePoint = computed(() => activeIndex.value === null ? null : points.value[activeIndex.value])

function activateNearest(event: MouseEvent) {
  if (!points.value.length) return
  const svg = event.currentTarget as SVGSVGElement
  const rect = svg.getBoundingClientRect()
  const pointerX = ((event.clientX - rect.left) / rect.width) * width
  activeIndex.value = points.value.reduce((nearest, point, index) => (
    Math.abs(point.x - pointerX) < Math.abs(points.value[nearest]!.x - pointerX) ? index : nearest
  ), 0)
}

function formatValue(value: number) {
  return `${props.valuePrefix}${value.toLocaleString()}${props.valueSuffix}`
}
</script>

<template>
  <div class="relative w-full" @mouseleave="activeIndex = null">
    <svg
      class="block w-full overflow-visible text-gray-600"
      viewBox="0 0 800 260"
      role="img"
      aria-label="Line chart"
      @mousemove="activateNearest"
    >
      <defs>
        <linearGradient :id="gradientId" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0" stop-color="var(--color-orange-200)" stop-opacity="0.65" />
          <stop offset="1" stop-color="var(--color-white)" stop-opacity="0" />
        </linearGradient>
      </defs>

      <g v-for="tick in ticks" :key="tick">
        <line
          :x1="left"
          :x2="left + plotWidth"
          :y1="top + plotHeight - (tick / max) * plotHeight"
          :y2="top + plotHeight - (tick / max) * plotHeight"
          stroke="var(--color-gray-300)"
          stroke-width="1"
        />
        <text
          :x="left - 12"
          :y="top + plotHeight - (tick / max) * plotHeight + 4"
          text-anchor="end"
          fill="currentColor"
          class="text-body3"
        >{{ formatValue(tick) }}</text>
      </g>

      <polygon :points="area" :fill="`url(#${gradientId})`" />
      <polyline :points="polyline" fill="none" stroke="var(--color-orange-500)" stroke-width="3" stroke-linecap="round" stroke-linejoin="round" />

      <g v-for="(point, index) in points" :key="point.label">
        <circle :cx="point.x" :cy="point.y" r="4" fill="var(--color-white)" stroke="var(--color-orange-500)" stroke-width="3" />
        <text :x="point.x" :y="height - 10" text-anchor="middle" fill="currentColor" class="text-body3">{{ point.label }}</text>
        <circle :cx="point.x" :cy="point.y" r="15" fill="transparent" tabindex="0" @focus="activeIndex = index" @blur="activeIndex = null" />
      </g>
    </svg>

    <div
      v-if="activePoint"
      class="pointer-events-none absolute z-20 -mt-3 -translate-x-1/2 -translate-y-full rounded-sm border border-gray-300 bg-white px-3 py-2 text-body1 whitespace-nowrap text-gray-700 shadow-md"
      :style="{ left: `${(activePoint.x / width) * 100}%`, top: `${(activePoint.y / height) * 100}%` }"
      role="tooltip"
    >
      {{ formatValue(activePoint.value) }}
    </div>
  </div>
</template>
