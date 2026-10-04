<template>
  <div class="profile-page">
    <div class="page-tabs">
      <div v-for="tab in tabs" :key="tab.path"
        class="tab-item" :class="{ active: isTabActive(tab) }"
        @click="router.push(tab.path)">
        {{ tab.icon }} {{ tab.name }}
      </div>
    </div>

    <el-card style="max-width:650px;margin:0 auto">
      <template #header><span>{{ sectionTitle }}</span></template>

      <div v-if="section === 'default'" style="display:flex;justify-content:center;margin-bottom:20px">
        <el-upload
          :action="uploadUrl"
          :headers="uploadHeaders"
          :show-file-list="false"
          :on-success="handleAvatarSuccess"
          :before-upload="beforeAvatarUpload"
          accept="image/*"
        >
          <el-avatar :size="80" :src="avatarFullUrl" style="cursor:pointer">
            <el-icon :size="30"><UserFilled /></el-icon>
          </el-avatar>
        </el-upload>
        <div style="margin-left:12px;display:flex;align-items:center;color:#999;font-size:13px">点击头像上传</div>
      </div>

      <el-form :model="form" label-width="100px">
        <template v-if="section === 'default'">
          <el-form-item label="真实姓名"><el-input v-model="form.realName" placeholder="请输入姓名" /></el-form-item>
          <el-form-item label="性别">
            <el-radio-group v-model="form.gender">
              <el-radio :value="1">男</el-radio>
              <el-radio :value="2">女</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="出生日期"><el-date-picker v-model="form.birthDate" type="date" value-format="YYYY-MM-DD" /></el-form-item>
        </template>

        <template v-if="section === 'body'">
          <el-form-item label="身高(cm)"><el-input-number v-model="form.height" :precision="1" :min="100" :max="250" /></el-form-item>
          <el-form-item label="体重(kg)"><el-input-number v-model="form.weight" :precision="1" :min="30" :max="300" /></el-form-item>
        </template>

        <template v-if="section === 'activity'">
          <el-form-item label="活动水平">
            <el-select v-model="form.activityLevel" placeholder="请选择">
              <el-option v-for="o in options.activity_level" :key="o.code" :value="Number(o.code)" :label="o.label" />
            </el-select>
          </el-form-item>
        </template>

        <template v-if="section === 'goal'">
          <el-form-item label="饮食目标">
            <el-select v-model="form.dietGoal" placeholder="请选择">
              <el-option v-for="o in options.diet_goal" :key="o.code" :value="o.code" :label="o.label" />
            </el-select>
          </el-form-item>
        </template>

        <template v-if="section === 'preference'">
          <el-form-item label="饮食偏好">
            <el-select v-model="selectedPreferences" multiple placeholder="选择饮食偏好" style="width:100%" @change="onPreferenceChange">
              <el-option v-for="o in options.diet_preference" :key="o.code" :value="o.code" :label="o.label" />
            </el-select>
          </el-form-item>
          <el-form-item label="">
            <el-input v-model="form.dietPreference" placeholder="或直接输入饮食偏好，如：不吃海鲜、乳糖不耐受等" />
          </el-form-item>
        </template>

        <template v-if="section === 'allergy'">
          <el-form-item label="忌口食物">
            <el-select v-model="selectedAllergies" multiple placeholder="选择忌口食物" style="width:100%" @change="onAllergyChange">
              <el-option v-for="o in options.allergy" :key="o.code" :value="o.code" :label="o.label" />
            </el-select>
          </el-form-item>
          <el-form-item label="">
            <el-input v-model="form.allergyNote" placeholder="补充说明其他忌口或过敏信息" />
          </el-form-item>
        </template>

        <template v-if="section === 'health'">
          <el-form-item label="慢性疾病">
            <el-select v-model="selectedDiseases" multiple placeholder="选择相关疾病" style="width:100%" @change="onDiseaseChange">
              <el-option v-for="o in options.disease" :key="o.code" :value="o.code" :label="o.label" />
            </el-select>
          </el-form-item>
          <el-form-item label="用药情况">
            <el-input v-model="form.medication" type="textarea" :rows="2" placeholder="请描述当前用药情况" />
          </el-form-item>
        </template>

        <el-form-item>
          <el-button type="primary" @click="saveProfile" :loading="saving">保存</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { UserFilled } from '@element-plus/icons-vue'
import api from '../utils/api'
import { useUserStore } from '../stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const section = computed(() => route.meta.section || 'default')

const tabs = [
  { name: '基本资料', path: '/profile', icon: '👤' },
  { name: '身体数据', path: '/profile/body', icon: '📏' },
  { name: '饮食目标', path: '/profile/goal', icon: '🎯' },
  { name: '饮食偏好', path: '/profile/preference', icon: '🥗' },
  { name: '忌口设置', path: '/profile/allergy', icon: '🚫' },
  { name: '活动水平', path: '/profile/activity', icon: '🏃' },
  { name: '健康信息', path: '/profile/health', icon: '❤️' }
]

function isTabActive(tab) {
  return route.path === tab.path
}

