import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView,
    },
    {
      path: '/bookings/:bookingId/change-date',
      name: 'change-booking-date',
      component: () => import('../views/ChangeBookingDateView.vue'),
    },
    {
      // Dev-facing component & token showcase — see client/DESIGN_SYSTEM.md
      path: '/design-system',
      name: 'design-system',
      component: () => import('../views/DesignSystemView.vue'),
    },
  ],
})

export default router
