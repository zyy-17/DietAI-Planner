<template>
  <div class="fq">
    <div class="fq-modes">
      <button type="button" class="fq-mode" :class="{ on: item.mode === 'gram' }" @click="setMode('gram')">
        按克数
      </button>
      <button type="button" class="fq-mode" :class="{ on: item.mode === 'unit' }" @click="setMode('unit')">
        按{{ unitName }}
      </button>
    </div>

    <div class="fq-body">
      <template v-if="item.mode === 'gram'">
        <el-input-number
          v-model="item.amount"
          :min="1"
          :max="5000"
          :step="10"
          size="small"
          controls-position="right"
          style="width: 118px" />
        <span class="fq-unit">g</span>
        <span class="fq-quick">
          <a v-for="q in gramQuick" :key="q" @click="setGrams(q)">{{ q }}g</a>
        </span>
      </template>

      <template v-else>
        <el-input-number
          v-model="item.count"
          :min="0.5"
          :max="99"
          :step="1"
          :precision="1"
          size="small"
          controls-position="right"
          style="width: 104px" />
        <span class="fq-unit">{{ unitName }}</span>
        <span class="fq-quick">
          <a v-for="q in countQuick" :key="q" @click="setCount(q)">{{ trim(q) }}{{ unitName }}</a>
        </span>
        <span class="fq-per">
          每{{ unitName }}约
          <el-input-number
            v-model="item.unitWeight"
            :min="1"
            :max="2000"
            :step="5"
            size="small"
            controls-position="right"
            style="width: 96px; margin: 0 4px" />
          g
        </span>
      </template>
    </div>

    <div class="fq-result">
      ≈ <b>{{ item.amount }}</b> g · 🔥 <b>{{ kcal }}</b> kcal
    </div>
  </div>
</template>

<script setup>
import { computed, watch } from 'vue'
import { GRAM_QUICK, COUNT_QUICK } from '../utils/foodUnits'

const props = defineProps({
  /** 已选食物条目（含 mode / count / unitName / unitWeight / amount） */
  item: { type: Object, required: true },
  /** 每 100g 热量 */
  calories: { type: Number, default: 0 }
})

const gramQuick = GRAM_QUICK
const countQuick = COUNT_QUICK

const unitName = computed(() => props.item.unitName || '份')
const kcal = computed(() => (((props.calories || 0) * (props.item.amount || 0)) / 100).toFixed(0))

function trim(num) {
  const n = Number(num)
  return Number.isInteger(n) ? String(n) : String(n)
}

/** 切换计量方式；首次切到「按个数」时补上默认单位与单重 */
function setMode(mode) {
  if (mode === 'unit' && !props.item.unitName) {
    props.item.unitName = '份'
    if (!props.item.unitWeight) props.item.unitWeight = 100
  }
  props.item.mode = mode
  syncAmount()
}

function setGrams(g) {
  props.item.mode = 'gram'
  props.item.amount = g
}

function setCount(c) {
  props.item.count = c
  syncAmount()
}

/** 按个数时，克重始终由「数量 × 每个克重」推导 */
function syncAmount() {
  if (props.item.mode !== 'unit') return
  const weight = Number(props.item.unitWeight) || 0
  const count = Number(props.item.count) || 0
  props.item.amount = Number((count * weight).toFixed(1))
}

watch(() => [props.item.mode, props.item.count, props.item.unitWeight], syncAmount)
</script>

<style scoped>
.fq {
  background: #fff;
  border: 1px solid #e2edf7;
  border-radius: 8px;
  padding: 8px 10px;
}
.fq-modes {
  display: flex;
  gap: 6px;
  margin-bottom: 8px;
}
.fq-mode {
  border: 1px solid #d9e8f5;
  background: #f6fbff;
  color: #4384b8;
  border-radius: 14px;
  padding: 3px 14px;
  font-size: 12px;
  cursor: pointer;
  line-height: 1.6;
}
.fq-mode.on {
  background: #288df0;
  border-color: #288df0;
  color: #fff;
}
.fq-body {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
}
.fq-unit {
  font-size: 12px;
  color: #7b93a5;
}
.fq-quick {
  display: flex;
  gap: 4px;
  margin-left: 4px;
}
.fq-quick a {
  font-size: 11px;
  color: #3287dc;
  background: #f0f7ff;
  border-radius: 10px;
  padding: 2px 8px;
  cursor: pointer;
  white-space: nowrap;
}
.fq-quick a:hover {
  background: #e0efff;
}
.fq-per {
  display: inline-flex;
  align-items: center;
  font-size: 12px;
  color: #7b93a5;
  margin-left: 6px;
}
.fq-result {
  margin-top: 7px;
  font-size: 12px;
  color: #7b93a5;
}
.fq-result b {
  color: #244b6b;
}
.fq-result b:last-child {
  color: #e6a23c;
}
</style>
