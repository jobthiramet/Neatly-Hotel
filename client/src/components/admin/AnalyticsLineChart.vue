<script setup lang="ts">
import { computed, ref, useId } from 'vue'

const props = withDefaults(defineProps<{
  labels: string[]
  values: number[]
  max: number
  step: number
  empty?: boolean
  revenue?: boolean
  valuePrefix?: string
  valueSuffix?: string
}>(), {
  valuePrefix: '',
  valueSuffix: '',
  empty: false,
  revenue: false,
})

const width = props.revenue ? 1000 : 800
const height = props.revenue ? 320 : 260
const left = props.revenue ? 70 : 62
const right = 14
const top = 18
const bottom = 42
const plotWidth = width - left - right
const plotHeight = height - top - bottom
const activeIndex = ref<number | null>(null)
const gradientId = useId()
const chartMax = computed(() => Math.max(props.max, 1))
const chartStep = computed(() => Math.max(props.step, 1))

const points = computed(() => (props.empty ? [] : props.values).map((value, index) => ({
  x: left + (props.values.length === 1 ? plotWidth / 2 : (index * plotWidth) / (props.values.length - 1)),
  y: top + plotHeight - (value / chartMax.value) * plotHeight,
  value,
  label: props.labels[index],
})))

const curve = computed(() => points.value.map((point, index) => {
  const previous = points.value[index - 1]
  if (!previous) return `M ${point.x},${point.y}`
  const middle = (previous.x + point.x) / 2
  return `C ${middle},${previous.y} ${middle},${point.y} ${point.x},${point.y}`
}).join(' '))
const area = computed(() => `${curve.value} L ${points.value.at(-1)?.x ?? left},${top + plotHeight} L ${left},${top + plotHeight} Z`)
const ticks = computed(() => Array.from({ length: Math.floor(chartMax.value / chartStep.value) + 1 }, (_, index) => index * chartStep.value))
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
      :viewBox="`0 0 ${width} ${height}`"
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
          :y1="top + plotHeight - (tick / chartMax) * plotHeight"
          :y2="top + plotHeight - (tick / chartMax) * plotHeight"
          stroke="var(--color-gray-300)"
          stroke-width="1"
        />
        <text
          :x="left - 12"
          :y="top + plotHeight - (tick / chartMax) * plotHeight + 4"
          text-anchor="end"
          fill="currentColor"
          :class="revenue ? 'text-body2' : 'text-body3'"
        >{{ formatValue(tick) }}</text>
      </g>

      <path v-if="points.length" :d="area" :fill="`url(#${gradientId})`" />
      <path v-if="points.length > 1" :d="curve" fill="none" stroke="var(--color-orange-500)" :stroke-width="revenue ? 2.5 : 3" stroke-linecap="round" stroke-linejoin="round" />

      <text
        v-for="(label, index) in labels"
        :key="`${index}-${label}`"
        v-show="index % Math.max(Math.ceil(labels.length / 7), 1) === 0 || index === labels.length - 1"
        :x="left + (labels.length === 1 ? plotWidth / 2 : (index * plotWidth) / (labels.length - 1))"
        :y="height - 10"
        text-anchor="middle"
        fill="currentColor"
        :class="revenue ? 'text-body2' : 'text-body3'"
      >{{ label }}</text>

      <g v-for="(point, index) in points" :key="point.label">
        <circle :cx="point.x" :cy="point.y" r="4" fill="var(--color-white)" stroke="var(--color-orange-500)" :stroke-width="revenue ? 2.5 : 3" />
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
