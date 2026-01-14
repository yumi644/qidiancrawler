<template>
  <div>
    <div>
      <a href="/" style="text-decoration:none;">Back</a>
    </div>

    <div v-if="loading" style="margin-top: 8px;">Loading...</div>
    <div v-else>
      <h3>{{ novel?.title }}</h3>
      <div style="color:#666;">Author: {{ novel?.author || '-' }}</div>
      <p style="white-space: pre-wrap;">{{ novel?.intro || '' }}</p>

      <h4>Chapters</h4>
      <table style="width:100%; border-collapse: collapse; margin-top: 8px;">
        <thead>
          <tr>
            <th style="text-align:left; border-bottom:1px solid #ddd; padding:8px;">Title</th>
            <th style="text-align:left; border-bottom:1px solid #ddd; padding:8px;">Free</th>
            <th style="text-align:left; border-bottom:1px solid #ddd; padding:8px;">Content Crawled</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="c in chapters" :key="c.id">
            <td style="border-bottom:1px solid #f0f0f0; padding:8px;">
              <router-link :to="`/content/${c.id}`" style="text-decoration:none; color:inherit;">{{ c.title
                }}</router-link>
            </td>
            <td style="border-bottom:1px solid #f0f0f0; padding:8px;">{{ c.free ? 'Y' : 'N' }}</td>
            <td style="border-bottom:1px solid #f0f0f0; padding:8px;">{{ c.contentCrawled ? 'Y' : 'N' }}</td>
          </tr>
        </tbody>
      </table>

      <div v-if="msg" style="margin-top:8px; color:#666;">{{ msg }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { getNovel, listChapters, type Chapter, type Novel } from '../lib/api'
const route = useRoute()
const id = Number(route.params.id)

const loading = ref(false)
const novel = ref<Novel | null>(null)
const chapters = ref<Chapter[]>([])
const msg = ref('')

async function load() {
  loading.value = true
  try {
    novel.value = await getNovel(id)
    chapters.value = await listChapters(id)
  } catch (e: any) {
    msg.value = e?.response?.data?.message || e?.message || 'Load failed'
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>
