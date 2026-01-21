import { createRouter, createWebHistory } from "vue-router";
import HomeView from "../views/HomeView.vue";
import NovelDetailView from "../views/NovelDetailView.vue";
import Content from "../views/Content.vue";
import Login from "../views/Login.vue";

const routes = [
  { path: "/", component: HomeView },
  { path: "/login", component: Login },
  { path: "/novels/:id", component: NovelDetailView, props: true },
  { path: "/content/:id", component: Content, props: true },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

export default router;
