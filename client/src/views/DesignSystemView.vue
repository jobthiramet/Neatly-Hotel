<script setup lang="ts">
import type { DateValue } from '@internationalized/date'
import type { RoomStatus } from '@/components/ui/badge'
import { getLocalTimeZone, today } from '@internationalized/date'
import { onMounted, ref, shallowRef } from 'vue'
import ShowcaseSection from '@/components/design-system/ShowcaseSection.vue'
import StateLabel from '@/components/design-system/StateLabel.vue'
import { IconArrowRight, IconBooking, IconCash } from '@/components/icons'
import NeatlyLogo from '@/components/NeatlyLogo.vue'
import { Badge, roomStatusTone } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Calendar } from '@/components/ui/calendar'
import { Carousel, CarouselContent, CarouselItem, CarouselNext, CarouselPrevious } from '@/components/ui/carousel'
import { Checkbox, CheckboxLabel } from '@/components/ui/checkbox'
import { DatePicker } from '@/components/ui/date-picker'
import { Dialog, DialogClose, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle, DialogTrigger } from '@/components/ui/dialog'
import { FormField } from '@/components/ui/form-field'
import { Input } from '@/components/ui/input'
import { MenuLink } from '@/components/ui/menu-link'
import { PaymentOption } from '@/components/ui/payment-option'
import { RadioGroup } from '@/components/ui/radio-group'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Stepper, StepperIndicator, StepperItem, StepperTitle, StepperTrigger } from '@/components/ui/stepper'

const FIGMA_URL = 'https://www.figma.com/design/T5JCNQ0DkLasdfWiUjD8ct/Neatly?node-id=0-1'

// ── Tokens ────────────────────────────────────────────────────────────────
// Class names are listed in full so Tailwind can detect them.
const scale = ['100', '200', '300', '400', '500', '600', '700', '800', '900'] as const
const colorGroups = [
  { name: 'Green', figma: 'green', swatches: scale.map(s => ({ label: `green/${s}`, variable: `--color-green-${s}` })), classes: ['bg-green-100', 'bg-green-200', 'bg-green-300', 'bg-green-400', 'bg-green-500', 'bg-green-600', 'bg-green-700', 'bg-green-800', 'bg-green-900'] },
  { name: 'Orange', figma: 'orange', swatches: scale.map(s => ({ label: `orange/${s}`, variable: `--color-orange-${s}` })), classes: ['bg-orange-100', 'bg-orange-200', 'bg-orange-300', 'bg-orange-400', 'bg-orange-500', 'bg-orange-600', 'bg-orange-700', 'bg-orange-800', 'bg-orange-900'] },
  { name: 'Gray', figma: 'gray', swatches: scale.map(s => ({ label: `gray/${s}`, variable: `--color-gray-${s}` })), classes: ['bg-gray-100', 'bg-gray-200', 'bg-gray-300', 'bg-gray-400', 'bg-gray-500', 'bg-gray-600', 'bg-gray-700', 'bg-gray-800', 'bg-gray-900'] },
  {
    name: 'Utility',
    figma: 'utility',
    swatches: ['white', 'black', 'red', 'bg'].map(s => ({ label: `utility/${s}`, variable: `--color-${s}` })),
    classes: ['bg-white', 'bg-black', 'bg-red', 'bg-bg'],
  },
  {
    name: 'Room status (code-only)',
    figma: 'status badge — unbound',
    swatches: ['neutral', 'neutral-subtle', 'info', 'info-subtle', 'success', 'success-subtle', 'danger', 'danger-subtle', 'warning', 'warning-subtle'].map(s => ({ label: `status/${s}`, variable: `--color-status-${s}` })),
    classes: ['bg-status-neutral', 'bg-status-neutral-subtle', 'bg-status-info', 'bg-status-info-subtle', 'bg-status-success', 'bg-status-success-subtle', 'bg-status-danger', 'bg-status-danger-subtle', 'bg-status-warning', 'bg-status-warning-subtle'],
  },
]

const semanticTokens = ['primary', 'primary-foreground', 'secondary', 'secondary-foreground', 'muted', 'muted-foreground', 'accent', 'accent-foreground', 'destructive', 'border', 'input', 'ring', 'sidebar', 'sidebar-foreground']

