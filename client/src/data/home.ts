// Static home page content (Figma: home, 6:2 desktop / 7410:2846 mobile).
// Shaped like API data so these arrays can be swapped for fetched data later.
import type { Component } from 'vue'
import roomDeluxe from '@/assets/home/room-deluxe.webp'
import roomPremierSeaView from '@/assets/home/room-premier-sea-view.webp'
import roomSuite from '@/assets/home/room-suite.webp'
import roomSuperiorGardenView from '@/assets/home/room-superior-garden-view.webp'
import roomSuperior from '@/assets/home/room-superior.webp'
import roomSupreme from '@/assets/home/room-supreme.webp'
import slider1 from '@/assets/home/slider-1.webp'
import slider3 from '@/assets/home/slider-3.webp'
import slider5 from '@/assets/home/slider-5.webp'
import emmaLindqvist from '@/assets/images/testimonials/emma-lindqvist.webp'
import margaretOconnor from '@/assets/images/testimonials/margaret-oconnor.webp'
import rajeshMenon from '@/assets/images/testimonials/rajesh-menon.webp'
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
  avatarAlt: string
}

export const navLinks: NavLink[] = [
  { label: 'About Neatly', href: '#about' },
  { label: 'Service & Facilities', href: '#services' },
  { label: 'Rooms & Suits', href: '#rooms' },
]

// TODO: placeholder until the login page exists.
export const loginHref = '#'

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

// PLACEHOLDER CONTENT: invented reviews. Replace with real guest reviews before launch.
// Portraits (Unsplash License), cropped to 64×64 WebP:
// - Emma Lindqvist: https://unsplash.com/photos/fnqDH-yaFWE (Katherine Volkovski, same photo as the Figma avatar)
// - Rajesh Menon: https://unsplash.com/photos/XGs1bSZ9P6s (Mohammed Umar Ashrafi)
// - Margaret O'Connor: https://unsplash.com/photos/vsfbqZ1YOnU (Vidak)
export const testimonials: Testimonial[] = [
  {
    id: 'emma-lindqvist',
    quote: 'The pool was the highlight of our stay, and the spa massage melted away a week of travel. Every corner of the hotel felt calm, and we left more rested than we arrived.',
    author: 'Emma Lindqvist',
    avatar: emmaLindqvist,
    avatarAlt: 'Portrait of Emma Lindqvist',
  },
  {
    id: 'rajesh-menon',
    quote: 'The front desk team remembered my name from day one and arranged a late check-out without any fuss. The location is perfect too, a short walk from the BTS and great street food.',
    author: 'Rajesh Menon',
    avatar: rajeshMenon,
    avatarAlt: 'Portrait of Rajesh Menon',
  },
  {
    id: 'margaret-oconnor',
    quote: 'Our room was spotless, and the bed was so comfortable I slept better than at home. Breakfast had a lovely mix of Thai dishes and fresh pastries, all served with a smile.',
    author: 'Margaret O’Connor',
    avatar: margaretOconnor,
    avatarAlt: 'Portrait of Margaret O’Connor',
  },
]

export const contact = {
  phone: '+66 99 999 9999',
  email: 'contact@neatlyhotel.com',
  address: '188 Phaya Thai Rd, Thung Phaya Thai, Ratchathewi, Bangkok 10400',
}

// Platform homepages until the hotel's own social accounts are known.
export const socialLinks = {
  facebook: 'https://www.facebook.com/',
  instagram: 'https://www.instagram.com/',
  twitter: 'https://x.com/',
}
