<template>
    <div>
        <div style="margin-top: 12px;">
            <h4 style="margin-bottom: 6px;">{{ chapter?.title || '' }}</h4>
            <div v-if="chapterLoading" style="color:#666;">Loading chapter...</div>
            <pre v-else
                style="white-space: pre-wrap; line-height: 1.6; background:#fafafa; border:1px solid #eee; padding:12px;">{{ chapter?.content || '(no content)' }}</pre>
            <div v-if="msg" style="margin-top:8px; color:#666;">{{ msg }}</div>
        </div>

        <div style="justify-content: center; display: flex;align-items: center;gap: 50px;margin-top: 12px;">

            <router-link v-if="beforeChapterId > 0" :to="`/content/${beforeChapterId}`"
                style="text-decoration:none; color:inherit;">上一章</router-link>

            <div><a :href="`/novels/${novelId || ''}`" style="text-decoration:none;">目录</a></div>

            <router-link :to="`/content/${afterChapterId}`"
                style="text-decoration:none; color:inherit;">下一章</router-link>
        </div>
    </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { crawlChapter, getChapter, type Chapter } from '../lib/api'

const route = useRoute()
const chapterLoading = ref(false)
const msg = ref('')
const chapter = ref<Chapter | null>(null)
const novelId = ref<number | null>(null)
const chapterId = computed(() => Number(route.params.id))
const beforeChapterId = computed(() => chapterId.value - 1)
const afterChapterId = computed(() => chapterId.value + 1)

async function load() {
    const chapterId = Number(route.params.id)

    if (!Number.isFinite(chapterId) || chapterId <= 0) {
        msg.value = 'Invalid chapter id'
        return
    }

    chapterLoading.value = true
    msg.value = ''
    try {
        let ch = await getChapter(chapterId)
        if (!ch.content || ch.content.trim().length < 20) {
            ch = await crawlChapter(chapterId)
        }
        chapter.value = ch
        novelId.value = ch.novelId
    } catch (e: any) {
        msg.value = e?.response?.data?.message || e?.message || 'Load chapter failed'
    } finally {
        chapterLoading.value = false
    }
}
watch(() => route.params.id, load)
onMounted(load)
</script>

<style scoped></style>