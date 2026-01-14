package com.example.qidian.domain;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

@Entity
@Table(name = "chapter", indexes = {
    @Index(name = "idx_chapter_novel_id", columnList = "novel_id"),
    @Index(name = "idx_chapter_url", columnList = "chapter_url")
})
public class Chapter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "novel_id", nullable = false)
    private Long novelId;

    @Column(name = "title", nullable = false, length = 256)
    private String title;

    @Column(name = "chapter_url", length = 768)
    private String chapterUrl;

    @Column(name = "is_free", nullable = false)
    private boolean free;

    @Column(name = "content_crawled", nullable = false)
    private boolean contentCrawled;

    @Lob
    @Column(name = "content", columnDefinition = "LONGTEXT")
    private String content;

    @Column(name = "crawled_at")
    private OffsetDateTime crawledAt;

    public Chapter() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getNovelId() {
        return novelId;
    }

    public void setNovelId(Long novelId) {
        this.novelId = novelId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getChapterUrl() {
        return chapterUrl;
    }

    public void setChapterUrl(String chapterUrl) {
        this.chapterUrl = chapterUrl;
    }

    public boolean isFree() {
        return free;
    }

    public void setFree(boolean free) {
        this.free = free;
    }

    public boolean isContentCrawled() {
        return contentCrawled;
    }

    public void setContentCrawled(boolean contentCrawled) {
        this.contentCrawled = contentCrawled;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public OffsetDateTime getCrawledAt() {
        return crawledAt;
    }

    public void setCrawledAt(OffsetDateTime crawledAt) {
        this.crawledAt = crawledAt;
    }
}
