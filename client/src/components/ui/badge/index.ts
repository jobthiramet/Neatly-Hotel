import type { VariantProps } from 'class-variance-authority'
import { cva } from 'class-variance-authority'

export { default as Badge } from './Badge.vue'

// Figma: status (85:2138). Colours are code-only tokens — see tokens.css "room status".
export const badgeVariants = cva(
  'inline-flex w-fit shrink-0 items-center rounded-sm px-3 py-1 text-body2 whitespace-nowrap',
  {
    variants: {
      tone: {
        neutral: 'bg-status-neutral-subtle text-status-neutral',
        info: 'bg-status-info-subtle text-status-info',
        success: 'bg-status-success-subtle text-status-success',
        danger: 'bg-status-danger-subtle text-status-danger',
        warning: 'bg-status-warning-subtle text-status-warning',
        vacant: 'bg-status-neutral-subtle text-status-success',
      },
    },
    defaultVariants: {
      tone: 'neutral',
    },
  },
)
export type BadgeVariants = VariantProps<typeof badgeVariants>

/** Room statuses from the Figma `status` component, in Figma order. */
export const roomStatusTone = {
  'Vacant': 'vacant',
  'Occupied': 'info',
  'Assign Clean': 'success',
  'Assign Dirty': 'danger',
  'Vacant Clean': 'success',
  'Vacant Clean Inspected': 'warning',
  'Vacant Clean Pick Up': 'success',
  'Occupied Clean': 'info',
  'Occupied Clean Inspected': 'warning',
  'Occupied Dirty': 'danger',
  'Out of Order': 'neutral',
  'Out of Service': 'neutral',
  'Out of Inventory': 'neutral',
} as const satisfies Record<string, NonNullable<BadgeVariants['tone']>>

export type RoomStatus = keyof typeof roomStatusTone
