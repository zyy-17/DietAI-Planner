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
        meta: { title: '今日概览' }
      },
      {
        path: 'today/breakfast',
        name: 'TodayBreakfast',
        component: () => import('../views/MealDetail.vue'),
        meta: { title: '早餐', mealType: 'breakfast' }
      },
      {
        path: 'today/lunch',
        name: 'TodayLunch',
        component: () => import('../views/MealDetail.vue'),
        meta: { title: '午餐', mealType: 'lunch' }
      },
      {
        path: 'today/dinner',
        name: 'TodayDinner',
        component: () => import('../views/MealDetail.vue'),
        meta: { title: '晚餐', mealType: 'dinner' }
      },
      {
        path: 'today/snack',
        name: 'TodaySnack',
        component: () => import('../views/MealDetail.vue'),
        meta: { title: '加餐', mealType: 'snack' }
      },
      {
        path: 'today/ai-suggest',
        name: 'TodayAiSuggest',
        component: () => import('../views/AiSuggest.vue'),
        meta: { title: 'AI今日建议' }
      },
      {
        path: 'records',
        name: 'DietRecords',
        component: () => import('../views/DietRecords.vue'),
        meta: { title: '历史记录' }
      },
      {
        path: 'records/week',
        name: 'RecordsWeek',
        component: () => import('../views/DietRecords.vue'),
        meta: { title: '周记录', mode: 'week' }
      },
      {
        path: 'records/month',
        name: 'RecordsMonth',
        component: () => import('../views/DietRecords.vue'),
        meta: { title: '月记录', mode: 'month' }
      },
      {
        path: 'records/search',
        name: 'RecordsSearch',
        component: () => import('../views/DietRecords.vue'),
        meta: { title: '条件查询', mode: 'search' }
      },
      {
        path: 'records/stats',
        name: 'RecordsStats',
        component: () => import('../views/DietStats.vue'),
        meta: { title: '饮食统计' }
      },
      {
        path: 'nutrition',
        name: 'NutritionAnalysis',
        component: () => import('../views/NutritionAnalysis.vue'),
        meta: { title: '今日营养' }
      },
      {
        path: 'nutrition/trend',
        name: 'NutritionTrend',
        component: () => import('../views/NutritionAnalysis.vue'),
        meta: { title: '营养趋势', mode: 'trend' }
      },
      {
        path: 'nutrition/nutrients',
        name: 'NutritionNutrients',
        component: () => import('../views/NutritionAnalysis.vue'),
        meta: { title: '营养素分析', mode: 'nutrients' }
      },
      {
        path: 'nutrition/calorie',
        name: 'NutritionCalorie',
        component: () => import('../views/NutritionAnalysis.vue'),
        meta: { title: '热量分析', mode: 'calorie' }
      },
      {
        path: 'nutrition/goal',
        name: 'NutritionGoal',
        component: () => import('../views/NutritionAnalysis.vue'),
        meta: { title: '目标完成度', mode: 'goal' }
      },
      {
        path: 'nutrition/report',
        name: 'NutritionReport',
        component: () => import('../views/NutritionAnalysis.vue'),
        meta: { title: '营养报告', mode: 'report' }
      },
      {
        path: 'chat',
        name: 'AiChat',
        component: () => import('../views/AiChat.vue'),
        meta: { title: '新建对话' }
      },
      {
        path: 'chat/diet-plan',
        name: 'AiDietPlan',
        component: () => import('../views/AiChat.vue'),
        meta: { title: '膳食规划', preset: 'diet-plan' }
      },
      {
        path: 'chat/fat-loss',
        name: 'AiFatLoss',
        component: () => import('../views/AiChat.vue'),
        meta: { title: '减脂方案', preset: 'fat-loss' }
      },
      {
        path: 'chat/muscle-gain',
        name: 'AiMuscleGain',
        component: () => import('../views/AiChat.vue'),
        meta: { title: '增肌方案', preset: 'muscle-gain' }
      },
      {
        path: 'chat/consult',
        name: 'AiConsult',
        component: () => import('../views/AiChat.vue'),
        meta: { title: '饮食咨询', preset: 'consult' }
      },
      {
        path: 'chat/history',
        name: 'AiHistory',
        component: () => import('../views/AiChatHistory.vue'),
        meta: { title: '历史对话' }
      },
      {
        path: 'foods',
        name: 'FoodLibrary',
        component: () => import('../views/FoodLibrary.vue'),
        meta: { title: '全部食物' }
      },
      {
        path: 'foods/search',
        name: 'FoodSearch',
        component: () => import('../views/FoodLibrary.vue'),
        meta: { title: '食物搜索', mode: 'search' }
      },
      {
        path: 'foods/categories',
        name: 'FoodCategories',
        component: () => import('../views/FoodLibrary.vue'),
        meta: { title: '食物分类', mode: 'categories' }
      },
      {
        path: 'foods/add',
        name: 'FoodAdd',
        component: () => import('../views/FoodLibrary.vue'),
        meta: { title: '添加食物', mode: 'add' }
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('../views/Profile.vue'),
        meta: { title: '基本资料' }
      },
      {
        path: 'profile/body',
        name: 'ProfileBody',
        component: () => import('../views/Profile.vue'),
        meta: { title: '身体数据', section: 'body' }
      },
      {
        path: 'profile/goal',
        name: 'ProfileGoal',
        component: () => import('../views/Profile.vue'),
        meta: { title: '饮食目标', section: 'goal' }
      },
      {
        path: 'profile/preference',
        name: 'ProfilePreference',
        component: () => import('../views/Profile.vue'),
        meta: { title: '饮食偏好', section: 'preference' }
      },
      {
        path: 'profile/allergy',
        name: 'ProfileAllergy',
        component: () => import('../views/Profile.vue'),
        meta: { title: '忌口设置', section: 'allergy' }
      },
      {
        path: 'profile/activity',
        name: 'ProfileActivity',
        component: () => import('../views/Profile.vue'),
        meta: { title: '活动水平', section: 'activity' }
      },
      {
        path: 'profile/health',
        name: 'ProfileHealth',
        component: () => import('../views/Profile.vue'),
        meta: { title: '健康信息', section: 'health' }
      },
      {
        path: 'settings',
        name: 'Settings',
        component: () => import('../views/Settings.vue'),
        meta: { title: '通知设置' }
      },
      {
        path: 'settings/appearance',
        name: 'SettingsAppearance',
        component: () => import('../views/Settings.vue'),
        meta: { title: '界面设置', section: 'appearance' }
      },
      {
        path: 'settings/privacy',
        name: 'SettingsPrivacy',
        component: () => import('../views/Settings.vue'),
        meta: { title: '隐私设置', section: 'privacy' }
      },
      {
        path: 'settings/password',
        name: 'SettingsPassword',
        component: () => import('../views/Settings.vue'),
        meta: { title: '修改密码', section: 'password' }
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