// Resolved values are read at runtime so hex literals only live in tokens.css.
const resolved = ref<Record<string, string>>({})
onMounted(() => {
  const styles = getComputedStyle(document.documentElement)
  const names = [...colorGroups.flatMap(g => g.swatches.map(s => s.variable)), ...semanticTokens.map(t => `--${t}`), '--shadow-md', '--shadow-modal']
  resolved.value = Object.fromEntries(names.map(n => [n, styles.getPropertyValue(n).trim()]))
})

const typeScale = [
  { figma: 'headline1', utility: 'font-serif text-h1', spec: 'Noto Serif Display Medium · 88 / 1.25 · −2%', sample: 'Headline1', class: 'font-serif font-stretch-semi-condensed text-h1' },
  { figma: 'headline2', utility: 'font-serif text-h2', spec: 'Noto Serif Display Medium · 68 / 1.25 · −2%', sample: 'Headline2', class: 'font-serif font-stretch-semi-condensed text-h2' },
  { figma: 'headline3', utility: 'font-serif text-h3', spec: 'Noto Serif Display Medium · 44 / 1.25 · −2%', sample: 'Headline3', class: 'font-serif font-stretch-semi-condensed text-h3' },
  { figma: 'headline4', utility: 'text-h4', spec: 'Inter SemiBold · 28 / 1.5 · −2%', sample: 'Headline4', class: 'text-h4' },
  { figma: 'headline5', utility: 'text-h5', spec: 'Inter SemiBold · 20 / 1.5 · −2%', sample: 'Headline5', class: 'text-h5' },
  { figma: 'body1', utility: 'text-body1', spec: 'Inter Regular · 16 / 1.5 · −2%', sample: 'Body1', class: 'text-body1' },
  { figma: 'body2', utility: 'text-body2', spec: 'Inter Medium · 14 / 1.5 · −2%', sample: 'Body2', class: 'text-body2' },
  { figma: 'body3', utility: 'text-body3', spec: 'Inter Medium · 12 / 1.5 · −2%', sample: 'Body3', class: 'text-body3' },
  { figma: '— (button label)', utility: 'font-button text-button', spec: 'Open Sans SemiBold · 16 / 16 · 0', sample: 'Button label', class: 'font-button text-button' },
]

const spacing = [
  { token: '1', px: 4, class: 'w-1' },
  { token: '2', px: 8, class: 'w-2' },
  { token: '3', px: 12, class: 'w-3' },
  { token: '4', px: 16, class: 'w-4' },
  { token: '6', px: 24, class: 'w-6' },
  { token: '8', px: 32, class: 'w-8' },
]

// ── Components ────────────────────────────────────────────────────────────
const buttonStates = [
  { label: 'Default', force: undefined, disabled: false },
  { label: 'Hover', force: 'hover', disabled: false },
  { label: 'Pressed', force: 'active', disabled: false },
  { label: 'Focus', force: 'focus', disabled: false },
  { label: 'Disabled', force: undefined, disabled: true },
] as const

const roomStatuses = Object.keys(roomStatusTone) as RoomStatus[]

const selectOptions = ['Superior Garden View', 'Deluxe', 'Premier Sea View', 'Supreme']
const selectedRoom = ref<string>()

const pickedDate = shallowRef<DateValue>()
// Current month so the “today” outline is visible next to a selected date.
const calendarDate = shallowRef<DateValue>(today(getLocalTimeZone()).add({ days: 5 }))

const paymentMethod = ref('cash')
const step = ref(2)
const steps = ['Basic Information', 'Special Request', 'Payment Method']
const agreed = ref(true)

const nav = [
  ['colors', 'Colors'],
  ['typography', 'Typography'],
  ['effects', 'Radius · Shadow · Spacing'],
  ['buttons', 'Buttons'],
  ['inputs', 'Inputs'],
  ['select', 'Select'],
  ['date-picker', 'Date picker'],
  ['checkbox', 'Checkbox'],
  ['dialog', 'Dialog'],
  ['badge', 'Status badge'],
  ['payment-option', 'Payment option'],
  ['stepper', 'Stepper'],
  ['menu-link', 'Menu link'],
  ['carousel', 'Carousel'],
  ['logo', 'Logo'],
] as const
</script>

