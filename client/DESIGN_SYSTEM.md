# Neatly Design System

The client UI is built from **design tokens** and **components** that mirror the Neatly Figma file.
Figma is the source of truth: if something isn't in Figma, add it there first.

- **Figma:** [Neatly → 🎨 DESIGN SYSTEM](https://www.figma.com/design/T5JCNQ0DkLasdfWiUjD8ct/Neatly?node-id=0-1)
- **Showcase:** run `npm run dev` and open **<http://localhost:5173/design-system>**. It shows every token and every component in every state.

Stack: Vue 3 · Tailwind CSS **v4** (CSS-first `@theme`, no `tailwind.config`) · [shadcn-vue](https://shadcn-vue.com) on [Reka UI](https://reka-ui.com). Light theme only.

---

## 1. Where things live

| What | Where |
| --- | --- |
| Token values (colors, fonts, text styles, radius, shadows) | [`src/assets/tokens.css`](src/assets/tokens.css) — **the only file allowed to contain color literals** |
| Tailwind wiring, shadcn aliases → utilities, state variants | [`src/assets/main.css`](src/assets/main.css) |
| Google Fonts `<link>` | [`index.html`](index.html) |
| UI components | [`src/components/ui/*`](src/components/ui) |
| Icons exported from Figma | [`src/components/icons/*`](src/components/icons) |
| Logo | [`src/components/NeatlyLogo.vue`](src/components/NeatlyLogo.vue) |
| Showcase page | [`src/views/DesignSystemView.vue`](src/views/DesignSystemView.vue) → `/design-system` |
| `cn()` helper (tailwind-merge that knows our tokens) | [`src/lib/utils.ts`](src/lib/utils.ts) |

## 2. Using tokens

Tokens are declared in `@theme`, so Tailwind generates a utility **and** a CSS variable for each one.
Names follow Figma: `orange/500` → `--color-orange-500` → `bg-orange-500`, `text-orange-500`, `border-orange-500` …

```vue
<!-- ✅ In templates, use utilities -->
<p class="text-body2 text-gray-700">Check-in</p>

<!-- ✅ In CSS, use the variables -->
<style scoped>
.map-pin { fill: var(--color-orange-500); }
</style>
```

Tailwind's default palette, font sizes, radii and shadows are **turned off** (`--color-*: initial` and so on).
`bg-blue-500`, `text-sm` or `shadow-lg` won't generate anything, and the linter flags them.
The spacing scale (`p-4`, `gap-6` …) is Tailwind's default 4px grid, which is what Figma uses.

### Colors

| Figma | CSS variable | Utility example | Value |
| --- | --- | --- | --- |
| `green/100` … `green/900` | `--color-green-100` … `--color-green-900` | `bg-green-800` | `#F1F5F3` `#E6EBE9` `#D5DFDA` `#ABC0B4` `#81A08F` `#5D7B6A` `#465C50` `#2F3E35` `#171F1B` |
| `orange/100` … `orange/900` | `--color-orange-*` | `bg-orange-600` | `#FAEDE8` `#F9DACE` `#F3B59C` `#ED906B` `#E76B39` `#C14817` `#803010` `#631F04` `#401808` |
| `gray/100` … `gray/900` | `--color-gray-*` | `text-gray-700` | `#F6F7FC` `#F1F2F6` `#E4E6ED` `#D6D9E4` `#C8CCDB` `#9AA1B9` `#646D89` `#424C6B` `#2A2E3F` |
| `utility/white` | `--color-white` | `bg-white` | `#FFFFFF` |
| `utility/black` | `--color-black` | `text-black` | `#000000` |
| `utility/red` | `--color-red` | `text-red` `border-red` | `#B61515` |
| `utility/bg` | `--color-bg` | `bg-bg` | `#F7F7FB` |
| *(code-only)* room status | `--color-status-{neutral,info,success,danger,warning}` and `…-subtle` | `bg-status-info-subtle text-status-info` | see `tokens.css` |

> The room-status colors are **not Figma variables**. They're unbound hex values on the Figma `status` component, kept in code only. Update them by hand if the design changes.

#### shadcn-vue semantic aliases

shadcn-vue components expect semantic names. They map onto the palette in `tokens.css`:

| Variable | → Token | | Variable | → Token |
| --- | --- | --- | --- | --- |
| `--background` | white | | `--muted` | gray-200 |
| `--foreground` | gray-900 | | `--muted-foreground` | gray-600 |
| `--primary` | orange-600 | | `--accent` | orange-100 |
| `--primary-foreground` | white | | `--accent-foreground` | orange-500 |
| `--secondary` | white | | `--destructive` | red |
| `--secondary-foreground` | orange-500 | | `--border` / `--input` | gray-400 |
| `--card` / `--popover` | white | | `--ring` | orange-500 |
| `--sidebar` | green-800 | | `--sidebar-primary` | green-600 |

Prefer the Neatly names (`bg-orange-600`) in new code. The aliases are there so shadcn-vue components you add later pick up the brand automatically.

### Typography

Fonts load from Google Fonts in `index.html` (with `preconnect` and `display=swap`).

| Family | Utility | Weights loaded |
| --- | --- | --- |
| Noto Serif Display (SemiCondensed, `wdth` 87.5) | `font-serif` | 500 |
| Inter | `font-sans` (default) | 400, 500, 600 |
| Open Sans | `font-button` | 600 |

| Figma style | Utility | Spec |
| --- | --- | --- |
| `headline1` | `font-serif text-h1` | 88px · line-height 1.25 · −2% · 500 |
| `headline2` | `font-serif text-h2` | 68px · 1.25 · −2% · 500 |
| `headline3` | `font-serif text-h3` | 44px · 1.25 · −2% · 500 |
| `headline4` | `text-h4` | 28px · 1.5 · −2% · 600 |
| `headline5` | `text-h5` | 20px · 1.5 · −2% · 600 |
| `body1` | `text-body1` | 16px · 1.5 · −2% · 400 |
| `body2` | `text-body2` | 14px · 1.5 · −2% · 500 |
| `body3` | `text-body3` | 12px · 1.5 · −2% · 500 |
| *(button label)* | `font-button text-button` | 16px · 16px · 0 · 600 |

`text-*` sets size, line height, letter spacing and weight. Set the family separately (`font-serif` for h1–h3).
You can still override pieces, e.g. `text-body1 font-medium`.

### Radius, shadow, spacing

| Token | Utility | Value | Figma |
| --- | --- | --- | --- |
| `--radius-sm` | `rounded-sm` | 4px | used by every component (no variable) |
| `--shadow-md` | `shadow-md` | `4px 4px 16px 0` black 8% | effect `shadow` |
| `--shadow-modal` | `shadow-modal` | `2px 2px 12px 0` `#403285` 12% | effect `shadow` on `modal` |
| spacing | `p-1` `p-2` `p-3` `p-4` `p-6` `p-8` | 4 · 8 · 12 · 16 · 24 · 32px | auto-layout paddings/gaps |

## 3. Components

Import from the component's folder:

```ts
import { Button } from '@/components/ui/button'
```

### Button — `button` (99:2292)

Variants: `primary` (default) · `secondary` · `ghost`. States: hover, pressed, focus (ring, code-only) and disabled.

```vue
<Button>Book now</Button>
<Button variant="secondary">Cancel</Button>
<Button variant="ghost">See all rooms <IconArrowRight /></Button>
<Button as-child><RouterLink to="/rooms">Rooms</RouterLink></Button>
```

### Input, Label, FormField — `input style` (12:342)

States: default · focus · success · error · disabled. Set `aria-invalid="true"` for the error state; the error icon appears automatically.

```vue
<FormField label="Email" for="email" :error="errors.email">
  <Input id="email" v-model="email" type="email" placeholder="Enter your email"
         :aria-invalid="!!errors.email" aria-describedby="email-error" />
</FormField>
```

### Select — `input style / dropdown` (12:349)

```vue
<FormField label="Room type" for="room-type">
  <Select v-model="roomType">
    <SelectTrigger id="room-type"><SelectValue placeholder="Select room type" /></SelectTrigger>
    <SelectContent>
      <SelectItem value="deluxe">Deluxe</SelectItem>
      <SelectItem value="superior">Superior</SelectItem>
    </SelectContent>
  </Select>
</FormField>
```

### DatePicker and Calendar — `input style / date picker` (12:365), `Date Picker` (106:4311)

Values are `@internationalized/date` objects. Store them with `shallowRef`.

```vue
<script setup lang="ts">
import type { DateValue } from '@internationalized/date'
const checkIn = shallowRef<DateValue>()
</script>

<FormField label="Check In" for="check-in">
  <DatePicker id="check-in" v-model="checkIn" placeholder="Select date" />
</FormField>
```

### Checkbox — `checkbox` (51:853)

```vue
<div class="flex items-center gap-3">
  <Checkbox id="terms" v-model="accepted" />
  <CheckboxLabel for="terms">I accept the terms</CheckboxLabel>
</div>
```

### Dialog — `modal` (102:2219)

```vue
<Dialog v-model:open="open">
  <DialogContent>
    <DialogHeader><DialogTitle>Delete Room</DialogTitle></DialogHeader>
    <DialogDescription>Are you sure you want to delete this room?</DialogDescription>
    <DialogFooter>
      <DialogClose as-child><Button variant="secondary">Yes, I want to delete</Button></DialogClose>
      <DialogClose as-child><Button>No, I don’t</Button></DialogClose>
    </DialogFooter>
  </DialogContent>
</Dialog>
```

### Badge — `status` (85:2138)

```vue
<Badge status="Vacant Clean" />          <!-- tone + label from the room status -->
<Badge tone="warning">Pending</Badge>    <!-- neutral | info | success | danger | warning | vacant -->
```

### PaymentOption — `payment option card` (32:1222)

```vue
<RadioGroup v-model="method" class="flex gap-6" aria-label="Payment method">
  <PaymentOption value="credit"><IconCash /> Credit Card</PaymentOption>
  <PaymentOption value="cash"><IconCash /> Cash</PaymentOption>
</RadioGroup>
```

### Stepper — `step` (35:1029)

```vue
<Stepper v-model="step">
  <StepperItem v-for="(title, i) in steps" :key="title" :step="i + 1">
    <StepperTrigger>
      <StepperIndicator>{{ i + 1 }}</StepperIndicator>
      <StepperTitle>{{ title }}</StepperTitle>
    </StepperTrigger>
  </StepperItem>
</Stepper>
```

### MenuLink — `menu link` (80:1381), admin sidebar

```vue
<MenuLink as-child :active="route.name === 'bookings'">
  <RouterLink to="/admin/bookings"><IconBooking /> Customer Booking</RouterLink>
</MenuLink>
```

### Carousel — `home / image slider` (17:53)

shadcn-vue carousel on [Embla](https://www.embla-carousel.com). Pass Embla options via `opts`.
Arrows are white outline circles with a translucent black fill so they stay visible over photos. Arrow keys work when the carousel has focus.
With `loop: true`, the slides must overflow the viewport (duplicate short lists).

```vue
<Carousel :opts="{ loop: true, align: 'center' }" aria-label="Hotel photos">
  <CarouselContent>
    <CarouselItem v-for="photo in photos" :key="photo.src" class="basis-auto">
      <img :src="photo.src" :alt="photo.alt">
    </CarouselItem>
  </CarouselContent>
  <CarouselPrevious aria-label="Previous image" />
  <CarouselNext aria-label="Next image" />
</Carousel>
```

### NeatlyLogo — `logo` (12:7)

```vue
<NeatlyLogo />                  <!-- on light backgrounds -->
<NeatlyLogo variant="light" />  <!-- on green-800 -->
```

### Forcing states (for review)

Components use `is-hover:`, `is-active:` and `is-focus:` instead of `hover:`, `active:` and `focus-visible:`.
They behave the same, but also respond to a `data-force` attribute. The showcase uses this to render every state at once:

```vue
<Button data-force="hover">Primary</Button>
```

Don't use `data-force` in product code.

## 4. Rules

1. **No hard-coded colors.** No hex, `rgb()`, `hsl()` or `oklch()` values, and no named colors, in `.vue`, `.ts` or `.css`. The only exception is `src/assets/tokens.css`.
2. **No arbitrary values.** `bg-[#fff]`, `p-[13px]`, `text-[18px]` and `[mask:…]` aren't allowed. CSS-variable shorthands such as `max-h-(--reka-select-content-available-height)` are fine.
3. **Only theme classes.** Classes Tailwind doesn't know (like `text-sm`) fail the lint.
4. **New tokens go into Figma first**, then into `tokens.css`, using the Figma name. If a designer hasn't defined it, ask before inventing one.
5. **Icons come from Figma.** Export the SVG, replace its fills and strokes with `currentColor`, and add it to `src/components/icons`. Color icons with `text-*` utilities.

## 5. Adding a component

1. Check Figma for the component and **all** its variants and states.
2. If shadcn-vue has a close match, scaffold it:
   ```bash
   npx shadcn-vue@latest add tooltip
   ```
   Otherwise create `src/components/ui/<name>/<Name>.vue` plus an `index.ts` barrel.
3. Replace the generated classes with Neatly tokens. Use `rounded-sm`, `text-body*`, `shadow-md`, and `is-hover:`/`is-active:`/`is-focus:` for states. Remove `dark:` classes (light theme only).
4. Add a comment naming the Figma node, e.g. `<!-- Figma: tooltip (123:456) -->`.
5. Add a section to `DesignSystemView.vue` showing every variant and state, and compare it with a Figma screenshot. Check sizes: Figma strokes are **inside** the box, while CSS borders add to it, so subtract 1px of padding per bordered side.
6. Document it in section 3 above.
7. Run `npm run lint` and `npm run build`.

## 6. Linting

```bash
npm run lint           # oxlint → eslint → stylelint (all with the rules below)
npm run lint:eslint    # .vue / .ts
npm run lint:style     # .css and <style> blocks in .vue
```

| Tool | Rule | Catches |
| --- | --- | --- |
| ESLint · `eslint-plugin-better-tailwindcss` | `no-unknown-classes` | classes not in the theme (`bg-blue-500`, `text-sm`) |
| | `no-restricted-classes` | arbitrary values (`bg-[#fff]`, `p-[13px]`) |
| | `no-conflicting-classes`, `no-duplicate-classes`, `no-deprecated-classes` | class hygiene |
| ESLint core | `no-restricted-syntax` | color literals in scripts (`'#E76B39'`, `` `rgba(…)` ``) |
| `eslint-plugin-vue` | `vue/no-restricted-syntax` | color literals in templates (`fill="#fff"`) |
| Stylelint | `color-no-hex`, `color-named`, `function-disallowed-list` | color literals in CSS (exempt: `tokens.css`) |

Config: [`eslint.config.ts`](eslint.config.ts), [`stylelint.config.mjs`](stylelint.config.mjs).

## 7. Known Figma inconsistencies

Found while building the system. Worth fixing in Figma:

- `gray/900` (#2A2E3F) and `Gray/900` (#323640) are two variables with different values. Code uses #2A2E3F.
- `utility/white` and `Utility/White` are duplicates.
- `Neutral/900` and `Neutral/0` are used only on the Focus/Error input labels. Code maps them to gray-900 and white.
- Two effect styles are both named `shadow` (card and modal). The date picker uses a third, unbound shadow; code uses `shadow-md` for it.
- The room-status badge colors are unbound hex values, the variants are unnamed (`Variant2`…`Variant13`), and two backgrounds are near-duplicates (#F0F2F8 and #F0F1F8). Code merges them.
- Buttons use Open Sans SemiBold 16 with no text style. Everything else uses Inter.
- `body4` (Nunito) is defined but only used on hidden date-picker cells. It isn't loaded.
- Success and Error inputs show value text in black; other states use gray-900.
- Disabled backgrounds differ: gray-200 for inputs, gray-100 for checkboxes.
- The dark logo has one path in green-800; the rest are green-700. Code renders the whole wordmark in green-700.
- No focus states are drawn for any component. Code adds an orange-500 focus ring.
- In the color frame, the green and orange palette layers are both named "dark blue palette".
