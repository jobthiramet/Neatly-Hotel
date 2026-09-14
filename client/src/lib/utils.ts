import type { ClassValue } from "clsx"
import { clsx } from "clsx"
import { extendTailwindMerge } from "tailwind-merge"

// Teach tailwind-merge the Neatly theme keys (src/assets/tokens.css) so that
// e.g. `text-h5` (font size) and `text-gray-700` (color) don't cancel each other out.
const twMerge = extendTailwindMerge({
  extend: {
    theme: {
      text: ["h1", "h2", "h3", "h4", "h5", "body1", "body2", "body3", "button"],
      shadow: ["md", "modal"],
      font: ["sans", "serif", "button"],
    },
  },
})

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}
