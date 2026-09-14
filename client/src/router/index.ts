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
      // Dev-facing component & token showcase — see client/DESIGN_SYSTEM.md
      path: '/design-system',
      name: 'design-system',
      component: () => import('../views/DesignSystemView.vue'),
    },
  ],
})

export default router
