import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue')
  },
  {
    path: '/',
    component: () => import('../layouts/MainLayout.vue'),
    redirect: '/today',
    children: [
      {
        path: 'today',
        name: 'TodayDiet',
        component: () => import('../views/TodayDiet.vue'),
        meta: { title: '今日饮食' }
      },
      {
        path: 'records',
        name: 'DietRecords',
        component: () => import('../views/DietRecords.vue'),
        meta: { title: '饮食记录' }
      },
      {
        path: 'nutrition',
        name: 'NutritionAnalysis',
        component: () => import('../views/NutritionAnalysis.vue'),
        meta: { title: '营养分析' }
      },
      {
        path: 'chat',
        name: 'AiChat',
        component: () => import('../views/AiChat.vue'),
        meta: { title: 'AI对话' }
      },
      {
        path: 'foods',
        name: 'FoodLibrary',
        component: () => import('../views/FoodLibrary.vue'),
        meta: { title: '食物库' }
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('../views/Profile.vue'),
        meta: { title: '个人中心' }
      }
    ]
  },
  {
    path: '/admin',
    component: () => import('../layouts/AdminLayout.vue'),
    redirect: '/admin/users',
    children: [
      {
        path: 'users',
        name: 'AdminUsers',
        component: () => import('../views/admin/Users.vue'),
        meta: { title: '用户管理' }
      },
      {
        path: 'foods',
        name: 'AdminFoods',
        component: () => import('../views/admin/Foods.vue'),
        meta: { title: '食物管理' }
      },
      {
        path: 'categories',
        name: 'AdminCategories',
        component: () => import('../views/admin/Categories.vue'),
        meta: { title: '分类管理' }
      },
      {
        path: 'ai-logs',
        name: 'AdminAiLogs',
        component: () => import('../views/admin/AiLogs.vue'),
        meta: { title: 'AI记录' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (to.path !== '/login' && !token) {
    next('/login')
  } else {
    next()
  }
})

export default router