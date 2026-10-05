// Room detail mock data (Figma: user > room detail 102:2788 / mobile > user > room detail)
import roomDeluxe from '@/assets/home/room-deluxe.webp'
import roomPremierSeaView from '@/assets/home/room-premier-sea-view.webp'
import roomSuite from '@/assets/home/room-suite.webp'
import roomSuperiorGardenView from '@/assets/home/room-superior-garden-view.webp'
import roomSuperior from '@/assets/home/room-superior.webp'
import roomSupreme from '@/assets/home/room-supreme.webp'
import slider1 from '@/assets/home/slider-1.webp'
import slider3 from '@/assets/home/slider-3.webp'
import slider5 from '@/assets/home/slider-5.webp'

export interface RoomDetail {
  id: string
  name: string
  description: string
  originalPrice: number
  currentPrice: number
  capacity: string
  bedType: string
  size: string
  gallery: { src: string; alt: string }[]
  amenitiesCol1: string[]
  amenitiesCol2: string[]
}

const standardAmenitiesCol1 = [
  'Safe in Room',
  'Air Conditioning',
  'High speed internet connection',
  'Hairdryer',
  'Shower',
  'Bathroom amenities',
  'Lamp',
]

const standardAmenitiesCol2 = [
  'Minibar',
  'Telephone',
  'Ironing board',
  'A floor only accessible via a guest room key',
  'Alarm clock',
  'Bathrobe',
]

export const roomDetails: Record<string, RoomDetail> = {
  'superior-garden-view': {
    id: 'superior-garden-view',
    name: 'Superior Garden View',
    description: 'Rooms (36sqm) with full garden views, 1 single bed, bathroom with bathtub & shower.',
    originalPrice: 3100,
    currentPrice: 2500,
    capacity: '2 Person',
    bedType: '1 Double bed',
    size: '32 sqm',
    gallery: [
      { src: roomSuperiorGardenView, alt: 'Poolside loungers with a mountain view' },
      { src: slider1, alt: 'Sunlight through window blinds casting stripes across a room' },
      { src: slider3, alt: 'Deck chairs by the pool in front of curtained rooms' },
      { src: slider5, alt: 'Loungers beside the hotel pool' },
    ],
    amenitiesCol1: standardAmenitiesCol1,
    amenitiesCol2: standardAmenitiesCol2,
  },
  deluxe: {
    id: 'deluxe',
    name: 'Deluxe',
    description: 'Spacious deluxe room with freestanding bathtub, premium bedding, and scenic courtyard view.',
    originalPrice: 3500,
    currentPrice: 2900,
    capacity: '2 Person',
    bedType: '1 Double bed',
    size: '42 sqm',
    gallery: [
      { src: roomDeluxe, alt: 'Deluxe room with a freestanding bathtub' },
      { src: slider1, alt: 'Sunlight through window blinds' },
      { src: slider5, alt: 'Loungers beside the hotel pool' },
    ],
    amenitiesCol1: standardAmenitiesCol1,
    amenitiesCol2: standardAmenitiesCol2,
  },
  superior: {
    id: 'superior',
    name: 'Superior',
    description: 'Comfortable superior room featuring custom wood finishings, modern ensuite, and workspace.',
    originalPrice: 3100,
    currentPrice: 2500,
    capacity: '2 Person',
    bedType: '1 Double bed',
    size: '35 sqm',
    gallery: [
      { src: roomSuperior, alt: 'Superior room with a wooden feature wall' },
      { src: slider3, alt: 'Deck chairs by the pool' },
      { src: slider1, alt: 'Room interior with soft lighting' },
    ],
    amenitiesCol1: standardAmenitiesCol1,
    amenitiesCol2: standardAmenitiesCol2,
  },
  'premier-sea-view': {
    id: 'premier-sea-view',
    name: 'Premier Sea View',
    description: 'Breathtaking ocean views from your private balcony with contemporary coastal aesthetics.',
    originalPrice: 4200,
    currentPrice: 3600,
    capacity: '2 Person',
    bedType: '1 King bed',
    size: '48 sqm',
    gallery: [
      { src: roomPremierSeaView, alt: 'Balcony chairs overlooking the sea' },
      { src: slider5, alt: 'Poolside area' },
      { src: slider1, alt: 'Sunlit interior' },
    ],
    amenitiesCol1: standardAmenitiesCol1,
    amenitiesCol2: standardAmenitiesCol2,
  },
  supreme: {
    id: 'supreme',
    name: 'Supreme',
    description: 'Sophisticated living space with panoramic city skyline views, executive desk, and walk-in shower.',
    originalPrice: 4800,
    currentPrice: 4000,
    capacity: '2 Person',
    bedType: '1 King bed',
    size: '54 sqm',
    gallery: [
      { src: roomSupreme, alt: 'Supreme room with a desk and city view' },
      { src: slider3, alt: 'Deck chairs by the pool' },
      { src: slider1, alt: 'Interior lounge' },
    ],
    amenitiesCol1: standardAmenitiesCol1,
    amenitiesCol2: standardAmenitiesCol2,
  },
  suite: {
    id: 'suite',
    name: 'Suite',
    description: 'Our most luxurious suite featuring separate living quarters, expansive balcony, and soaking tub.',
    originalPrice: 6500,
    currentPrice: 5500,
    capacity: '4 Person',
    bedType: '2 King beds',
    size: '78 sqm',
    gallery: [
      { src: roomSuite, alt: 'Suite bedroom with a teal throw and large windows' },
      { src: roomDeluxe, alt: 'Bathroom suite' },
      { src: slider5, alt: 'Poolside access' },
    ],
    amenitiesCol1: standardAmenitiesCol1,
    amenitiesCol2: standardAmenitiesCol2,
  },
}

export const defaultRoomId = 'superior-garden-view'
