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
      path: '/bookings/:bookingId/cancel',
      name: 'cancel-booking',
      component: () => import('../views/CancelBookingView.vue'),
    },
    {
      // TODO: add an admin auth guard (pending auth work).
      path: '/admin',
      component: () => import('../components/layout/AdminLayout.vue'),
      redirect: { name: 'admin-hotel-information' },
      children: [
        {
          path: 'hotel-information',
          name: 'admin-hotel-information',
          component: () => import('../views/admin/HotelInformationView.vue'),
          meta: { title: 'Hotel Information' },
        },
      ],
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
