<template>
  <div class="profile-page">
    <el-card style="max-width:650px;margin:0 auto">
      <template #header><span>个人中心</span></template>

      <div style="display:flex;justify-content:center;margin-bottom:20px">
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
        <el-form-item label="真实姓名"><el-input v-model="form.realName" placeholder="请输入姓名" /></el-form-item>
        <el-form-item label="性别">
          <el-radio-group v-model="form.gender">
            <el-radio :value="1">男</el-radio>
            <el-radio :value="2">女</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="出生日期"><el-date-picker v-model="form.birthDate" type="date" value-format="YYYY-MM-DD" /></el-form-item>
        <el-form-item label="身高(cm)"><el-input-number v-model="form.height" :precision="1" :min="100" :max="250" /></el-form-item>
        <el-form-item label="体重(kg)"><el-input-number v-model="form.weight" :precision="1" :min="30" :max="300" /></el-form-item>
        <el-form-item label="活动水平">
          <el-select v-model="form.activityLevel" placeholder="请选择">
            <el-option :value="1" label="久坐（几乎不运动）" />
            <el-option :value="2" label="轻度活动（每周1-3次）" />
            <el-option :value="3" label="中度活动（每周3-5次）" />
            <el-option :value="4" label="高度活动（每周6-7次）" />
            <el-option :value="5" label="极高活动（体力劳动）" />
          </el-select>
        </el-form-item>
        <el-form-item label="饮食目标">
          <el-select v-model="form.dietGoal" placeholder="请选择">
            <el-option value="lose" label="减脂" />
            <el-option value="maintain" label="维持" />
            <el-option value="gain" label="增肌" />
          </el-select>
        </el-form-item>
        <el-form-item label="饮食偏好">
          <el-select v-model="selectedPreferences" multiple placeholder="选择饮食偏好" style="width:100%" @change="onPreferenceChange">
            <el-option value="清淡" label="清淡" />
            <el-option value="中式" label="中式" />
            <el-option value="西式" label="西式" />
            <el-option value="素食" label="素食" />
            <el-option value="低糖" label="低糖" />
            <el-option value="低脂" label="低脂" />
            <el-option value="0高蛋白" label="高蛋白" />
            <el-option value="无辣" label="无辣" />
            <el-option value="地中海" label="地中海饮食" />
            <el-option value="生酮" label="生酮" />
          </el-select>
        </el-form-item>
        <el-form-item label="">
          <el-input v-model="form.dietPreference" placeholder="或直接输入饮食偏好，如：不吃海鲜、乳糖不耐受等" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="saveProfile" :loading="saving">保存</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { UserFilled } from '@element-plus/icons-vue'
import api from '../utils/api'

const saving = ref(false)
const selectedPreferences = ref([])
const form = reactive({
  realName: '',
  gender: null,
  birthDate: null,
  height: null,
  weight: null,
  activityLevel: null,
  dietGoal: null,
  dietPreference: '',
  avatarUrl: ''
})

const uploadUrl = '/api/user/avatar'
const uploadHeaders = computed(() => ({
  Authorization: `Bearer ${localStorage.getItem('token')}`
}))

const avatarFullUrl = computed(() => {
  if (form.avatarUrl) return form.avatarUrl
  return ''
})

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
    ElMessage.success('头像上传成功')
  } else {
    ElMessage.error(response.message || '上传失败')
  }
}

function onPreferenceChange(values) {
  form.dietPreference = values.join('、')
}

async function loadProfile() {
  try {
    const res = await api.get('/user/profile')
    const data = res.data || res
    Object.assign(form, data)
    if (form.dietPreference) {
      selectedPreferences.value = form.dietPreference.split('、').filter(Boolean)
    }
  } catch (e) {
    console.error(e)
  }
}

async function saveProfile() {
  saving.value = true
  try {
    await api.put('/user/profile', form)
    ElMessage.success('保存成功')
  } catch (e) {
    console.error(e)
  } finally {
    saving.value = false
  }
}

onMounted(loadProfile)
</script>