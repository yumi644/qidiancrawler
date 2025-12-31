package com.example.qidian.web.dto;

import jakarta.validation.constraints.Size;

public class CrawlNovelRequest {

    @Size(max = 1024)
    private String url;

    @Size(max = 32)
    private String bookId;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getBookId() {
        return bookId;
    }

    public void setBookId(String bookId) {
        this.bookId = bookId;
    }
}
