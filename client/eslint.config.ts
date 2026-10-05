import { globalIgnores } from 'eslint/config'
import { defineConfigWithVueTs, vueTsConfigs } from '@vue/eslint-config-typescript'
import pluginVue from 'eslint-plugin-vue'
import pluginOxlint from 'eslint-plugin-oxlint'
import betterTailwind from 'eslint-plugin-better-tailwindcss'

// To allow more languages other than `ts` in `.vue` files, uncomment the following lines:
// import { configureVueProject } from '@vue/eslint-config-typescript'
// configureVueProject({ scriptLangs: ['ts', 'tsx'] })
// More info at https://github.com/vuejs/eslint-config-typescript/#advanced-setup

// ── Neatly design-system guardrails (see DESIGN_SYSTEM.md → "Linting") ──────────
const HEX_COLOR = String.raw`#(?:[0-9a-fA-F]{3,4}|[0-9a-fA-F]{6}|[0-9a-fA-F]{8})\b`
const COLOR_FUNCTION = String.raw`\b(?:rgba?|hsla?|hwb|lab|lch|oklab|oklch)\(`
const COLOR_MESSAGE = 'Hard-coded colour. Use a Neatly token class (e.g. `text-orange-500`) or var(--color-*) from src/assets/tokens.css.'

const noColorLiterals = [HEX_COLOR, COLOR_FUNCTION].flatMap(pattern => [
  { selector: `Literal[value=/${pattern}/]`, message: COLOR_MESSAGE },
  { selector: `TemplateElement[value.raw=/${pattern}/]`, message: COLOR_MESSAGE },
])

export default defineConfigWithVueTs(
  {
    name: 'app/files-to-lint',
    files: ['**/*.{vue,ts,mts,tsx}'],
  },

  globalIgnores(['**/dist/**', '**/dist-ssr/**', '**/coverage/**']),

  ...pluginVue.configs['flat/essential'],
  vueTsConfigs.recommended,

  {
    name: 'app/design-system',
    files: ['src/**/*.{vue,ts}'],
    plugins: {
      'better-tailwindcss': betterTailwind,
    },
    settings: {
      'better-tailwindcss': {
        entryPoint: 'src/assets/main.css',
      },
    },
    rules: {
      // Classes must exist in the Tailwind theme (default palette/sizes are disabled in tokens.css).
      'better-tailwindcss/no-unknown-classes': 'error',
      // No arbitrary values: bg-[#fff], p-[13px], text-[18px], [mask-type:alpha] …
      'better-tailwindcss/no-restricted-classes': ['error', {
        restrict: [
          {
            pattern: String.raw`^.*\]!?$`,
            message: 'Arbitrary value "$0" is not allowed. Use a design token (see DESIGN_SYSTEM.md).',
          },
          {
            pattern: String.raw`^.*\((?!--)[^)]*\)!?$`,
            message: 'Arbitrary value "$0" is not allowed. Use a design token (see DESIGN_SYSTEM.md).',
          },
        ],
      }],
      'better-tailwindcss/no-conflicting-classes': 'error',
      'better-tailwindcss/no-duplicate-classes': 'error',
      'better-tailwindcss/no-deprecated-classes': 'error',
      // No colour literals in scripts…
      'no-restricted-syntax': ['error', ...noColorLiterals],
      // …or in templates (static attributes such as fill="#fff" / style="color: rgb()").
      'vue/no-restricted-syntax': ['error', ...noColorLiterals, {
        selector: `VLiteral[value=/${HEX_COLOR}|${COLOR_FUNCTION}/]`,
        message: COLOR_MESSAGE,
      }],
    },
  },

  {
    // shadcn-vue primitives use single-word names (Button, Input, …) by convention.
    name: 'app/ui-primitives',
    files: ['src/components/ui/**/*.vue'],
    rules: {
      'vue/multi-word-component-names': 'off',
    },
  },

  ...pluginOxlint.buildFromOxlintConfigFile('.oxlintrc.json'),
)
