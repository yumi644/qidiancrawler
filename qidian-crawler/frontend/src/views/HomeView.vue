<template>
  <div>
    <div style="display:flex; gap: 8px; align-items:center; flex-wrap: wrap;">
      <input v-model="bookId" placeholder="bookId (e.g. 123456)" style="padding:8px; width: 260px;" />
      <input v-model="url" placeholder="URL" style="padding:8px; width: 420px;" />
      <button @click="onCrawl" :disabled="loading" style="padding:8px 12px;">Crawl</button>
      <span v-if="msg" style="color:#444;">{{ msg }}</span>
    </div>

    <h3 style="margin-top: 16px;">Novels</h3>
    <button @click="load" :disabled="loading" style="padding:6px 10px;">Refresh</button>

    <div v-if="loading" style="margin-top: 8px;">Loading...</div>
    <table v-else style="width:100%; border-collapse: collapse; margin-top: 8px;">
      <thead>
        <tr>
          <th style="text-align:left; border-bottom:1px solid #ddd; padding:8px;">Title</th>
          <th style="text-align:left; border-bottom:1px solid #ddd; padding:8px;">Author</th>
          <th style="text-align:left; border-bottom:1px solid #ddd; padding:8px;">BookId</th>
          <th style="text-align:left; border-bottom:1px solid #ddd; padding:8px;">Last Crawled</th>
          <th style="text-align:left; border-bottom:1px solid #ddd; padding:8px;">Download</th>
          <th style="text-align:left; border-bottom:1px solid #ddd; padding:8px;">CrawlBook</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="n in novels" :key="n.id" @click="go(n.id)" style="cursor:pointer;">
          <td style="border-bottom:1px solid #f0f0f0; padding:8px;">{{ n.title }}</td>
          <td style="border-bottom:1px solid #f0f0f0; padding:8px;">{{ n.author || '-' }}</td>
          <td style="border-bottom:1px solid #f0f0f0; padding:8px;">{{ n.qidianBookId }}</td>
          <td style="border-bottom:1px solid #f0f0f0; padding:8px;">{{ n.lastCrawledAt || '-' }}</td>
          <td style="border-bottom:1px solid #f0f0f0; padding:8px;"><button @click.stop="download(n)"
              style="padding:4px 8px;">下载</button></td>
          <td style="border-bottom:1px solid #f0f0f0; padding:8px;"><button @click.stop="crawlBook(n)"
              style="padding:4px 8px;">爬取全文</button></td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import { crawlNovel, listNovels, type Novel } from '../lib/api'

const router = useRouter()

const bookId = ref('')
const url = ref('')
const msg = ref('')
const loading = ref(false)
const novels = ref<Novel[]>([])

function go(id: number) {
  router.push(`/novels/${id}`)
}

async function load() {
  loading.value = true
  try {
    novels.value = await listNovels()
  } finally {
    loading.value = false
  }
}

async function onCrawl() {
  msg.value = ''
  loading.value = true
  try {
    await crawlNovel({ bookId: bookId.value || undefined, url: url.value || undefined })
    msg.value = 'Crawl started / finished (depending on site response).'
    await load()
  } catch (e: any) {
    msg.value = e?.response?.data?.message || e?.message || 'Crawl failed'
  } finally {
    loading.value = false
  }
}

async function download(novel: Novel) {
  msg.value = ''
  try {
    const r = await axios.get(`/api/novels/${novel.id}/download`, { responseType: 'blob' })
    const blob = r.data as Blob

    let filename = 'novel.txt'
    const cd = r.headers?.['content-disposition'] as string | undefined
    if (cd) {
      const m = cd.match(/filename\*=UTF-8''([^;]+)/i)
      if (m && m[1]) filename = decodeURIComponent(m[1])
      else {
        const m2 = cd.match(/filename="?([^";]+)"?/i)
        if (m2 && m2[1]) filename = m2[1]
      }
    }

    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = filename
    document.body.appendChild(a)
    a.click()
    a.remove()
    window.URL.revokeObjectURL(url)
  } catch (e: any) {
    msg.value = e?.response?.data?.message || e?.message || 'Download failed'
  } finally {
    loading.value = false
  }
}

async function crawlBook(novel: Novel) {
  msg.value = ''
  loading.value = true
  try {
    await axios.post(`/api/novels/${novel.id}/crawl`)
    msg.value = 'Crawl started / finished (depending on site response).'
    await load()
  } catch (e: any) {
    msg.value = e?.response?.data?.message || e?.message || 'Crawl failed'
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>
