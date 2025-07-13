import { createApp } from 'vue'
import './style.css'
import App from './App.vue'
import { createRouter, createWebHistory } from 'vue-router'
import PortfolioInput from './components/PortfolioInput.vue'
import PortfolioResult from './components/PortfolioResult.vue'

const routes = [
  { path: '/', component: PortfolioInput },
  { path: '/result', component: PortfolioResult }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

createApp(App).use(router).mount('#app')
