<template>
  <div class="notion-form-wrapper">
    <form @submit.prevent="onSubmit" class="notion-form-card">
      <h2 class="notion-title">投资组合提交</h2>
      <div v-for="field in fields" :key="field.key" class="notion-form-group">
        <label :for="field.key" class="notion-label">{{ field.label }}</label>
        <input
          :id="field.key"
          v-model="form[field.key]"
          :type="field.type || 'text'"
          :placeholder="field.placeholder"
          class="notion-input"
          :class="{ 'notion-input-error': errors[field.key] }"
          @focus="focusMap[field.key] = true"
          @blur="focusMap[field.key] = false"
        />
        <div v-if="errors[field.key]" class="notion-error">{{ errors[field.key] }}</div>
      </div>
      <button type="submit" :disabled="loading" class="notion-btn" @click="showRipple">
        <span v-if="loading" class="notion-loader"></span>
        <span v-else>提交</span>
        <span class="notion-btn-ripple" v-if="ripple.show" :style="ripple.style"></span>
      </button>
    </form>
    <transition name="fade">
      <div v-if="loading" class="notion-loading-overlay">
        <div class="notion-spinner"></div>
        <div class="notion-loading-text">正在分析，请稍候…</div>
      </div>
    </transition>
    <transition name="fade">
      <div v-if="error" class="notion-error-msg">{{ error }}</div>
    </transition>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'

// 表单字段配置
const fields = [
  { key: 'symbol', label: '交易对 (symbol)', placeholder: '如 BTCusdt', type: 'text' },
  { key: 'interval', label: '周期 (interval)', placeholder: '如 30m', type: 'text' },
  { key: 'limit', label: '数据条数 (limit)', placeholder: '如 200', type: 'number' },
  { key: 'cash', label: '现金 (cash)', placeholder: '如 10000', type: 'number' },
  { key: 'stock', label: '持仓 (stock)', placeholder: '如 0.01，可为小数', type: 'text' }, // 明确允许小数
]

const router = useRouter()
const form = reactive({
  symbol: 'BTCusdt',
  interval: '30m',
  limit: '200',
  cash: '10000',
  stock: '0.01',
})
const errors = reactive({})
const loading = ref(false)
const error = ref('')
const focusMap = reactive({})
fields.forEach(f => focusMap[f.key] = false)

// 按钮波纹动效
const ripple = reactive({ show: false, style: {} })
function showRipple(e) {
  if (!e) return
  const btn = e.currentTarget
  const rect = btn.getBoundingClientRect()
  const size = Math.max(rect.width, rect.height)
  const x = e.clientX - rect.left - size / 2
  const y = e.clientY - rect.top - size / 2
  ripple.style = {
    width: size + 'px',
    height: size + 'px',
    left: x + 'px',
    top: y + 'px',
  }
  ripple.show = true
  setTimeout(() => (ripple.show = false), 400)
}

// 表单校验
function validate() {
  let valid = true
  fields.forEach(field => {
    errors[field.key] = ''
    const val = form[field.key]
    if (!val || String(val).trim() === '') {
      errors[field.key] = '必填项'
      valid = false
    } else if (field.key === 'stock') {
      // 持仓允许任意小数
      if (isNaN(Number(val))) {
        errors[field.key] = '请输入有效数字（可为小数）'
        valid = false
      }
    } else if (field.type === 'number' && isNaN(Number(val))) {
      errors[field.key] = '请输入有效数字'
      valid = false
    }
  })
  return valid
}

