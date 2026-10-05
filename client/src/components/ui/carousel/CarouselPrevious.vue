<script setup lang="ts">
import type { WithClassAsProps } from './interface'
import { IconArrowRight } from '@/components/icons'
import { Button } from '@/components/ui/button'
import { cn } from '@/lib/utils'
import { useCarousel } from './useCarousel'

const props = defineProps<WithClassAsProps>()

const { orientation, canScrollPrev, scrollPrev } = useCarousel()
</script>

<!--
  Figma: home / image slider arrow (17:54). White outline circle; the translucent
  black fill is code-only so the arrow stays visible over light photos.
-->
<template>
  <Button
    data-slot="carousel-previous"
    variant="ghost"
    size="icon"
    :disabled="!canScrollPrev"
    :class="cn(
      'absolute size-10 touch-manipulation rounded-full border-white bg-black/40 text-white is-hover:bg-black/60 is-hover:text-white lg:size-14 [&_svg]:size-4 lg:[&_svg]:size-6',
      orientation === 'horizontal'
        ? 'top-1/2 left-4 -translate-y-1/2'
        : '-top-12 left-1/2 -translate-x-1/2 rotate-90',
      props.class,
    )"
    @click="scrollPrev"
  >
    <slot>
      <IconArrowRight class="rotate-180" />
      <span class="sr-only">Previous slide</span>
    </slot>
  </Button>
</template>
