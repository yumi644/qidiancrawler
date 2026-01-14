import axios from "axios";
import type { AxiosResponse } from "axios";

// 后端 API 返回的“小说”数据结构（TypeScript 类型，仅用于前端类型检查/提示）
export interface Novel {
  id: number;
  qidianBookId: string;
  title: string;
  author?: string;
  intro?: string;
  coverUrl?: string;
  bookUrl?: string;
  lastCrawledAt?: string;
}

// 后端 API 返回的“章节”数据结构。
// 注意：只有在正文已抓取后，`content` 才会有值；`contentCrawled` 表示是否已抓取过正文。
export interface Chapter {
  id: number;
  novelId: number;
  title: string;
  chapterUrl?: string;
  free: boolean;
  contentCrawled: boolean;
  content?: string;
}

// 批量抓取正文的统计结果
export interface BatchCrawlResult {
  total: number;
  success: number;
  skipped: number;
  failed: number;
}

// 触发“整本小说爬取”（目录 + 基础信息）。
// POST /api/crawl/novel
// - input.bookId：站点 bookId（例如 qidian:123 或 bqg:106006）
// - input.url：也可以直接传小说页面/目录 URL，后端会尝试自动解析
export async function crawlNovel(input: { bookId?: string; url?: string }) {
  return axios
    .post("/api/crawl/novel", input)
    .then((r: AxiosResponse) => r.data);
}

// 获取已入库的小说列表
// GET /api/novels
export async function listNovels() {
  return axios.get("/api/novels").then((r: AxiosResponse) => r.data as Novel[]);
}

// 根据小说数据库 id 获取小说详情
// GET /api/novels/{id}
export async function getNovel(id: number) {
  return axios
    .get(`/api/novels/${id}`)
    .then((r: AxiosResponse) => r.data as Novel);
}

// 获取某本小说的章节目录（不一定包含正文）
// GET /api/novels/{novelId}/chapters
export async function listChapters(novelId: number) {
  return axios
    .get(`/api/novels/${novelId}/chapters`)
    .then((r: AxiosResponse) => r.data as Chapter[]);
}

// 获取单章详情（如果未抓取正文，content 可能为空）
// GET /api/chapters/{id}
export async function getChapter(id: number) {
  return axios
    .get(`/api/chapters/${id}`)
    .then((r: AxiosResponse) => r.data as Chapter);
}

// 按需抓取单章正文，并返回更新后的章节记录
// POST /api/chapters/{id}/crawl
export async function crawlChapter(id: number) {
  return axios
    .post(`/api/chapters/${id}/crawl`)
    .then((r: AxiosResponse) => r.data as Chapter);
}

// 批量抓取某本小说的所有章节正文（同步执行）
// POST /api/novels/{id}/crawl
export async function crawlNovelContent(novelId: number) {
  return axios
    .post(`/api/novels/${novelId}/crawl`)
    .then((r: AxiosResponse) => r.data as BatchCrawlResult);
}

export async function downloadNovel(id: number) {
  return axios.get(`/api/novels/${id}/download`, { responseType: 'blob' });
}
