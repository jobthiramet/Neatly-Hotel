<script setup lang="ts">
import type { WithClassAsProps } from './interface'
import { IconArrowRight } from '@/components/icons'
import { Button } from '@/components/ui/button'
import { cn } from '@/lib/utils'
import { useCarousel } from './useCarousel'

const props = defineProps<WithClassAsProps>()

const { orientation, canScrollNext, scrollNext } = useCarousel()
</script>

<!--
  Figma: home / image slider arrow (17:52). White outline circle; the translucent
  black fill is code-only so the arrow stays visible over light photos.
-->
<template>
  <Button
    data-slot="carousel-next"
    variant="ghost"
    size="icon"
    :disabled="!canScrollNext"
    :class="cn(
      'absolute size-10 touch-manipulation rounded-full border-white bg-black/40 text-white is-hover:bg-black/60 is-hover:text-white lg:size-14 [&_svg]:size-4 lg:[&_svg]:size-6',
      orientation === 'horizontal'
        ? 'top-1/2 right-4 -translate-y-1/2'
        : '-bottom-12 left-1/2 -translate-x-1/2 rotate-90',
      props.class,
    )"
    @click="scrollNext"
  >
    <slot>
      <IconArrowRight />
      <span class="sr-only">Next slide</span>
    </slot>
  </Button>
</template>
