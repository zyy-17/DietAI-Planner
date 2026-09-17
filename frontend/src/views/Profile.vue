<template>
  <div class="profile-page">
    <el-card style="max-width:600px;margin:0 auto">
      <template #header><span>个人中心</span></template>
      <el-form :model="form" label-width="100px">
        <el-form-item label="真实姓名"><el-input v-model="form.realName" /></el-form-item>
        <el-form-item label="性别">
          <el-radio-group v-model="form.gender">
            <el-radio :value="1">男</el-radio>
            <el-radio :value="2">女</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="出生日期"><el-date-picker v-model="form.birthDate" type="date" /></el-form-item>
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
        <el-form-item>
          <el-button type="primary" @click="saveProfile" :loading="saving">保存</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../utils/api'

const saving = ref(false)
const form = reactive({ realName: '', gender: null, birthDate: null, height: null, weight: null, activityLevel: null, dietGoal: null })

async function loadProfile() {
  const data = await api.get('/user/profile')
  Object.assign(form, data)
}

async function saveProfile() {
  saving.value = true
  try {
    await api.put('/user/profile', form)
    ElMessage.success('保存成功')
  } finally {
    saving.value = false
  }
}

onMounted(loadProfile)
</script>