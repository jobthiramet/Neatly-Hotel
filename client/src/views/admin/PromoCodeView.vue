<script setup lang="ts">
import type { DiscountType, PromotionCode } from '@/stores/promotionCodes'
import { isAxiosError } from 'axios'
import { RadioGroupIndicator, RadioGroupItem } from 'reka-ui'
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { toast } from 'vue-sonner'
import { IconEdit, IconTrash } from '@/components/icons'
import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogClose,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { FormField } from '@/components/ui/form-field'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { MultiSelect } from '@/components/ui/multi-select'
import { RadioGroup } from '@/components/ui/radio-group'
import { apiErrorMessage } from '@/stores/hotel'
import { usePromotionCodesStore } from '@/stores/promotionCodes'
import { apiFieldErrors, useRoomsStore } from '@/stores/rooms'

const PAGE_SIZE = 10

const promotionCodes = usePromotionCodesStore()
const roomsStore = useRoomsStore()

const search = ref('')
const page = ref(1)
const formOpen = ref(false)
const formMode = ref<'create' | 'edit'>('create')
const editingId = ref<string | null>(null)
const deleteOpen = ref(false)
const deletingCode = ref<PromotionCode | null>(null)
const saving = ref(false)
const deleting = ref(false)

const form = reactive({
  code: '',
  minPurchase: '',
  discountType: 'FIXED' as DiscountType,
  amountOff: '',
  percentOff: '',
  roomTypeIds: [] as string[],
})

const errors = reactive<Partial<Record<'code' | 'minPurchase' | 'discount', string>>>({})