<template>
  <div class="min-h-screen bg-bg text-gray-900">
    <header class="bg-white shadow-md">
      <div class="mx-auto flex max-w-7xl flex-wrap items-center justify-between gap-4 px-8 py-6">
        <div class="flex items-center gap-6">
          <NeatlyLogo />
          <div>
            <h1 class="text-h4">
              Design system
            </h1>
            <p class="text-body2 text-gray-700">
              Tokens and components from Figma — for developers and designers.
            </p>
          </div>
        </div>
        <Button as="a" variant="ghost" :href="FIGMA_URL" target="_blank" rel="noopener">
          Open in Figma <IconArrowRight />
        </Button>
      </div>
    </header>

    <div class="mx-auto flex max-w-7xl gap-8 px-8 py-8">
      <nav class="sticky top-8 hidden h-fit w-52 shrink-0 flex-col gap-1 lg:flex" aria-label="Sections">
        <a
          v-for="[id, label] in nav"
          :key="id"
          :href="`#${id}`"
          class="rounded-sm px-3 py-2 text-body2 text-gray-700 is-hover:bg-white is-hover:text-orange-500"
        >{{ label }}</a>
      </nav>

      <main class="flex min-w-0 flex-1 flex-col gap-8">
        <!-- ── Colors ─────────────────────────────────────────────── -->
        <ShowcaseSection id="colors" title="Colors" figma="colors (9:58)">
          <div class="flex flex-col gap-8">
            <div v-for="group in colorGroups" :key="group.name">
              <h3 class="mb-3 text-h5">
                {{ group.name }}
              </h3>
              <ul class="grid grid-cols-2 gap-4 sm:grid-cols-3 md:grid-cols-5 xl:grid-cols-9">
                <li v-for="(swatch, i) in group.swatches" :key="swatch.variable" class="flex flex-col gap-2">
                  <div class="h-14 rounded-sm border border-gray-300" :class="group.classes[i]" />
                  <div class="flex flex-col">
                    <span class="text-body2 text-gray-900">{{ swatch.label }}</span>
                    <code class="text-body3 break-all text-gray-700">{{ swatch.variable }}</code>
                    <code class="text-body3 text-gray-600 uppercase">{{ resolved[swatch.variable] }}</code>
                  </div>
                </li>
              </ul>
            </div>

            <div>
              <h3 class="mb-3 text-h5">
                shadcn-vue semantic aliases
              </h3>
              <div class="overflow-x-auto">
                <table class="w-full text-left text-body2">
                  <thead class="text-gray-600">
                    <tr>
                      <th class="py-2 pr-4 font-medium">
                        Variable
                      </th>
                      <th class="py-2 pr-4 font-medium">
                        Utility
                      </th>
                      <th class="py-2 font-medium">
                        Resolves to
                      </th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="token in semanticTokens" :key="token" class="border-t border-gray-300">
                      <td class="py-2 pr-4">
                        <code>--{{ token }}</code>
                      </td>
                      <td class="py-2 pr-4">
                        <code>bg-{{ token }} / text-{{ token }}</code>
                      </td>
                      <td class="py-2 text-gray-700">
                        <code class="uppercase">{{ resolved[`--${token}`] }}</code>
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        </ShowcaseSection>

        <!-- ── Typography ─────────────────────────────────────────── -->
        <ShowcaseSection id="typography" title="Typography" figma="typography (99:2304)">
          <ul class="flex flex-col divide-y divide-gray-300">
            <li v-for="style in typeScale" :key="style.figma" class="flex flex-wrap items-baseline justify-between gap-x-8 gap-y-1 py-4">
              <span :class="style.class">{{ style.sample }}</span>
              <span class="flex flex-col items-end text-right">
                <code class="text-body2 text-gray-900">{{ style.utility }}</code>
                <span class="text-body3 text-gray-600">{{ style.figma }} · {{ style.spec }}</span>
              </span>
            </li>
          </ul>
        </ShowcaseSection>

        <!-- ── Radius / shadow / spacing ──────────────────────────── -->
        <ShowcaseSection id="effects" title="Radius · Shadow · Spacing" figma="style (101:2378)">
          <div class="grid gap-8 md:grid-cols-3">
            <div class="flex flex-col gap-3">
              <h3 class="text-h5">
                Radius
              </h3>
              <div class="size-24 rounded-sm border border-gray-400 bg-gray-100" />
              <code class="text-body2">rounded-sm · --radius-sm · 4px</code>
              <div class="size-24 rounded-full border border-gray-400 bg-gray-100" />
              <code class="text-body2">rounded-full</code>
            </div>
            <div class="flex flex-col gap-3">
              <h3 class="text-h5">
                Shadow
              </h3>
              <div class="h-28 w-40 rounded-sm bg-white shadow-md" />
              <code class="text-body2">shadow-md</code>
              <span class="text-body3 text-gray-600">{{ resolved['--shadow-md'] }}</span>
              <div class="mt-2 h-28 w-40 rounded-sm bg-white shadow-modal" />
              <code class="text-body2">shadow-modal</code>
              <span class="text-body3 text-gray-600">{{ resolved['--shadow-modal'] }}</span>
            </div>
            <div class="flex flex-col gap-3">
              <h3 class="text-h5">
                Spacing (Tailwind 4px scale)
              </h3>
              <div v-for="space in spacing" :key="space.token" class="flex items-center gap-3">
                <div class="h-4 bg-orange-300" :class="space.class" />
                <code class="text-body2">{{ space.token }} · {{ space.px }}px</code>
              </div>
            </div>
          </div>
        </ShowcaseSection>

        <!-- ── Buttons ────────────────────────────────────────────── -->
        <ShowcaseSection id="buttons" title="Buttons" figma="button (99:2292)">
          <div class="-m-2 overflow-x-auto p-2">
            <table class="text-left">
              <thead>
                <tr>
                  <th class="pr-4 pb-4">
                    <StateLabel>variant</StateLabel>
                  </th>
                  <th v-for="state in buttonStates" :key="state.label" class="pr-4 pb-4">
                    <StateLabel>{{ state.label }}</StateLabel>
                  </th>
                </tr>
              </thead>
              <tbody>
                <tr>
                  <td class="pr-4 pb-6">
                    <code class="text-body2">primary</code>
                  </td>
                  <td v-for="state in buttonStates" :key="state.label" class="pr-4 pb-6">
                    <Button :data-force="state.force" :disabled="state.disabled">
                      Primary
                    </Button>
                  </td>
                </tr>
                <tr>
                  <td class="pr-4 pb-6">
                    <code class="text-body2">secondary</code>
                  </td>
                  <td v-for="state in buttonStates" :key="state.label" class="pr-4 pb-6">
                    <Button variant="secondary" :data-force="state.force" :disabled="state.disabled">
                      Secondary
                    </Button>
                  </td>
                </tr>
                <tr>
                  <td class="pr-4">
                    <code class="text-body2">ghost</code>
                  </td>
                  <td v-for="state in buttonStates" :key="state.label" class="pr-4">
                    <Button variant="ghost" :data-force="state.force" :disabled="state.disabled">
                      Ghost <IconArrowRight />
                    </Button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <p class="mt-4 text-body3 text-gray-600">
            Focus ring is code-only (Figma has no focus state).
          </p>
        </ShowcaseSection>

        <!-- ── Inputs ─────────────────────────────────────────────── -->
        <ShowcaseSection id="inputs" title="Inputs" figma="input style (12:342)">
          <div class="grid gap-x-12 gap-y-6 md:grid-cols-2">
            <FormField label="Normal" for="in-normal">
              <Input id="in-normal" placeholder="Place Holder" />
            </FormField>
            <FormField label="Focus" for="in-focus">
              <Input id="in-focus" placeholder="Place Holder" data-force="focus" />
            </FormField>
            <FormField label="Success" for="in-success">
              <Input id="in-success" default-value="Place Holder" />
            </FormField>
            <FormField label="Error" for="in-error" error="supporting text">
              <Input id="in-error" default-value="Place Holder" aria-invalid="true" aria-describedby="in-error-error" />
            </FormField>
            <FormField label="Disable" for="in-disabled" disabled>
              <Input id="in-disabled" placeholder="Place Holder" disabled />
            </FormField>
          </div>
        </ShowcaseSection>

        <!-- ── Select ─────────────────────────────────────────────── -->
        <ShowcaseSection id="select" title="Select" figma="input style / dropdown (12:349) · Drop Down - Service (101:2443)">
          <div class="grid gap-x-12 gap-y-6 md:grid-cols-2">
            <FormField label="Dropdown" for="sel-room">
              <Select v-model="selectedRoom">
                <SelectTrigger id="sel-room">
                  <SelectValue placeholder="Place Holder" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem v-for="option in selectOptions" :key="option" :value="option">
                    {{ option }}
                  </SelectItem>
                </SelectContent>
              </Select>
            </FormField>
            <FormField label="Dropdown (disabled)" for="sel-disabled" disabled>
              <Select disabled>
                <SelectTrigger id="sel-disabled">
                  <SelectValue placeholder="Place Holder" />
                </SelectTrigger>
              </Select>
            </FormField>
          </div>
          <p class="mt-4 text-body3 text-gray-600">
            Open the dropdown to see the menu. Item highlight / selected colours are code-only.
          </p>
        </ShowcaseSection>

        <!-- ── Date picker ────────────────────────────────────────── -->
        <ShowcaseSection id="date-picker" title="Date picker" figma="input style / date picker (12:365) · Date Picker (106:4311)">
          <div class="grid gap-x-12 gap-y-6 md:grid-cols-2">
            <FormField label="Date Picker" for="date">
              <DatePicker id="date" v-model="pickedDate" placeholder="Place Holder" />
            </FormField>
            <div class="flex flex-col gap-2">
              <StateLabel>Calendar (inline) — today outline · selected fill · hover · outside month 40%</StateLabel>
              <div class="w-fit overflow-hidden rounded-sm shadow-md">
                <Calendar v-model="calendarDate" />
              </div>
            </div>
          </div>
        </ShowcaseSection>

        <!-- ── Checkbox ───────────────────────────────────────────── -->
        <ShowcaseSection id="checkbox" title="Checkbox" figma="checkbox (51:853)">
          <div class="flex flex-wrap gap-12">
            <div class="flex flex-col gap-2">
              <StateLabel>Default</StateLabel>
              <div class="flex items-center gap-3">
                <Checkbox id="cb-default" />
                <CheckboxLabel for="cb-default">
                  Default
                </CheckboxLabel>
              </div>
            </div>
            <div class="flex flex-col gap-2">
              <StateLabel>Hover</StateLabel>
              <div class="flex items-center gap-3">
                <Checkbox id="cb-hover" data-force="hover" />
                <CheckboxLabel for="cb-hover">
                  Hover
                </CheckboxLabel>
              </div>
            </div>
            <div class="flex flex-col gap-2">
              <StateLabel>Checked</StateLabel>
              <div class="flex items-center gap-3">
                <Checkbox id="cb-checked" v-model="agreed" />
                <CheckboxLabel for="cb-checked">
                  Checked
                </CheckboxLabel>
              </div>
            </div>
            <div class="flex flex-col gap-2">
              <StateLabel>Focus</StateLabel>
              <div class="flex items-center gap-3">
                <Checkbox id="cb-focus" data-force="focus" />
                <CheckboxLabel for="cb-focus">
                  Focus
                </CheckboxLabel>
              </div>
            </div>
            <div class="flex flex-col gap-2">
              <StateLabel>Disabled</StateLabel>
              <div class="flex items-center gap-3">
                <Checkbox id="cb-disabled" disabled />
                <CheckboxLabel for="cb-disabled">
                  Disable
                </CheckboxLabel>
              </div>
            </div>
          </div>
        </ShowcaseSection>

        <!-- ── Dialog ─────────────────────────────────────────────── -->
        <ShowcaseSection id="dialog" title="Dialog" figma="modal (102:2219)">
          <Dialog>
            <DialogTrigger as-child>
              <Button variant="secondary">
                Open dialog
              </Button>
            </DialogTrigger>
            <DialogContent>
              <DialogHeader>
                <DialogTitle>Delete Room</DialogTitle>
              </DialogHeader>
              <DialogDescription>
                Are you sure you want to delete this room?
              </DialogDescription>
              <DialogFooter>
                <DialogClose as-child>
                  <Button variant="secondary">
                    Yes, I want to delete
                  </Button>
                </DialogClose>
                <DialogClose as-child>
                  <Button>No, I don’t</Button>
                </DialogClose>
              </DialogFooter>
            </DialogContent>
          </Dialog>
        </ShowcaseSection>

        <!-- ── Badge ──────────────────────────────────────────────── -->
        <ShowcaseSection id="badge" title="Status badge" figma="status (85:2138)">
          <div class="flex flex-wrap gap-4">
            <div v-for="status in roomStatuses" :key="status" class="flex flex-col gap-1">
              <Badge :status="status" />
              <StateLabel>tone: {{ roomStatusTone[status] }}</StateLabel>
            </div>
          </div>
        </ShowcaseSection>

        <!-- ── Payment option ─────────────────────────────────────── -->
        <ShowcaseSection id="payment-option" title="Payment option" figma="payment option card (32:1222)">
          <RadioGroup v-model="paymentMethod" class="flex flex-wrap gap-6" aria-label="Payment method">
            <div class="flex flex-col gap-2">
              <StateLabel>Selected</StateLabel>
              <PaymentOption value="cash">
                <IconCash /> Cash
              </PaymentOption>
            </div>
            <div class="flex flex-col gap-2">
              <StateLabel>Hover</StateLabel>
              <PaymentOption value="credit" data-force="hover">
                <IconCash /> Credit
              </PaymentOption>
            </div>
            <div class="flex flex-col gap-2">
              <StateLabel>Default</StateLabel>
              <PaymentOption value="cheque">
                <IconCash /> Cheque
              </PaymentOption>
            </div>
            <div class="flex flex-col gap-2">
              <StateLabel>Disabled</StateLabel>
              <PaymentOption value="wallet" disabled>
                <IconCash /> Wallet
              </PaymentOption>
            </div>
          </RadioGroup>
        </ShowcaseSection>

        <!-- ── Stepper ────────────────────────────────────────────── -->
        <ShowcaseSection id="stepper" title="Stepper" figma="step (35:1029)">
          <Stepper v-model="step" class="flex-col gap-6 md:flex-row md:gap-12">
            <StepperItem v-for="(title, i) in steps" :key="title" :step="i + 1">
              <StepperTrigger>
                <StepperIndicator>{{ i + 1 }}</StepperIndicator>
                <StepperTitle>{{ title }}</StepperTitle>
              </StepperTrigger>
            </StepperItem>
          </Stepper>
          <p class="mt-4 text-body3 text-gray-600">
            finish · current · none
          </p>
        </ShowcaseSection>

        <!-- ── Menu link ──────────────────────────────────────────── -->
        <ShowcaseSection id="menu-link" title="Menu link" figma="menu link (80:1381)">
          <div class="flex flex-wrap gap-6">
            <div class="flex flex-col gap-2">
              <StateLabel>Default</StateLabel>
              <MenuLink href="#menu-link">
                <IconBooking /> Menu link
              </MenuLink>
            </div>
            <div class="flex flex-col gap-2">
              <StateLabel>Hover</StateLabel>
              <MenuLink href="#menu-link" data-force="hover">
                <IconBooking /> Menu link
              </MenuLink>
            </div>
            <div class="flex flex-col gap-2">
              <StateLabel>Selected</StateLabel>
              <MenuLink href="#menu-link" active>
                <IconBooking /> Menu link
              </MenuLink>
            </div>
          </div>
        </ShowcaseSection>

        <!-- ── Carousel ───────────────────────────────────────────── -->
        <ShowcaseSection id="carousel" title="Carousel" figma="home / image slider (17:53)">
          <Carousel :opts="{ loop: true }" aria-label="Carousel demo" class="overflow-hidden rounded-sm">
            <CarouselContent>
              <CarouselItem v-for="n in 6" :key="n" class="basis-1/3">
                <div class="flex h-40 items-center justify-center rounded-sm bg-green-700 text-h4 text-white">
                  {{ n }}
                </div>
              </CarouselItem>
            </CarouselContent>
            <CarouselPrevious aria-label="Previous slide" />
            <CarouselNext aria-label="Next slide" />
          </Carousel>
        </ShowcaseSection>

        <!-- ── Logo ───────────────────────────────────────────────── -->
        <ShowcaseSection id="logo" title="Logo" figma="logo (12:7)">
          <div class="flex flex-wrap gap-6">
            <div class="flex flex-col gap-2">
              <StateLabel>variant="dark"</StateLabel>
              <div class="rounded-sm border border-gray-300 bg-white p-8">
                <NeatlyLogo />
              </div>
            </div>
            <div class="flex flex-col gap-2">
              <StateLabel>variant="light"</StateLabel>
              <div class="rounded-sm bg-green-800 p-8">
                <NeatlyLogo variant="light" />
              </div>
            </div>
          </div>
        </ShowcaseSection>
      </main>
    </div>
  </div>
</template>
