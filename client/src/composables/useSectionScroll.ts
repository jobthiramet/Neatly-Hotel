import type { MaybeRefOrGetter } from 'vue'
import { useMediaQuery, usePreferredReducedMotion } from '@vueuse/core'
import { onScopeDispose, readonly, ref, toValue, watch } from 'vue'

// ── Tuning ────────────────────────────────────────────────────────────────
/** Length of one section-to-section glide. */
const DURATION_MS = 800
/** Accumulated wheel delta (px) before a gesture counts, so small trackpad moves don't trigger. */
const WHEEL_THRESHOLD = 50
/** No wheel events for this long = the gesture (and its trackpad momentum) has ended. */
const QUIET_MS = 150
/** After a glide, a delta this many times larger than the last one = a new deliberate swipe. */
const NEW_GESTURE_RATIO = 1.5
/** ...and at least this big (px), so jitter at the end of a momentum tail isn't a new swipe. */
const NEW_GESTURE_MIN_DELTA = 20
/** Wheel `deltaMode` line height (Firefox). */
const LINE_HEIGHT_PX = 16

function easeInOutCubic(t: number) {
  return t < 0.5 ? 4 * t * t * t : 1 - (-2 * t + 2) ** 3 / 2
}
const EASING = easeInOutCubic

/** Same condition as the `desktop` CSS variant in main.css (lg = 64rem). */
const DESKTOP_QUERY = '(min-width: 64rem) and (hover: hover) and (pointer: fine)'

const KEY_DIRECTIONS: Record<string, 1 | -1> = {
  PageDown: 1,
  ArrowDown: 1,
  PageUp: -1,
  ArrowUp: -1,
}

interface FreeRange { start: number, end: number }

/**
 * Desktop home page: one wheel/trackpad gesture or key press glides to the next
 * `[data-scroll-section]` inside `root`. Sections taller than the screen scroll
 * natively between their top and bottom edges. Mobile and touch devices are untouched.
 */
