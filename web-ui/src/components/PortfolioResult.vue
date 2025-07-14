<template>
  <div class="notion-result-wrapper">
    <div class="notion-result">
      <h2>投资组合分析结果</h2>
      <transition name="fade">
        <div v-if="result">
          <div class="result-row">
            <strong>操作建议:</strong>
            <span class="notion-tag notion-tag-action">{{ result.action }}</span>
          </div>
          <div class="result-row">
            <strong>交易数量:</strong>
            <span class="notion-tag">{{ result.quantity }}</span>
          </div>
          <div class="result-row">
            <strong>决策置信度:</strong>
            <span class="notion-tag notion-tag-confidence">{{ (result.confidence * 100).toFixed(2) }}%</span>
          </div>
          <h3>分析师信号</h3>
          <table class="notion-table">
            <thead>
              <tr>
                <th>分析师</th>
                <th>信号</th>
                <th>置信度</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="signal in result.agent_signals" :key="signal.agent_name">
                <td>{{ signal.agent_name }}</td>
                <td><span class="notion-tag notion-tag-signal">{{ signal.signal }}</span></td>
                <td><span class="notion-tag notion-tag-confidence">{{ (signal.confidence * 100).toFixed(2) }}%</span></td>
              </tr>
            </tbody>
          </table>
          <div class="result-row">
            <strong>推理说明:</strong>
            <span class="notion-block">{{ result.reasoning }}</span>
          </div>
          <div v-if="result['分析报告']">
            <h3>分析报告</h3>
            <div class="notion-block-report notion-block-report-rich">
              <div v-html="marked.parse(result['分析报告'])"></div>
            </div>
          </div>
        </div>
        <div v-else>
          <p class="notion-loading">正在加载分析结果...</p>
        </div>
      </transition>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { marked } from 'marked'

const result = ref(null)

onMounted(() => {
  const data = localStorage.getItem('portfolio_result')
  if (data) {
    result.value = JSON.parse(data)
  } else {
    result.value = null
  }
})
</script>

<style scoped>
.notion-result-wrapper {
  min-height: 100vh;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  background: #fafbfc;
  padding-top: 48px;
}
.notion-result {
  width: 100%;
  max-width: 700px;
  padding: 0 0 40px 0;
  display: flex;
  flex-direction: column;
  gap: 32px;
  font-family: 'Inter', 'Segoe UI', 'Helvetica Neue', Arial, sans-serif;
  font-size: 1.08em;
  line-height: 1.7;
}
.notion-result h2 {
  font-size: 1.7em;
  font-weight: 700;
  color: #222;
  margin-bottom: 10px;
  letter-spacing: 0.01em;
  text-align: left;
}
.notion-result h3 {
  font-size: 1.15em;
  font-weight: 600;
  color: #444;
  margin: 24px 0 10px 0;
  text-align: left;
}
.result-row {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 1.05em;
  color: #333;
  margin-bottom: 8px;
}
.notion-tag {
  display: inline-block;
  background: #f3f4f6;
  color: #555;
  border-radius: 6px;
  padding: 2px 10px;
  font-size: 0.98em;
  font-weight: 500;
  margin-left: 2px;
  transition: background 0.2s, color 0.2s;
}
.notion-tag-action {
  background: #e6f4ff;
  color: #2563eb;
}
.notion-tag-confidence {
  background: #f0f9eb;
  color: #2e7d32;
}
.notion-tag-signal {
  background: #f8f5ff;
  color: #7c3aed;
}
.notion-table {
  border-collapse: separate;
  border-spacing: 0;
  width: 100%;
  margin: 16px 0 24px 0;
  background: #fff;
  box-shadow: 0 1px 2px 0 rgba(60,60,60,0.02);
  border-radius: 8px;
  overflow: hidden;
  font-size: 1em;
  transition: box-shadow 0.2s;
}
.notion-table th, .notion-table td {
  padding: 12px 16px;
  text-align: left;
  font-size: 1em;
  transition: background 0.2s;
}
.notion-table th {
  background: #f3f4f6;
  color: #555;
  font-weight: 600;
  border-bottom: 1.5px solid #e3e4e8;
}
.notion-table td {
  background: #fff;
  color: #222;
  border-bottom: 1px solid #f3f4f6;
}
.notion-table tr:last-child td {
  border-bottom: none;
}
.notion-table tbody tr {
  transition: background 0.18s;
}
.notion-table tbody tr:hover {
  background: #f6faff;
}
.notion-block {
  display: inline-block;
  background: #f3f4f6;
  border-radius: 6px;
  padding: 8px 12px;
  margin: 4px 0;
  color: #222;
  font-size: 1em;
  line-height: 1.7;
  font-family: inherit;
  transition: background 0.2s;
}
.notion-block-report {
  display: block;
  background: #f8fafc;
  border-left: 4px solid #a3a9b8;
  margin-top: 10px;
  padding: 18px 22px 18px 26px;
  overflow-x: auto;
}
.notion-block-report-rich {
  display: flex;
  flex-direction: column;
  gap: 18px;
}
.notion-report-heading {
  font-size: 1.13em;
  font-weight: 700;
  color: #2563eb;
  margin: 0 0 6px 0;
  padding-left: 2px;
  letter-spacing: 0.01em;
}
.notion-report-ul {
  margin: 0 0 8px 18px;
  padding: 0;
  list-style: disc inside;
  color: #2e7d32;
  font-size: 1.01em;
  line-height: 1.8;
}
.notion-report-ol {
  margin: 0 0 8px 18px;
  padding: 0;
  list-style: decimal inside;
  color: #7c3aed;
  font-size: 1.01em;
  line-height: 1.8;
}
.notion-report-paragraph {
  font-size: 1.04em;
  color: #222;
  line-height: 1.8;
  margin: 0;
  padding: 0;
  word-break: break-word;
}
.notion-pre {
  background: transparent;
  padding: 0;
  border-radius: 0;
  font-size: 1em;
  color: #222;
  white-space: pre-wrap;
  font-family: inherit;
  margin: 0;
}
.notion-loading {
  color: #888;
  font-size: 1.1em;
  text-align: center;
  margin-top: 32px;
}
.fade-enter-active, .fade-leave-active {
  transition: opacity 0.3s;
}
.fade-enter-from, .fade-leave-to {
  opacity: 0;
}
@media (max-width: 800px) {
  .notion-result {
    max-width: 98vw;
    padding: 0 2vw 32px 2vw;
  }
}
@media (max-width: 500px) {
  .notion-result-wrapper {
    padding-top: 16px;
  }
  .notion-result {
    font-size: 0.98em;
    gap: 18px;
  }
  .notion-table th, .notion-table td {
    padding: 8px 6px;
  }
}
</style> 