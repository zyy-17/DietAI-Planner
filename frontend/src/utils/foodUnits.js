/**
 * 食物计量单位辅助。
 *
 * 后端 food / user_custom_food 表存有 unit_name（如「个」）与 unit_weight（每个约多少克），
 * 管理员可以为食物配置；未配置时用下面的常见食物预设兜底，
 * 让「鸡蛋」这类食物开箱即可按个数记录。
 */

/** 常见单位的参考克重（食物名 -> { unitName, unitWeight }） */
export const UNIT_PRESETS = {
  鸡蛋: { unitName: '个', unitWeight: 50 },
  鸡蛋清: { unitName: '个', unitWeight: 35 },
  牛奶: { unitName: '盒', unitWeight: 250 },
  酸奶: { unitName: '杯', unitWeight: 150 },
  豆浆: { unitName: '杯', unitWeight: 250 },
  咖啡: { unitName: '杯', unitWeight: 250 },
  可乐: { unitName: '罐', unitWeight: 330 },
  啤酒: { unitName: '罐', unitWeight: 330 },
  苹果: { unitName: '个', unitWeight: 200 },
  香蕉: { unitName: '根', unitWeight: 120 },
  橙子: { unitName: '个', unitWeight: 180 },
  橘子: { unitName: '个', unitWeight: 100 },
  梨: { unitName: '个', unitWeight: 200 },
  猕猴桃: { unitName: '个', unitWeight: 80 },
  芒果: { unitName: '个', unitWeight: 200 },
  西瓜: { unitName: '块', unitWeight: 200 },
  葡萄: { unitName: '粒', unitWeight: 8 },
  草莓: { unitName: '个', unitWeight: 15 },
  番茄: { unitName: '个', unitWeight: 150 },
  黄瓜: { unitName: '根', unitWeight: 200 },
  胡萝卜: { unitName: '根', unitWeight: 100 },
  玉米: { unitName: '根', unitWeight: 200 },
  土豆: { unitName: '个', unitWeight: 150 },
  红薯: { unitName: '个', unitWeight: 200 },
  米饭: { unitName: '碗', unitWeight: 200 },
  面条: { unitName: '碗', unitWeight: 200 },
  馒头: { unitName: '个', unitWeight: 100 },
  包子: { unitName: '个', unitWeight: 80 },
  饺子: { unitName: '个', unitWeight: 20 },
  馄饨: { unitName: '个', unitWeight: 15 },
  面包: { unitName: '片', unitWeight: 35 },
  全麦面包: { unitName: '片', unitWeight: 35 },
  吐司: { unitName: '片', unitWeight: 35 },
  饼干: { unitName: '片', unitWeight: 10 },
  花生: { unitName: '粒', unitWeight: 10 },
  核桃: { unitName: '个', unitWeight: 10 },
  巧克力: { unitName: '块', unitWeight: 10 },
  火腿肠: { unitName: '根', unitWeight: 40 },
  香肠: { unitName: '根', unitWeight: 50 },
  培根: { unitName: '片', unitWeight: 20 },
  鸡胸肉: { unitName: '块', unitWeight: 150 },
  鸡腿: { unitName: '个', unitWeight: 120 },
  豆腐: { unitName: '块', unitWeight: 100 },
  虾: { unitName: '只', unitWeight: 15 },
  蜂蜜: { unitName: '勺', unitWeight: 15 },
  燕麦: { unitName: '勺', unitWeight: 15 },
  橄榄油: { unitName: '勺', unitWeight: 10 },
  蛋白粉: { unitName: '勺', unitWeight: 30 },
  月饼: { unitName: '个', unitWeight: 100 },
  蛋糕: { unitName: '块', unitWeight: 80 }
}

/** 下拉可选的常用单位 */
export const COMMON_UNIT_NAMES = ['个', '份', '只', '块', '片', '根', '杯', '盒', '袋', '碗', '勺', '粒', '罐', '串']

/** 按克数时的快捷份量 */
export const GRAM_QUICK = [50, 100, 150, 200]

/** 按个数时的快捷数量 */
export const COUNT_QUICK = [0.5, 1, 2, 3]

/**
 * 解析食物的计量单位：优先后端配置，其次内置预设。
 * @returns {{ unitName: string, unitWeight: number|null }}
 */
export function resolveUnit(food) {
  if (!food) return { unitName: '', unitWeight: null }
  const name = food.unitName && String(food.unitName).trim()
  const weight = Number(food.unitWeight)
  if (name) {
    return { unitName: name, unitWeight: weight > 0 ? weight : null }
  }
  const preset = UNIT_PRESETS[food.name]
  if (preset) {
    return { unitName: preset.unitName, unitWeight: preset.unitWeight }
  }
  return { unitName: '', unitWeight: null }
}

/** 把条目上的份量格式化成展示文案：按个数时显示「2个 (100g)」 */
export function formatPortion(item) {
  if (!item) return ''
  const grams = Number(item.amount || 0)
  if (item.mode === 'unit') {
    const count = Number(item.count || 0)
    const unit = item.unitName || '份'
    return `${trimZero(count)}${unit}（${trimZero(grams)}g）`
  }
  return `${trimZero(grams)}g`
}

function trimZero(num) {
  const n = Number(num || 0)
  return Number.isInteger(n) ? String(n) : String(Number(n.toFixed(2)))
}
