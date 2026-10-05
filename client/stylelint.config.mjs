/**
 * Stylelint — enforces the Neatly design tokens in CSS and Vue <style> blocks.
 * See DESIGN_SYSTEM.md → "Linting".
 */

const COLOR_FUNCTIONS = ['rgb', 'rgba', 'hsl', 'hsla', 'hwb', 'lab', 'lch', 'oklab', 'oklch', 'color']

/** @type {import('stylelint').Config} */
export default {
  extends: [
    'stylelint-config-standard',
    '@dreamsicle.io/stylelint-config-tailwindcss',
    'stylelint-config-html/vue',
  ],
  rules: {
    // Allow Tailwind v4 theme syntax: sub-properties (--text-h1--line-height) and namespace resets (--color-*).
    'custom-property-pattern': [
      String.raw`^[a-z][a-z0-9]*(?:--?[a-z0-9]+)*(?:-\*)?$`,
      { message: name => `Custom property "${name}" should be kebab-case.` },
    ],
    // No hard-coded colours — use a token from src/assets/tokens.css.
    'color-no-hex': [true, { message: 'Hard-coded hex colour. Use a Neatly token (var(--color-*)) instead.' }],
    'color-named': ['never', { message: 'Named colour. Use a Neatly token (var(--color-*)) instead.' }],
    'function-disallowed-list': [COLOR_FUNCTIONS, { message: fn => `Colour function "${fn}()" is not allowed. Use a Neatly token instead.` }],
  },
  overrides: [
    {
      // The single place where colour literals are defined.
      files: ['src/assets/tokens.css'],
      rules: {
        'color-no-hex': null,
        'color-named': null,
        'function-disallowed-list': null,
      },
    },
  ],
}
