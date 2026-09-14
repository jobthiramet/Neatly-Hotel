// Static home page content (Figma: home, 6:2 desktop / 7410:2846 mobile).
// Shaped like API data so these arrays can be swapped for fetched data later.
import type { Component } from 'vue'
import avatar from '@/assets/home/avatar.webp'
import roomDeluxe from '@/assets/home/room-deluxe.webp'
import roomPremierSeaView from '@/assets/home/room-premier-sea-view.webp'
import roomSuite from '@/assets/home/room-suite.webp'
import roomSuperiorGardenView from '@/assets/home/room-superior-garden-view.webp'
import roomSuperior from '@/assets/home/room-superior.webp'
import roomSupreme from '@/assets/home/room-supreme.webp'
import slider1 from '@/assets/home/slider-1.webp'
import slider3 from '@/assets/home/slider-3.webp'
import slider5 from '@/assets/home/slider-5.webp'
import {
  IconCar,
  IconFitness,
  IconLounge,
  IconPhoneCall,
  IconSauna,
  IconSpa,
  IconWifi,
} from '@/components/icons'

export interface NavLink {
  label: string
  href: string
}

export interface Photo {
  src: string
  alt: string
}

export interface Facility {
  label: string
  icon: Component
}

export interface Room {
  id: string
  name: string
  image: string
  alt: string
  href: string
}

export interface Testimonial {
  id: string
  quote: string
  author: string
  avatar: string
}

export const navLinks: NavLink[] = [
  { label: 'About Neatly', href: '#about' },
  { label: 'Service & Facilities', href: '#services' },
  { label: 'Rooms & Suits', href: '#rooms' },
]

// TODO: placeholder until the login page exists.
export const loginHref = '#'

export const aboutParagraphs = [
  'Set in Bangkok, Thailand. Neatly Hotel offers 5-star accommodation with an outdoor pool, kids\' club, sports facilities and a fitness centre. There is also a spa, an indoor pool and saunas.',
  'All units at the hotel are equipped with a seating area, a flat-screen TV with satellite channels, a dining area and a private bathroom with free toiletries, a bathtub and a hairdryer. Every room in Neatly Hotel features a furnished balcony. Some rooms are equipped with a coffee machine.',
  'Free WiFi and entertainment facilities are available at property and also rentals are provided to explore the area.',
]

export const hotelPhotos: Photo[] = [
  { src: slider1, alt: 'Sunlight through window blinds casting stripes across a room' },
  { src: roomDeluxe, alt: 'Freestanding bathtub next to a bed' },
  { src: slider3, alt: 'Deck chairs by the pool in front of curtained rooms' },
  { src: roomSuperior, alt: 'Bedroom with a wooden wall and a round mirror' },
  { src: slider5, alt: 'Loungers beside the hotel pool' },
]

export const facilities: Facility[] = [
  { label: 'Spa', icon: IconSpa },
  { label: 'Sauna', icon: IconSauna },
  { label: 'Fitness', icon: IconFitness },
  { label: 'Arrival Lounge', icon: IconLounge },
  { label: 'Free Wifi', icon: IconWifi },
  { label: 'Parking', icon: IconCar },
  { label: '24 hours operation', icon: IconPhoneCall },
]

// TODO: `href` values are placeholders until the room detail page exists.
export const rooms: Room[] = [
  { id: 'superior-garden-view', name: 'Superior Garden View', image: roomSuperiorGardenView, alt: 'Poolside loungers with a mountain view', href: '#' },
  { id: 'deluxe', name: 'Deluxe', image: roomDeluxe, alt: 'Deluxe room with a freestanding bathtub', href: '#' },
  { id: 'superior', name: 'Superior', image: roomSuperior, alt: 'Superior room with a wooden feature wall', href: '#' },
  { id: 'premier-sea-view', name: 'Premier Sea View', image: roomPremierSeaView, alt: 'Balcony chairs overlooking the sea', href: '#' },
  { id: 'supreme', name: 'Supreme', image: roomSupreme, alt: 'Supreme room with a desk and city view', href: '#' },
  { id: 'suite', name: 'Suite', image: roomSuite, alt: 'Suite bedroom with a teal throw and large windows', href: '#' },
]

// Placeholder copy from Figma.
const loremQuote = 'lorem ipsum dolor sit amet minim mollit non deserunt ullamco est sit aliqua dolor do amet sint, velit official consequat duis enim velit mollit, exercitation minim amet consequat sunt.'

export const testimonials: Testimonial[] = [
  { id: '1', quote: loremQuote, author: 'Katherine, Company®', avatar },
  { id: '2', quote: loremQuote, author: 'Katherine, Company®', avatar },
  { id: '3', quote: loremQuote, author: 'Katherine, Company®', avatar },
]

export const contact = {
  phone: '+66 99 999 9999',
  email: 'contact@neatlyhotel.com',
  address: '188 Phaya Thai Rd, Thung Phaya Thai, Ratchathewi, Bangkok 10400',
}

// TODO: placeholders until the hotel's social accounts are known.
export const socialLinks = {
  facebook: '#',
  instagram: '#',
  twitter: '#',
}
