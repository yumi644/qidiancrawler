package com.example.qidian.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "novel", indexes = {
    @Index(name = "idx_novel_qidian_book_id", columnList = "qidian_book_id", unique = true)
})
public class Novel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "qidian_book_id", nullable = false, length = 32)
    private String qidianBookId;

    @Column(name = "title", nullable = false, length = 256)
    private String title;

    @Column(name = "author", length = 128)
    private String author;

    @Lob
    @Column(name = "intro")
    private String intro;

    @Column(name = "cover_url", length = 1024)
    private String coverUrl;

    @Column(name = "book_url", length = 1024)
    private String bookUrl;

    @Column(name = "last_crawled_at")
    private OffsetDateTime lastCrawledAt;

    public Novel() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getQidianBookId() {
        return qidianBookId;
    }

    public void setQidianBookId(String qidianBookId) {
        this.qidianBookId = qidianBookId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getIntro() {
        return intro;
    }

    public void setIntro(String intro) {
        this.intro = intro;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    public String getBookUrl() {
        return bookUrl;
    }

    public void setBookUrl(String bookUrl) {
        this.bookUrl = bookUrl;
    }

    public OffsetDateTime getLastCrawledAt() {
        return lastCrawledAt;
    }

    public void setLastCrawledAt(OffsetDateTime lastCrawledAt) {
        this.lastCrawledAt = lastCrawledAt;
    }
}