export function useSectionScroll(
  root: MaybeRefOrGetter<HTMLElement | null>,
  /** `offset`: height (px) of the sticky navbar sections must land below. */
  { offset = () => 0 }: { offset?: () => number } = {},
) {
  const enabled = useMediaQuery(DESKTOP_QUERY)
  const reducedMotion = usePreferredReducedMotion()
  const currentIndex = ref(0)

  // Cached layout: scroll positions to stop at, and ranges that scroll natively.
  let stops: number[] = [0]
  let freeRanges: FreeRange[] = []

  let frame = 0
  let animating = false
  let animationEndedAt = 0
  let targetIndex = 0

  let lastWheelAt = 0
  let lastDelta = 0
  let accumulated = 0
  let momentumLock = false
  let nativeStream = false
  let scrollbarDrag = false
  let nativeKeyScroll = false

  let observer: ResizeObserver | undefined

  function measure() {
    const sections = toValue(root)?.querySelectorAll<HTMLElement>('[data-scroll-section]') ?? []
    const top0 = offset()
    const viewport = window.innerHeight
    const maxScroll = document.documentElement.scrollHeight - viewport
    const positions = new Set<number>([0])
    freeRanges = []
    sections.forEach((section, index) => {
      const top = Math.round(section.getBoundingClientRect().top + window.scrollY) - top0
      const bottomStop = Math.min(top + top0 + section.offsetHeight - viewport, maxScroll)
      if (index > 0) positions.add(Math.min(top, maxScroll))
      // Taller than the area below the navbar: scroll natively between its top and bottom stops.
      if (section.offsetHeight > viewport - top0 + 1) {
        positions.add(bottomStop)
        freeRanges.push({ start: top, end: bottomStop })
      }
    })
    stops = [...positions].sort((a, b) => a - b)
  }

  function nearestIndex(y: number) {
    let best = 0
    stops.forEach((stop, index) => {
      if (Math.abs(stop - y) < Math.abs(stops[best]! - y)) best = index
    })
    return best
  }

  /** The tall section whose free range scrolls natively from `y` in this direction (not yet at its edge). */
  function nativeRange(y: number, direction: number) {
    return freeRanges.find(({ start, end }) =>
      direction > 0 ? y >= start - 1 && y < end - 1 : y > start + 1 && y <= end + 1)
  }

  function isInFreeRange(y: number) {
    return freeRanges.some(({ start, end }) => y > start + 1 && y < end - 1)
  }

  function overlayOpen() {
    return document.querySelector('[data-reka-popper-content-wrapper], [role="dialog"][data-state="open"]') !== null
  }

  function innerCanScroll(target: EventTarget | null, direction: number) {
    for (let el = target instanceof Element ? target : null; el && el !== document.body; el = el.parentElement) {
      const { overflowY } = getComputedStyle(el)
      if ((overflowY === 'auto' || overflowY === 'scroll') && el.scrollHeight > el.clientHeight) {
        if (direction > 0 ? el.scrollTop + el.clientHeight < el.scrollHeight - 1 : el.scrollTop > 0) return true
      }
    }
    return false
  }

  function scrollInstantly(top: number) {
    window.scrollTo({ top, behavior: 'instant' })
  }

  function glideTo(index: number) {
    cancelAnimationFrame(frame)
    targetIndex = index
    currentIndex.value = index
    if (reducedMotion.value === 'reduce') {
      scrollInstantly(stops[index]!)
      animationEndedAt = performance.now()
      return
    }
    const from = window.scrollY
    const startedAt = performance.now()
    animating = true
    const step = (now: number) => {
      const progress = Math.min((now - startedAt) / DURATION_MS, 1)
      // Read the target each frame so a layout change mid-glide still lands exactly.
      const to = stops[targetIndex]!
      scrollInstantly(from + (to - from) * EASING(progress))
      if (progress < 1) {
        frame = requestAnimationFrame(step)
      }
      else {
        animating = false
        animationEndedAt = performance.now()
      }
    }
    frame = requestAnimationFrame(step)
  }

  function move(direction: number) {
    // Cheap safety net: re-measure once per gesture in case a resize went unobserved.
    measure()
    const y = window.scrollY
    const index = direction > 0
      ? stops.findIndex(stop => stop > y + 1)
      : stops.filter(stop => stop < y - 1).length - 1
    if (index !== -1) glideTo(index)
  }

  function onWheel(event: WheelEvent) {
    if (event.ctrlKey || event.metaKey) return
    if (Math.abs(event.deltaX) > Math.abs(event.deltaY)) return
    const delta = event.deltaMode === 1
      ? event.deltaY * LINE_HEIGHT_PX
      : event.deltaMode === 2 ? event.deltaY * window.innerHeight : event.deltaY
    const direction = Math.sign(delta)
    if (!direction || overlayOpen() || innerCanScroll(event.target, direction)) return

    const now = performance.now()
    const gap = now - lastWheelAt
    const size = Math.abs(delta)
    const newGesture = gap > QUIET_MS || (size > lastDelta * NEW_GESTURE_RATIO && size >= NEW_GESTURE_MIN_DELTA)
    lastWheelAt = now
    lastDelta = size
    if (gap > QUIET_MS) nativeStream = false

    if (animating) {
      event.preventDefault()
      momentumLock = true
      return
    }
    if (momentumLock) {
      if (!newGesture) {
        event.preventDefault()
        return
      }
      momentumLock = false
    }

    const range = nativeRange(window.scrollY, direction)
    if (range) {
      accumulated = 0
      nativeStream = true
      // Stop exactly at the edge instead of overshooting into the neighbouring section.
      const next = window.scrollY + delta
      if (next < range.start || next > range.end) {
        event.preventDefault()
        scrollInstantly(Math.min(Math.max(next, range.start), range.end))
      }
      return
    }

    event.preventDefault()
    // Momentum from native scrolling inside a tall section reached its edge: wait for a new gesture.
    if (nativeStream && !newGesture) return
    nativeStream = false

    if (gap > QUIET_MS || Math.sign(accumulated) !== direction) accumulated = 0
    accumulated += delta
    if (Math.abs(accumulated) < WHEEL_THRESHOLD) return
    accumulated = 0
    momentumLock = true
    move(direction)
  }

  function onKeydown(event: KeyboardEvent) {
    if (event.defaultPrevented || event.altKey || event.ctrlKey || event.metaKey) return
    const focused = document.activeElement
    if (focused?.closest('input, textarea, select, [contenteditable=""], [contenteditable="true"]') || overlayOpen()) return

    let direction = KEY_DIRECTIONS[event.key]
    if (event.key === ' ') {
      // Space activates focused buttons and links; leave those alone.
      if (focused?.closest('button, a, [role="tab"], [tabindex]')) return
      direction = event.shiftKey ? -1 : 1
    }
    if (event.key === 'Home' || event.key === 'End') {
      event.preventDefault()
      if (!animating) {
        measure()
        glideTo(event.key === 'Home' ? 0 : stops.length - 1)
      }
      return
    }
    if (!direction) return
    if (nativeRange(window.scrollY, direction)) {
      nativeKeyScroll = true
      return
    }
    event.preventDefault()
    if (!animating) move(direction)
  }

  function onPointerdown(event: PointerEvent) {
    // The page scrollbar sits right of the document's client width.
    scrollbarDrag = event.clientX >= document.documentElement.clientWidth
  }

  function onScrollend() {
    // Our own frames also end in scrollend; ignore those.
    if (animating || performance.now() - animationEndedAt < 100) return
    const index = nearestIndex(window.scrollY)
    currentIndex.value = index
    // Scrollbar drags, and native key scrolls that overshot a tall section's edge, settle on a section.
    // Anchor links and find-in-page are left where they land.
    if (scrollbarDrag || nativeKeyScroll) {
      scrollbarDrag = false
      nativeKeyScroll = false
      if (!isInFreeRange(window.scrollY) && Math.abs(stops[index]! - window.scrollY) > 1) glideTo(index)
    }
  }

  function onLayoutChange() {
    measure()
    if (!animating && !isInFreeRange(window.scrollY)) scrollInstantly(stops[currentIndex.value]!)
  }

  function attach() {
    const sections = toValue(root)?.querySelectorAll<HTMLElement>('[data-scroll-section]')
    if (!sections?.length) return
    // Adopt wherever the page already is (e.g. after an anchor link), then realign on layout changes.
    measure()
    currentIndex.value = nearestIndex(window.scrollY)
    let initialCallback = true
    observer = new ResizeObserver(() => {
      if (initialCallback) initialCallback = false
      else onLayoutChange()
    })
    sections.forEach(section => observer!.observe(section))
    window.addEventListener('wheel', onWheel, { passive: false })
    window.addEventListener('keydown', onKeydown)
    window.addEventListener('pointerdown', onPointerdown)
    window.addEventListener('scrollend', onScrollend)
    window.addEventListener('resize', onLayoutChange)
  }

  function detach() {
    cancelAnimationFrame(frame)
    animating = false
    observer?.disconnect()
    observer = undefined
    window.removeEventListener('wheel', onWheel)
    window.removeEventListener('keydown', onKeydown)
    window.removeEventListener('pointerdown', onPointerdown)
    window.removeEventListener('scrollend', onScrollend)
    window.removeEventListener('resize', onLayoutChange)
  }

  watch([enabled, () => toValue(root)], ([isDesktop, element]) => {
    detach()
    if (isDesktop && element) attach()
  }, { immediate: true, flush: 'post' })
  onScopeDispose(detach)

  /** Back to the very top: eased on desktop, instant on mobile/tablet or with reduced motion. */
  function scrollToTop() {
    if (enabled.value) {
      glideTo(0)
    }
    else {
      currentIndex.value = 0
      scrollInstantly(0)
    }
  }

  return { enabled: readonly(enabled), currentIndex: readonly(currentIndex), scrollToTop }
}
