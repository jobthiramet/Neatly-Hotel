<!-- Figma: user > booking > special request -->
<script setup lang="ts">
import { Checkbox, CheckboxLabel } from '@/components/ui/checkbox'
import { Textarea } from '@/components/ui/textarea'
import { specialRequests, standardRequests } from '@/data/booking'

const selectedIds = defineModel<string[]>('selectedIds', { required: true })
const additionalRequest = defineModel<string>('additionalRequest', { required: true })

function isSelected(id: string) {
  return selectedIds.value.includes(id)
}

function toggleRequest(id: string, checked: boolean | 'indeterminate') {
  const on = checked === true
  if (on && !selectedIds.value.includes(id)) {
    selectedIds.value = [...selectedIds.value, id]
    return
  }
  if (!on) {
    selectedIds.value = selectedIds.value.filter(item => item !== id)
  }
}
</script>

<template>
  <div class="flex flex-col gap-10">
    <section aria-labelledby="standard-request-title" class="flex flex-col gap-4">
      <div class="flex flex-col gap-1">
        <h2 id="standard-request-title" class="text-h5 text-gray-800">
          Standard Request
        </h2>
        <p id="standard-request-hint" class="text-body3 font-normal text-gray-600">
          These requests are not confirmed (Depend on the available room)
        </p>
      </div>
      <ul class="flex flex-col gap-4" aria-describedby="standard-request-hint">
        <li v-for="item in standardRequests" :key="item.id">
          <div class="group/checkbox flex items-center gap-3">
            <Checkbox
              :id="`request-${item.id}`"
              :model-value="isSelected(item.id)"
              @update:model-value="(checked) => toggleRequest(item.id, checked)"
            />
            <CheckboxLabel :for="`request-${item.id}`">
              {{ item.label }}
            </CheckboxLabel>
          </div>
        </li>
      </ul>
    </section>

    <section aria-labelledby="special-request-title" class="flex flex-col gap-4">
      <div class="flex flex-col gap-1">
        <h2 id="special-request-title" class="text-h5 text-gray-800">
          Special Request
        </h2>
        <p id="special-request-hint" class="text-body3 font-normal text-gray-600">
          Additional charge may apply
        </p>
      </div>
      <ul class="flex flex-col gap-4" aria-describedby="special-request-hint">
        <li v-for="item in specialRequests" :key="item.id">
          <div class="group/checkbox flex items-center gap-3">
            <Checkbox
              :id="`request-${item.id}`"
              :model-value="isSelected(item.id)"
              @update:model-value="(checked) => toggleRequest(item.id, checked)"
            />
            <CheckboxLabel :for="`request-${item.id}`">
              {{ item.label }} (+THB {{ item.price }})
            </CheckboxLabel>
          </div>
        </li>
      </ul>
    </section>

    <section aria-labelledby="additional-request-title" class="flex flex-col gap-4">
      <h2 id="additional-request-title" class="text-h5 text-gray-800">
        Additional Request
      </h2>
      <Textarea
        id="additional-request"
        v-model="additionalRequest"
        rows="5"
        placeholder="Please input additional request here…"
        aria-labelledby="additional-request-title"
      />
    </section>
  </div>
</template>
