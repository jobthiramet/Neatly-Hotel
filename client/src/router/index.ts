import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import { hasAgentRole } from '@/api/agent'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  // New pages start at the top; back/forward restores the previous position.
  // Hash links (#about) are left to the browser, which honours scroll-padding for the sticky navbar.
  scrollBehavior(to, _from, savedPosition) {
    if (savedPosition) return savedPosition
    if (to.hash) return false
    return { top: 0 }
  },
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView,
    },
    {
      path: '/booking-history',
      name: 'booking-history',
      component: () => import('../views/BookingHistoryView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/bookings',
      redirect: { name: 'booking-history' },
    },
    {
      path: '/bookings/:bookingId/change-date',
      name: 'change-booking-date',
      component: () => import('../views/ChangeBookingDateView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/rooms/:roomId',
      name: 'room-detail',
      component: () => import('../views/RoomDetailView.vue'),
    },
    {
      path: '/room-detail',
      redirect: '/rooms/superior-garden-view',
    },
    {
      path: '/bookings/:bookingId/refund',
      name: 'booking-refund',
      component: () => import('../views/CancelBookingView.vue'),
      props: { refund: true },
      meta: { requiresAuth: true },
    },
    {
      path: '/bookings/:bookingId/cancel',
      name: 'booking-cancel',
      component: () => import('../views/CancelBookingView.vue'),
      props: { refund: false },
      meta: { requiresAuth: true },
    },
    {
      path: '/admin',
      meta: { requiresAgent: true },
      component: () => import('../components/layout/AdminLayout.vue'),
      redirect: { name: 'admin-hotel-information' },
      children: [
        {
          path: 'hotel-information',
          name: 'admin-hotel-information',
          component: () => import('../views/admin/HotelInformationView.vue'),
          meta: { title: 'Hotel Information' },
        },
        {
          path: 'rooms',
          name: 'admin-rooms',
          component: () => import('../views/admin/RoomListView.vue'),
          meta: { title: 'Room & Property', adminNav: 'admin-rooms' },
        },
        {
          path: 'rooms/new',
          name: 'admin-room-create',
          component: () => import('../views/admin/RoomCreateView.vue'),
          meta: { title: 'Create New Room', adminNav: 'admin-rooms' },
        },
        {
          // No title: the page teleports a back link and the room name into the header.
          path: 'rooms/:id',
          name: 'admin-room-edit',
          component: () => import('../views/admin/RoomEditView.vue'),
          meta: { adminNav: 'admin-rooms' },
        },
      ],
    },
    {
      path: '/admin/login',
      name: 'agent-login',
      component: () => import('@/views/admin/AgentSignInView.vue'),
      meta: { agentLogin: true },
    },
    {
      path: '/admin/forbidden',
      name: 'agent-forbidden',
      component: () => import('@/views/admin/AgentSignInView.vue'),
      props: { forbidden: true },
    },
    {
      path: '/admin/:pathMatch(.*)*',
      redirect: '/admin',
    },
    {
      // Dev-facing component & token showcase — see client/DESIGN_SYSTEM.md
      path: '/design-system',
      name: 'design-system',
      component: () => import('../views/DesignSystemView.vue'),
    },
    {
      path: "/sign-in",
      component: () => import("@/views/SignInView.vue"),
    },
    {
      path: "/sign-up",
      component: () => import("@/views/SignUpView.vue"),
    },
    {
      path: "/sso-callback",
      component: () => import("@/views/SsoCallbackView.vue"),
    },
  ],
})

// Routes with `meta: { requiresAuth: true }` need a signed-in Clerk user.
// Guests go to Clerk's sign-in, which returns them via `redirect_url`
// (Clerk only follows same-origin redirect URLs).
router.beforeEach(async (to) => {
  if (to.meta.requiresAgent || to.meta.agentLogin) {
    const clerk = await loadedClerk()
    if (!clerk?.user) return to.meta.agentLogin ? undefined : { name: 'agent-login' }
    try {
      const token = await clerk.session?.getToken()
      if (!token || !await hasAgentRole(token)) return { name: 'agent-forbidden' }
      if (to.meta.agentLogin) return { name: 'admin-hotel-information' }
    }
    catch {
      // Fail closed: missing profiles and unavailable role checks never grant access.
      return { name: 'agent-forbidden' }
    }
    return
  }
  if (!to.meta.requiresAuth)
    return
  const clerk = await loadedClerk()
  if (!clerk?.user)
    return { path: '/sign-in', query: { redirect_url: to.fullPath } }
})

// Clerk restores the session asynchronously on page load; wait for it so a
// signed-in user who refreshes on a protected page isn't bounced to sign-in.
// ponytail: polls window.Clerk (set by clerkPlugin); gives up after 10s and treats the user as signed out.
async function loadedClerk() {
  for (let waited = 0; waited < 10_000; waited += 50) {
    if (window.Clerk?.loaded)
      return window.Clerk
    await new Promise(resolve => setTimeout(resolve, 50))
  }
}

export default router