// 提交处理
async function onSubmit(e) {
  if (e) showRipple(e)
  if (!validate()) return
  loading.value = true
  error.value = ''
  try {
    const params = {
      symbol: form.symbol,
      interval: form.interval,
      limit: Number(form.limit),
      portfolio: {
        cash: Number(form.cash),
        stock: Number(form.stock)
      }
    }
    // 可切换为实际API
    const res = await fetch('http://localhost:8080/api/getPortfolioManagement', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(params)
    })
    if (!res.ok) throw new Error('接口请求失败')
    const data = await res.json()
    localStorage.setItem('portfolio_result', JSON.stringify(data))
    router.push({ path: '/result' })
  } catch (e) {
    error.value = e.message || '请求失败'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.notion-form-wrapper {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
  font-family: 'Inter', 'Segoe UI', 'Helvetica Neue', Arial, sans-serif;
}
.notion-form-card {
  background: #fff;
  border-radius: 14px;
  box-shadow: 0 4px 24px 0 rgba(60,60,60,0.10), 0 1.5px 4px 0 rgba(60,60,60,0.04);
  padding: 38px 32px 32px 32px;
  margin: 0 auto;
  width: 100%;
  max-width: 420px;
  display: flex;
  flex-direction: column;
  gap: 24px;
  transition: box-shadow 0.2s;
}
.notion-title {
  font-size: 1.5em;
  font-weight: 700;
  color: #2563eb;
  margin-bottom: 8px;
  letter-spacing: 0.01em;
  text-align: left;
}
.notion-form-group {
  margin-bottom: 18px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.notion-label {
  font-size: 1.08em;
  color: #444;
  font-weight: 500;
  margin-bottom: 2px;
}
.notion-input {
  width: 100%;
  padding: 12px 10px;
  border: 1.5px solid #e3e4e8;
  border-radius: 7px;
  background: #f8fafc;
  font-size: 1.08em;
  color: #222;
  outline: none;
  transition: border-color 0.2s, background 0.2s;
}
.notion-input:focus {
  border-color: #2563eb;
  background: #fff;
}
.notion-input-error {
  border-color: #e74c3c;
  background: #fff0f0;
}
.notion-error {
  color: #e74c3c;
  font-size: 0.98em;
  margin-top: 2px;
}
.notion-btn {
  margin-top: 18px;
  padding: 14px 0;
  font-size: 1.12em;
  background: linear-gradient(90deg, #2563eb 0%, #4f8cff 100%);
  color: #fff;
  border: none;
  border-radius: 8px;
  font-weight: 700;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  transition: background 0.18s, color 0.18s, box-shadow 0.18s;
  box-shadow: 0 2px 8px 0 rgba(60,60,60,0.06);
  letter-spacing: 0.01em;
}
.notion-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}
.notion-btn-ripple {
  position: absolute;
  border-radius: 50%;
  background: rgba(255,255,255,0.4);
  transform: scale(0);
  animation: ripple 0.4s linear;
  pointer-events: none;
  z-index: 2;
}
@keyframes ripple {
  to {
    transform: scale(2.5);
    opacity: 0;
  }
}
.notion-loader {
  border: 3px solid #f3f3f3;
  border-top: 3px solid #2563eb;
  border-radius: 50%;
  width: 18px;
  height: 18px;
  animation: spin 1s linear infinite;
  margin-right: 8px;
  display: inline-block;
}
@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}
.notion-loading-overlay {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(250,251,252,0.85);
  z-index: 1000;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}
.notion-spinner {
  border: 6px solid #e3e4e8;
  border-top: 6px solid #2563eb;
  border-radius: 50%;
  width: 48px;
  height: 48px;
  animation: spin 1s linear infinite;
  margin-bottom: 18px;
}
.notion-loading-text {
  font-size: 1.1em;
  color: #555;
  font-weight: 500;
}
.notion-error-msg {
  color: #e74c3c;
  background: #fff0f0;
  border: 1px solid #e0b4b4;
  border-radius: 8px;
  padding: 12px 18px;
  margin: 18px auto 0 auto;
  max-width: 400px;
  text-align: center;
  font-size: 1.04em;
  box-shadow: 0 1px 2px 0 rgba(60,60,60,0.03);
}
.fade-enter-active, .fade-leave-active {
  transition: opacity 0.3s;
}
.fade-enter-from, .fade-leave-to {
  opacity: 0;
}
@media (max-width: 600px) {
  .notion-form-card {
    padding: 18px 6vw 18px 6vw;
    border-radius: 10px;
    box-shadow: 0 2px 8px 0 rgba(60,60,60,0.10);
  }
  .notion-title {
    font-size: 1.18em;
  }
}
</style> 