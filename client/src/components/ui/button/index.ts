import type { VariantProps } from 'class-variance-authority'
import { cva } from 'class-variance-authority'

export { default as Button } from './Button.vue'

// Figma: button primary (12:99), button secondary (64:2291), button / ghost (17:131)
export const buttonVariants = cva(
  'group/button inline-flex shrink-0 cursor-pointer items-center justify-center gap-2 rounded-sm border border-transparent font-button text-button whitespace-nowrap transition-colors outline-none select-none is-focus:ring-2 is-focus:ring-ring is-focus:ring-offset-2 disabled:cursor-not-allowed [&_svg]:pointer-events-none [&_svg]:shrink-0',
  {
    variants: {
      variant: {
        primary: 'bg-orange-600 text-white is-hover:bg-orange-500 is-active:bg-orange-700 disabled:bg-gray-300 disabled:text-gray-600',
        secondary: 'border-orange-500 bg-white text-orange-500 is-hover:border-orange-400 is-hover:text-orange-400 is-active:border-orange-600 is-active:text-orange-600 disabled:border-gray-400 disabled:text-gray-400',
        ghost: 'text-orange-500 is-hover:text-orange-400 is-active:text-orange-600 disabled:text-gray-500',
      },
      size: {
        default: 'px-7.75 py-3.75', // 32/16 in Figma minus the 1px border
        icon: 'size-10 text-gray-500 is-hover:text-gray-700 [&_svg]:size-6',
      },
    },
    compoundVariants: [
      { variant: 'ghost', size: 'default', class: 'px-1.75 py-0.75 [&_svg]:size-4' },
    ],
    defaultVariants: {
      variant: 'primary',
      size: 'default',
    },
  },
)
export type ButtonVariants = VariantProps<typeof buttonVariants>
