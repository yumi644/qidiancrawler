import axios from 'axios'
import type { AxiosResponse } from 'axios'

export interface Novel {
  id: number
  qidianBookId: string
  title: string
  author?: string
  intro?: string
  coverUrl?: string
  bookUrl?: string
  lastCrawledAt?: string
}

export interface Chapter {
  id: number
  novelId: number
  title: string
  chapterUrl?: string
  isFree: boolean
  contentCrawled: boolean
}

export async function crawlNovel(input: { bookId?: string; url?: string }) {
  return axios.post('/api/crawl/novel', input).then((r: AxiosResponse) => r.data)
}

export async function listNovels() {
  return axios.get('/api/novels').then((r: AxiosResponse) => r.data as Novel[])
}

export async function getNovel(id: number) {
  return axios.get(`/api/novels/${id}`).then((r: AxiosResponse) => r.data as Novel)
}

export async function listChapters(novelId: number) {
  return axios.get(`/api/novels/${novelId}/chapters`).then((r: AxiosResponse) => r.data as Chapter[])
}
