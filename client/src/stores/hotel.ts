import { defineStore } from 'pinia'
import { ref } from 'vue'
import neatlyLogo from '@/assets/images/neatly-logo.svg'

export interface HotelInfo {
  name: string
  /** Paragraphs separated by blank lines. */
  description: string
  /** Image URL, or a freshly picked file not uploaded yet. */
  logo: string | File | null
}

// ponytail: in-memory only, resets on reload. Swap for API calls once the hotel endpoint exists.
export const useHotelStore = defineStore('hotel', () => {
  const name = ref('Neatly Hotel')
  const description = ref([
    'Set in Bangkok, Thailand. Neatly Hotel offers 5-star accommodation with an outdoor pool, kids\' club, sports facilities and a fitness centre. There is also a spa, an indoor pool and saunas.',
    'All units at the hotel are equipped with a seating area, a flat-screen TV with satellite channels, a dining area and a private bathroom with free toiletries, a bathtub and a hairdryer. Every room in Neatly Hotel features a furnished balcony. Some rooms are equipped with a coffee machine.',
    'Free WiFi and entertainment facilities are available at property and also rentals are provided to explore the area.',
  ].join('\n\n'))
  const logo = ref<HotelInfo['logo']>(neatlyLogo)

  function update(info: HotelInfo) {
    name.value = info.name
    description.value = info.description
    logo.value = info.logo
  }

  return { name, description, logo, update }
})
