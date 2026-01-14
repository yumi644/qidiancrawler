package com.example.qidian.web;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.qidian.domain.Chapter;
import com.example.qidian.domain.Novel;
import com.example.qidian.repo.ChapterRepository;
import com.example.qidian.repo.NovelRepository;
import com.example.qidian.service.QidianCrawlerService;

@RestController
@RequestMapping("/api")
public class NovelController {

    // 小说信息表的持久化访问（书名/作者/封面/来源ID 等）
    private final NovelRepository novelRepository;
    // 章节表的持久化访问（目录 + 正文抓取结果）
    private final ChapterRepository chapterRepository;
    // 爬虫服务（支持多站点：起点 + 笔趣阁类 apibi.cc）
    private final QidianCrawlerService crawlerService;

    public NovelController(NovelRepository novelRepository, ChapterRepository chapterRepository, QidianCrawlerService crawlerService) {
        this.novelRepository = novelRepository;
        this.chapterRepository = chapterRepository;
        this.crawlerService = crawlerService;
    }

    @GetMapping("/novels")
    // 获取小说列表
    // GET /api/novels
    public List<Novel> listNovels() {
        return novelRepository.findAll();
    }

    @GetMapping("/novels/{id}")
    // 获取小说详情（按数据库主键 id）
    // GET /api/novels/{id}
    public Novel getNovel(@PathVariable Long id) {
        return novelRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("novel not found"));
    }

    @PostMapping("/novels/{id}/crawl")
    // 批量抓取某本小说的所有章节正文（同步执行，返回统计信息）
    // POST /api/novels/{id}/crawl
    public QidianCrawlerService.BatchCrawlResult crawlNovelContent(@PathVariable Long id) {
        novelRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("novel not found"));
        return crawlerService.crawlAllChapterContent(id);
    }

    @GetMapping("/novels/{id}/download")
    // 下载整本小说
    public ResponseEntity<byte[]> downloadNovel(@PathVariable Long id) {
        Novel novel = novelRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("novel not found"));
        List<Chapter> chapters = chapterRepository.findByNovelIdOrderByIdAsc(id);

        StringBuilder sb = new StringBuilder();
        sb.append(novel.getTitle() == null ? "" : novel.getTitle()).append("\n");
        sb.append("\n");
        if (novel.getAuthor() != null && !novel.getAuthor().isBlank()) {
            sb.append("Author: ").append(novel.getAuthor()).append("\n");
        }
        if (novel.getQidianBookId() != null && !novel.getQidianBookId().isBlank()) {
            sb.append("BookId: ").append(novel.getQidianBookId()).append("\n");
        }
        if (novel.getBookUrl() != null && !novel.getBookUrl().isBlank()) {
            sb.append("URL: ").append(novel.getBookUrl()).append("\n");
        }
        sb.append("\n");
        if (novel.getIntro() != null && !novel.getIntro().isBlank()) {
            sb.append(novel.getIntro()).append("\n\n");
        }

        for (Chapter c : chapters) {
            sb.append(c.getTitle() == null ? "" : c.getTitle()).append("\n");
            sb.append("\n");
            if (c.getContent() != null && !c.getContent().isBlank()) {
                sb.append(c.getContent().trim()).append("\n");
            } else {
                sb.append("(No content crawled for this chapter)").append("\n");
            }
            sb.append("\n\n");
        }

        String safeTitle = (novel.getTitle() == null || novel.getTitle().isBlank()) ? "novel" : novel.getTitle();
        safeTitle = safeTitle.replaceAll("[\\\\/:*?\"<>|]", "_");
        String filename = safeTitle + ".txt";

        byte[] body = sb.toString().getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + URLEncoder.encode(filename, StandardCharsets.UTF_8))
                .contentType(new MediaType("text", "plain", StandardCharsets.UTF_8))
                .body(body);
    }

    @GetMapping("/novels/{id}/chapters")
    // 获取某本小说的章节目录（按 chapter.id 正序）
    // GET /api/novels/{id}/chapters
    public List<Chapter> listChapters(@PathVariable Long id) {
        return chapterRepository.findByNovelIdOrderByIdAsc(id);
    }

    @GetMapping("/chapters/{id}")
    // 获取单章信息。
    // 若该章节未抓取正文，则返回的 content 可能为空，contentCrawled=false
    // GET /api/chapters/{id}
    public Chapter getChapter(@PathVariable Long id) {
        return chapterRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("chapter not found"));
    }

    @PostMapping("/chapters/{id}/crawl")
    // 按需抓取章节正文，并保存到数据库后返回更新结果
    // POST /api/chapters/{id}/crawl
    public Chapter crawlChapter(@PathVariable Long id) {
        return crawlerService.crawlChapterContent(id);
    }
}
