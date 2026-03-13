import { createRouter, createWebHistory } from 'vue-router';

const LoginView = () => import('../views/Login.vue');
const ChatView = () => import('../views/Chat.vue');
const ReportView = () => import('../views/Report.vue');
const DashboardView = () => import('../views/Dashboard.vue');

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: LoginView,
  },
  {
    path: '/chat',
    name: 'Chat',
    component: ChatView,
  },
  {
    path: '/report',
    name: 'Report',
    component: ReportView,
  },
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: DashboardView,
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/login',
  },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

const allowedAuthRoutes = ['/chat', '/report', '/dashboard'];

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token');

  if (!token && to.path !== '/login') {
    next('/login');
    return;
  }

  if (token && to.path === '/login') {
    next('/chat');
    return;
  }

  if (token && allowedAuthRoutes.includes(to.path)) {
    next();
    return;
  }

  if (to.path === '/login') {
    next();
    return;
  }

  next('/login');
});

export default router;
