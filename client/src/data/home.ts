// Static home page content (Figma: home, 6:2 desktop / 7410:2846 mobile).
// Shaped like API data so these arrays can be swapped for fetched data later.
import type { Component } from 'vue'
import roomDeluxe from '@/assets/home/room-deluxe.webp'
import roomPremierSeaView from '@/assets/home/room-premier-sea-view.webp'
import roomSuite from '@/assets/home/room-suite.webp'
import roomSuperiorGardenView from '@/assets/home/room-superior-garden-view.webp'
import roomSuperior from '@/assets/home/room-superior.webp'
import roomSupreme from '@/assets/home/room-supreme.webp'
import serviceFitness from '@/assets/home/services/service-fitness.webp'
import serviceLounge from '@/assets/home/services/service-lounge.webp'
import serviceParking from '@/assets/home/services/service-parking.webp'
import serviceReception from '@/assets/home/services/service-reception.webp'
import serviceSauna from '@/assets/home/services/service-sauna.webp'
import serviceSpa from '@/assets/home/services/service-spa.webp'
import serviceWifi from '@/assets/home/services/service-wifi.webp'
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
  id: string
  label: string
  icon: Component
  description: string
  image: string
  alt: string
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

// PLACEHOLDER CONTENT: invented descriptions. Replace with real hotel details before launch.
// Photos supplied by the team (source URLs not recorded), cropped to 1200×800 WebP.
export const facilities: Facility[] = [
  {
    id: 'spa',
    label: 'Spa',
    icon: IconSpa,
    description: 'Unwind in private treatment rooms finished in warm wood and stone. Our therapists offer signature massages, facials and body wraps using natural oils. Book a session at reception or from your room.',
    image: serviceSpa,
    alt: 'Spa lounge with two armchairs beside a treatment room with massage beds',
  },
  {
    id: 'sauna',
    label: 'Sauna',
    icon: IconSauna,
    description: 'A traditional Finnish sauna with softly lit cedar benches and a stone heater. Sessions are included with every stay, with fresh towels and chilled water waiting outside.',
    image: serviceSauna,
    alt: 'Wooden sauna with lit benches and a round stone heater',
  },
  {
    id: 'fitness',
    label: 'Fitness',
    icon: IconFitness,
    description: 'Keep up your routine in our fully equipped gym, open around the clock. Treadmills, bikes and a full rack of free weights are ready whenever you are.',
    image: serviceFitness,
    alt: 'Hotel gym with treadmills, a mirrored wall and a dumbbell rack',
  },
  {
    id: 'arrival-lounge',
    label: 'Arrival Lounge',
    icon: IconLounge,
    description: 'Arrived early or leaving late? Relax in the arrival lounge with comfortable seating, refreshments and luggage storage until your room is ready.',
    image: serviceLounge,
    alt: 'Sunlit hotel lounge with armchairs and a polished marble floor',
  },
  {
    id: 'free-wifi',
    label: 'Free Wifi',
    icon: IconWifi,
    description: 'Fast, free wifi reaches every room, the lounge and the poolside. Stream, work or video call without a password hunt: connect once and stay online for your whole visit.',
    image: serviceWifi,
    alt: 'Laptop on a hotel bed beside a lit bedside lamp',
  },
  {
    id: 'parking',
    label: 'Parking',
    icon: IconCar,
    description: 'Free on-site parking is available for every guest, shaded by mature trees. Spaces are a short walk from the entrance, and staff can help carry your luggage in.',
    image: serviceParking,
    alt: 'Open-air car park with parked cars under a large tree',
  },
  {
    id: 'reception',
    label: '24 hours operation',
    icon: IconPhoneCall,
    description: 'Our front desk is staffed day and night. Late check-in, taxi bookings or a quick recommendation: someone is always here to help.',
    image: serviceReception,
    alt: 'Hotel reception desk with wooden panels and leather armchairs',
  },
]

export const rooms: Room[] = [
  { id: 'superior-garden-view', name: 'Superior Garden View', image: roomSuperiorGardenView, alt: 'Poolside loungers with a mountain view', href: '/rooms/superior-garden-view' },
  { id: 'deluxe', name: 'Deluxe', image: roomDeluxe, alt: 'Deluxe room with a freestanding bathtub', href: '/rooms/deluxe' },
  { id: 'superior', name: 'Superior', image: roomSuperior, alt: 'Superior room with a wooden feature wall', href: '/rooms/superior' },
  { id: 'premier-sea-view', name: 'Premier Sea View', image: roomPremierSeaView, alt: 'Balcony chairs overlooking the sea', href: '/rooms/premier-sea-view' },
  { id: 'supreme', name: 'Supreme', image: roomSupreme, alt: 'Supreme room with a desk and city view', href: '/rooms/supreme' },
  { id: 'suite', name: 'Suite', image: roomSuite, alt: 'Suite bedroom with a teal throw and large windows', href: '/rooms/suite' },
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