const money = new Intl.NumberFormat('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })

const roomTypeOptions = computed(() =>
  roomsStore.roomTypes.map(type => ({ value: type.id, label: type.name })),
)

const formTitle = computed(() => formMode.value === 'create' ? 'Create promo code' : 'Edit promo code')
const submitLabel = computed(() => {
  if (saving.value)
    return formMode.value === 'create' ? 'Creating…' : 'Saving…'
  return formMode.value === 'create' ? 'Create' : 'Save'
})

const filteredCodes = computed(() => {
  const q = search.value.trim().toLowerCase()
  if (!q)
    return promotionCodes.codes
  return promotionCodes.codes.filter(promo =>
    promo.code.toLowerCase().includes(q)
    || discountLabel(promo).toLowerCase().includes(q)
    || roomTypesLabel(promo).toLowerCase().includes(q),
  )
})

const totalPages = computed(() => Math.max(1, Math.ceil(filteredCodes.value.length / PAGE_SIZE)))

const pagedCodes = computed(() => {
  const start = (page.value - 1) * PAGE_SIZE
  return filteredCodes.value.slice(start, start + PAGE_SIZE)
})

const pageNumbers = computed(() => {
  const total = totalPages.value
  const current = page.value
  const windowSize = 5
  let start = Math.max(1, current - Math.floor(windowSize / 2))
  const end = Math.min(total, start + windowSize - 1)
  start = Math.max(1, end - windowSize + 1)
  return Array.from({ length: end - start + 1 }, (_, i) => start + i)
})

watch(search, () => {
  page.value = 1
})

watch(filteredCodes, () => {
  if (page.value > totalPages.value)
    page.value = totalPages.value
})

onMounted(async () => {
  const results = await Promise.allSettled([
    promotionCodes.fetchAll(),
    roomsStore.types(),
  ])
  const roomTypes = results[1]
  if (roomTypes.status === 'rejected')
    toast.error(apiErrorMessage(roomTypes.reason, 'Could not load room types.'))
})

function discountLabel(promo: PromotionCode) {
  if (promo.discountType === 'PERCENT')
    return `${formatPercent(promo.percentOff)}%`
  return `${money.format(promo.amountOff ?? 0)} THB`
}

function formatPercent(value: number | null) {
  if (value == null)
    return '0'
  return Number.isInteger(value) ? String(value) : money.format(value)
}

function roomTypesLabel(promo: PromotionCode) {
  if (promo.roomTypes.length === 0)
    return 'All room types'
  return promo.roomTypes.map(type => type.name).join(', ')
}

function roomTypeCountLabel(count: number) {
  return `${count} room types`
}

function resetForm() {
  form.code = ''
  form.minPurchase = ''
  form.discountType = 'FIXED'
  form.amountOff = ''
  form.percentOff = ''
  form.roomTypeIds = []
  errors.code = undefined
  errors.minPurchase = undefined
  errors.discount = undefined
  editingId.value = null
}

function openCreate() {
  formMode.value = 'create'
  resetForm()
  formOpen.value = true
}

function openEdit(promo: PromotionCode) {
  formMode.value = 'edit'
  editingId.value = promo.id
  form.code = promo.code
  form.minPurchase = String(promo.minPurchaseAmount)
  form.discountType = promo.discountType
  form.amountOff = promo.discountType === 'FIXED' && promo.amountOff != null ? String(promo.amountOff) : ''
  form.percentOff = promo.discountType === 'PERCENT' && promo.percentOff != null ? String(promo.percentOff) : ''
  form.roomTypeIds = promo.roomTypes.map(type => type.id)
  errors.code = undefined
  errors.minPurchase = undefined
  errors.discount = undefined
  formOpen.value = true
}

function openDelete(promo: PromotionCode) {
  deletingCode.value = promo
  deleteOpen.value = true
}

function onCodeInput(value: string | number) {
  form.code = String(value ?? '').toUpperCase().replace(/[^A-Z0-9-]/g, '').slice(0, 40)
  errors.code = undefined
}

function onDecimalInput(field: 'minPurchase' | 'amountOff' | 'percentOff', value: string | number) {
  form[field] = decimalText(value)
  if (field === 'minPurchase')
    errors.minPurchase = undefined
  else
    errors.discount = undefined
}

function onAmountFocus(type: DiscountType) {
  form.discountType = type
  errors.discount = undefined
}

function onDiscountType(value: string | number | bigint | Record<string, unknown> | null) {
  const next = value == null ? '' : String(value)
  if (next === 'FIXED' || next === 'PERCENT')
    form.discountType = next
  errors.discount = undefined
}

function decimalText(value: string | number) {
  const raw = String(value ?? '').replace(/[^\d.]/g, '')
  const dot = raw.indexOf('.')
  if (dot === -1)
    return raw
  return `${raw.slice(0, dot)}.${raw.slice(dot + 1).replace(/\./g, '').slice(0, 2)}`
}

function validate() {
  const code = form.code.trim()
  if (!code)
    errors.code = 'Promo code is required.'
  else if (!/^[A-Z0-9-]{1,40}$/.test(code))
    errors.code = 'Use letters, numbers, or hyphens.'
  else
    errors.code = undefined

  const min = Number(form.minPurchase)
  errors.minPurchase = form.minPurchase.trim() && Number.isFinite(min) && min >= 0
    ? undefined
    : 'Minimum purchase amount is required.'

  if (form.discountType === 'FIXED') {
    const amount = Number(form.amountOff)
    errors.discount = form.amountOff.trim() && Number.isFinite(amount) && amount > 0
      ? undefined
      : 'Fixed amount must be greater than 0.'
  }
  else {
    const percent = Number(form.percentOff)
    errors.discount = form.percentOff.trim() && Number.isFinite(percent) && percent > 0 && percent <= 100
      ? undefined
      : 'Percent must be greater than 0 and at most 100.'
  }

  const valid = !errors.code && !errors.minPurchase && !errors.discount
  if (!valid)
    focusFirstError()
  return valid
}

function focusFirstError() {
  const id = errors.code
    ? 'promo-code'
    : errors.minPurchase
      ? 'min-purchase'
      : form.discountType === 'PERCENT'
        ? 'discount-percent-amount'
        : 'discount-amount'
  document.getElementById(id)?.focus()
}

async function submitForm() {
  if (!validate())
    return
  saving.value = true
  const payload = {
    code: form.code.trim(),
    discountType: form.discountType,
    amountOff: form.discountType === 'FIXED' ? Number(form.amountOff) : null,
    percentOff: form.discountType === 'PERCENT' ? Number(form.percentOff) : null,
    minPurchaseAmount: Number(form.minPurchase),
    roomTypeIds: [...form.roomTypeIds],
  }
  try {
    if (formMode.value === 'create') {
      await promotionCodes.create(payload)
      toast.success('Promo code created')
    }
    else if (editingId.value) {
      await promotionCodes.update(editingId.value, payload)
      toast.success('Promo code updated')
    }
    formOpen.value = false
    resetForm()
  }
  catch (e) {
    const fieldErrors = apiFieldErrors(e)
    errors.code = fieldErrors.code
    errors.minPurchase = fieldErrors.minPurchaseAmount
    errors.discount = fieldErrors.amountOff ?? fieldErrors.percentOff ?? fieldErrors.discountType
    if (isAxiosError(e) && e.response?.status === 409)
      errors.code = apiErrorMessage(e, 'Promo code already exists.')
    if (errors.code || errors.minPurchase || errors.discount)
      focusFirstError()
    toast.error(apiErrorMessage(e, formMode.value === 'create' ? 'Could not create promo code.' : 'Could not update promo code.'))
  }
  finally {
    saving.value = false
  }
}

async function confirmDelete() {
  if (!deletingCode.value)
    return
  deleting.value = true
  try {
    await promotionCodes.remove(deletingCode.value.id)
    deleteOpen.value = false
    deletingCode.value = null
    toast.success('Promo code deleted')
  }
  catch (e) {
    toast.error(apiErrorMessage(e, 'Could not delete promo code.'))
  }
  finally {
    deleting.value = false
  }
}
</script>

<template>
  <Teleport defer to="#admin-header-actions">
    <Input
      v-model="search"
      type="search"
      placeholder="Search..."
      aria-label="Search promo codes"
      class="w-80"
    />
    <Button type="button" @click="openCreate">
      + Create Promo code
    </Button>
  </Teleport>

  <div
    v-if="promotionCodes.loading"
    role="status"
    class="rounded-sm bg-white px-6 py-10 text-body1 text-gray-700"
  >
    Loading promo codes…
  </div>

  <div
    v-else-if="promotionCodes.error"
    role="alert"
    class="flex flex-col items-start gap-4 rounded-sm bg-white px-6 py-10"
  >
    <p class="text-body1 text-red">
      {{ promotionCodes.error }}
    </p>
    <Button variant="secondary" @click="promotionCodes.fetchAll()">
      Try again
    </Button>
  </div>

  <template v-else>
    <div class="overflow-x-auto rounded-sm bg-white">
      <table class="w-full min-w-200 table-fixed text-left text-body2 text-black">
        <colgroup>
          <col class="w-1/5">
          <col>
          <col>
          <col class="w-2/5">
          <col class="w-28">
        </colgroup>
        <thead class="bg-gray-300 text-gray-800">
          <tr>
            <th scope="col" class="px-4 py-2.5 font-medium">
              Promo code
            </th>
            <th scope="col" class="px-4 py-2.5 font-medium">
              Discount
            </th>
            <th scope="col" class="px-4 py-2.5 font-medium">
              Min. purchase
            </th>
            <th scope="col" class="px-4 py-2.5 font-medium">
              Room types
            </th>
            <th scope="col" class="px-4 py-2.5 font-medium">
              Update
            </th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="pagedCodes.length === 0">
            <td colspan="5" class="px-4 py-10 text-center text-body1 text-gray-700">
              No promo codes found. Create the first promo code to get started.
            </td>
          </tr>
          <tr
            v-for="promo in pagedCodes"
            :key="promo.id"
            class="border-t border-gray-200 is-hover:bg-gray-100"
          >
            <td class="px-4 py-4">
              {{ promo.code }}
            </td>
            <td class="px-4 py-4 tabular-nums">
              {{ discountLabel(promo) }}
            </td>
            <td class="px-4 py-4 tabular-nums">
              {{ money.format(promo.minPurchaseAmount) }} THB
            </td>
            <td class="px-4 py-4">
              <span class="line-clamp-2" :title="roomTypesLabel(promo)">{{ roomTypesLabel(promo) }}</span>
            </td>
            <td class="px-4 py-4">
              <div class="flex items-center gap-1">
                <button
                  type="button"
                  class="flex size-8 items-center justify-center text-orange-500 outline-none is-hover:text-orange-600 is-focus:ring-2 is-focus:ring-ring"
                  :aria-label="`Edit promo code ${promo.code}`"
                  @click="openEdit(promo)"
                >
                  <IconEdit class="size-5" />
                </button>
                <button
                  type="button"
                  class="flex size-8 items-center justify-center text-red outline-none is-hover:text-red is-focus:ring-2 is-focus:ring-ring"
                  :aria-label="`Delete promo code ${promo.code}`"
                  @click="openDelete(promo)"
                >
                  <IconTrash class="size-5" />
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <nav
      v-if="filteredCodes.length > 0"
      class="mt-10 flex items-center justify-center gap-2"
      aria-label="Pagination"
    >
      <Button
        type="button"
        variant="ghost"
        class="size-8 p-0"
        :disabled="page <= 1"
        aria-label="Previous page"
        @click="page -= 1"
      >
        ‹
      </Button>
      <Button
        v-for="n in pageNumbers"
        :key="n"
        type="button"
        :variant="n === page ? 'primary' : 'ghost'"
        class="size-8 p-0"
        :aria-current="n === page ? 'page' : undefined"
        @click="page = n"
      >
        {{ n }}
      </Button>
      <Button
        type="button"
        variant="ghost"
        class="size-8 p-0"
        :disabled="page >= totalPages"
        aria-label="Next page"
        @click="page += 1"
      >
        ›
      </Button>
    </nav>
  </template>

  <Dialog v-model:open="formOpen">
    <DialogContent class="max-h-screen w-11/12 max-w-3xl overflow-y-auto">
      <DialogHeader>
        <DialogTitle>{{ formTitle }}</DialogTitle>
      </DialogHeader>

      <form
        id="promo-code-form"
        class="flex flex-col gap-6 px-6 py-6"
        @submit.prevent="submitForm"
      >
        <div class="grid grid-cols-1 gap-6 sm:grid-cols-2">
          <FormField label="Set promo code *" for="promo-code" :error="errors.code">
            <Input
              id="promo-code"
              :model-value="form.code"
              type="text"
              maxlength="40"
              autocomplete="off"
              spellcheck="false"
              placeholder="SAVE10"
              :aria-invalid="!!errors.code"
              aria-describedby="promo-code-error"
              @update:model-value="onCodeInput"
            />
          </FormField>

          <FormField label="Minimum purchase amount (THB) *" for="min-purchase" :error="errors.minPurchase">
            <Input
              id="min-purchase"
              :model-value="form.minPurchase"
              type="text"
              inputmode="decimal"
              autocomplete="off"
              placeholder="0.00"
              :aria-invalid="!!errors.minPurchase"
              aria-describedby="min-purchase-error"
              @update:model-value="onDecimalInput('minPurchase', $event)"
            />
          </FormField>
        </div>

        <FormField label="Select discount type *" for="discount-type" :error="errors.discount">
          <RadioGroup
            id="discount-type"
            :model-value="form.discountType"
            class="grid grid-cols-1 gap-4 sm:grid-cols-2"
            aria-label="Discount type"
            :aria-invalid="!!errors.discount || undefined"
            aria-describedby="discount-type-error"
            @update:model-value="onDiscountType"
          >
            <div class="flex items-center gap-3">
              <RadioGroupItem
                id="discount-fixed"
                value="FIXED"
                class="flex size-5 shrink-0 cursor-pointer items-center justify-center rounded-full border border-gray-600 bg-white outline-none is-focus:ring-2 is-focus:ring-ring data-[state=checked]:border-orange-500"
              >
                <RadioGroupIndicator class="size-2.5 rounded-full bg-orange-500" />
              </RadioGroupItem>
              <Label for="discount-fixed" class="cursor-pointer font-normal">
                Fixed amount (THB)
              </Label>
              <div class="w-28 shrink-0">
                <Input
                  id="discount-amount"
                  :model-value="form.amountOff"
                  type="text"
                  inputmode="decimal"
                  autocomplete="off"
                  placeholder="THB"
                  :aria-invalid="form.discountType === 'FIXED' && !!errors.discount"
                  @focus="onAmountFocus('FIXED')"
                  @update:model-value="onDecimalInput('amountOff', $event)"
                />
              </div>
            </div>

            <div class="flex items-center gap-3">
              <RadioGroupItem
                id="discount-percent"
                value="PERCENT"
                class="flex size-5 shrink-0 cursor-pointer items-center justify-center rounded-full border border-gray-600 bg-white outline-none is-focus:ring-2 is-focus:ring-ring data-[state=checked]:border-orange-500"
              >
                <RadioGroupIndicator class="size-2.5 rounded-full bg-orange-500" />
              </RadioGroupItem>
              <Label for="discount-percent" class="cursor-pointer font-normal">
                Percent (%)
              </Label>
              <div class="w-28 shrink-0">
                <Input
                  id="discount-percent-amount"
                  :model-value="form.percentOff"
                  type="text"
                  inputmode="decimal"
                  autocomplete="off"
                  placeholder="Percent"
                  :aria-invalid="form.discountType === 'PERCENT' && !!errors.discount"
                  @focus="onAmountFocus('PERCENT')"
                  @update:model-value="onDecimalInput('percentOff', $event)"
                />
              </div>
            </div>
          </RadioGroup>
        </FormField>

        <FormField label="Room types included" for="room-types">
          <MultiSelect
            id="room-types"
            v-model="form.roomTypeIds"
            :options="roomTypeOptions"
            all-label="All room types"
            :count-label="roomTypeCountLabel"
          />
        </FormField>
      </form>

      <DialogFooter class="border-t border-gray-300 pt-6">
        <DialogClose as-child>
          <Button type="button" variant="secondary" :disabled="saving">
            Cancel
          </Button>
        </DialogClose>
        <Button type="submit" form="promo-code-form" :disabled="saving">
          {{ submitLabel }}
        </Button>
      </DialogFooter>
    </DialogContent>
  </Dialog>

  <Dialog v-model:open="deleteOpen">
    <DialogContent class="w-11/12 sm:max-w-md">
      <DialogHeader>
        <DialogTitle>Delete promo code</DialogTitle>
        <DialogDescription>
          Delete promo code {{ deletingCode?.code }}? This cannot be undone from the list.
        </DialogDescription>
      </DialogHeader>
      <DialogFooter>
        <DialogClose as-child>
          <Button type="button" variant="secondary" :disabled="deleting">
            Cancel
          </Button>
        </DialogClose>
        <Button type="button" variant="secondary" :disabled="deleting" @click="confirmDelete">
          {{ deleting ? 'Deleting…' : 'Delete' }}
        </Button>
      </DialogFooter>
    </DialogContent>
  </Dialog>
</template>
