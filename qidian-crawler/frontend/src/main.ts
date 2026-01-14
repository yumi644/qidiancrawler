import { createApp } from 'vue'
import { createPinia } from 'pinia'
import { createRouter, createWebHistory } from 'vue-router'
import App from './App.vue'
import HomeView from './views/HomeView.vue'
import NovelDetailView from './views/NovelDetailView.vue'
import Content from './views/Content.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', component: HomeView },
    { path: '/novels/:id', component: NovelDetailView, props: true },
    { path: '/content/:id', component: Content, props: true }
  ]
})

createApp(App).use(createPinia()).use(router).mount('#app')