const sectionTitles = {
  default: '👤 基本资料',
  body: '📏 身体数据',
  goal: '🎯 饮食目标',
  preference: '🥗 饮食偏好',
  allergy: '🚫 忌口设置',
  activity: '🏃 活动水平',
  health: '❤️ 健康信息'
}
const sectionTitle = computed(() => sectionTitles[section.value] || '个人中心')

const saving = ref(false)
const selectedPreferences = ref([])
const selectedAllergies = ref([])
const selectedDiseases = ref([])

// 下拉选项由后端 /user/options 下发，管理员在后台改动后刷新即生效
const options = ref({
  diet_goal: [],
  activity_level: [],
  diet_preference: [],
  allergy: [],
  disease: []
})

// 兜底选项：接口异常时页面仍可正常使用（不白屏、不报错）
const FALLBACK_OPTIONS = {
  diet_goal: [
    { code: 'lose', label: '减脂' },
    { code: 'maintain', label: '维持' },
    { code: 'gain', label: '增肌' }
  ],
  activity_level: [
    { code: '1', label: '久坐（几乎不运动）' },
    { code: '2', label: '轻度活动（每周1-3次）' },
    { code: '3', label: '中度活动（每周3-5次）' },
    { code: '4', label: '高度活动（每周6-7次）' },
    { code: '5', label: '极高活动（体力劳动）' }
  ],
  diet_preference: [],
  allergy: [],
  disease: []
}

const form = reactive({
  realName: '',
  gender: null,
  birthDate: null,
  height: null,
  weight: null,
  activityLevel: null,
  dietGoal: null,
  dietPreference: '',
  allergyNote: '',
  disease: '',
  medication: '',
  avatarUrl: ''
})

const uploadUrl = '/api/user/avatar'
const uploadHeaders = computed(() => ({
  Authorization: `Bearer ${localStorage.getItem('token')}`
}))

const avatarFullUrl = computed(() => form.avatarUrl || '')

function beforeAvatarUpload(file) {
  const isImage = file.type.startsWith('image/')
  const isLt2M = file.size / 1024 / 1024 < 2
  if (!isImage) ElMessage.error('只能上传图片文件!')
  if (!isLt2M) ElMessage.error('图片大小不能超过2MB!')
  return isImage && isLt2M
}

function handleAvatarSuccess(response) {
  if (response.code === 200) {
    form.avatarUrl = response.data
    userStore.setAvatar(response.data)
    ElMessage.success('头像上传成功')
  } else {
    ElMessage.error(response.message || '上传失败')
  }
}

function onPreferenceChange(values) {
  form.dietPreference = values.join('、')
}

/** 多选值以「、」分隔存库，与后端 labelsOf 的解析规则保持一致 */
function onAllergyChange(values) {
  form.allergyNote = values.join('、')
}

function onDiseaseChange(values) {
  form.disease = values.join('、')
}

async function loadOptions() {
  try {
    const data = await api.get('/user/options')
    if (data) {
      // 逐项合并：某分组为空时保留兜底，避免管理员清空后页面无选项可选
      const merged = {}
      for (const key of Object.keys(FALLBACK_OPTIONS)) {
        merged[key] = (data[key] && data[key].length) ? data[key] : FALLBACK_OPTIONS[key]
      }
      options.value = merged
    }
  } catch (e) {
    options.value = JSON.parse(JSON.stringify(FALLBACK_OPTIONS))
  }
}

async function loadProfile() {
  try {
    const data = await api.get('/user/profile')
    if (data) {
      Object.assign(form, data)
      if (form.dietPreference) {
        selectedPreferences.value = form.dietPreference.split('、').filter(Boolean)
      }
      if (form.allergyNote) {
        selectedAllergies.value = form.allergyNote.split('、').filter(Boolean)
      }
      if (form.disease) {
        selectedDiseases.value = form.disease.split('、').filter(Boolean)
      }
    }
  } catch (e) {}
}

async function saveProfile() {
  // 下拉多选与自由文本可能不一致：以下拉选中项为准同步回字符串字段
  if (selectedPreferences.value.length) form.dietPreference = selectedPreferences.value.join('、')
  if (selectedAllergies.value.length) form.allergyNote = selectedAllergies.value.join('、')
  if (selectedDiseases.value.length) form.disease = selectedDiseases.value.join('、')

  saving.value = true
  try {
    await api.put('/user/profile', form)
    ElMessage.success('保存成功')
  } catch (e) {} finally {
    saving.value = false
  }
}

onMounted(async () => {
  await Promise.all([loadOptions(), loadProfile()])
})
</script>

<style scoped>
.profile-page { padding: 18px; }
.page-tabs { display: flex; gap: 4px; margin-bottom: 16px; border-bottom: 2px solid #eef2f6; padding-bottom: 0; }
.tab-item { padding: 8px 16px; font-size: 13px; color: #55738d; cursor: pointer; border-bottom: 2px solid transparent; margin-bottom: -2px; transition: all 0.2s; white-space: nowrap; }
.tab-item:hover { color: #2789ed; }
.tab-item.active { color: #2589ee; border-bottom-color: #2589ee; font-weight: 600; }
</style>