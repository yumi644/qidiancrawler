import { createRouter, createWebHistory } from "vue-router";
import Home from "../views/Home.vue";
import NovelDetailView from "../views/NovelDetailView.vue";
import Content from "../views/Content.vue";

const routes = [
  { path: "/", component: Home },
  { path: "/novel/:id", component: NovelDetailView },
  { path: "/content/:id", component: Content },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

export default router;
