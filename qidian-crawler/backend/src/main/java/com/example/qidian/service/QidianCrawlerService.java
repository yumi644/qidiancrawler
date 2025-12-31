package com.example.qidian.service;

import com.example.qidian.config.CrawlerProperties;
import com.example.qidian.domain.Chapter;
import com.example.qidian.domain.Novel;
import com.example.qidian.repo.ChapterRepository;
import com.example.qidian.repo.NovelRepository;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class QidianCrawlerService {

    private static final Pattern BOOK_ID_PATTERN = Pattern.compile("/book/(\\d+)");

    private final NovelRepository novelRepository;
    private final ChapterRepository chapterRepository;
    private final CrawlerProperties props;
    private final RateLimiter rateLimiter;

    public QidianCrawlerService(
            NovelRepository novelRepository,
            ChapterRepository chapterRepository,
            CrawlerProperties props
    ) {
        this.novelRepository = novelRepository;
        this.chapterRepository = chapterRepository;
        this.props = props;
        this.rateLimiter = new RateLimiter(props.getMinDelayMs(), props.getMaxDelayMs());
    }

    public String normalizeBookId(String bookId, String url) {
        if (StringUtils.hasText(bookId)) {
            return bookId.trim();
        }
        if (!StringUtils.hasText(url)) {
            throw new IllegalArgumentException("bookId or url is required");
        }
        String u = url.trim();
        Matcher m = BOOK_ID_PATTERN.matcher(u);
        if (m.find()) {
            return m.group(1);
        }
        throw new IllegalArgumentException("Cannot parse bookId from url: " + u);
    }

    public CrawlResult crawlNovelAndCatalog(String qidianBookId) {
        String bookUrl = "https://www.qidian.com/book/" + qidianBookId + "/";

        Document bookDoc = get(bookUrl);

        String title = textFirst(bookDoc, "h1 em, .book-info h1 em, .book-info h1");
        if (!StringUtils.hasText(title)) {
            throw new IllegalArgumentException("Failed to parse title from book page. Site structure may have changed.");
        }

        String author = textFirst(bookDoc, ".writer, .book-info .writer, .book-info a.writer");
        String intro = textFirst(bookDoc, ".intro, .book-intro p, .book-info .intro");

        String coverUrl = null;
        Element img = bookDoc.selectFirst(".book-img img, .cover img");
        if (img != null) {
            coverUrl = absUrl(bookUrl, img.attr("src"));
        }

        Novel novel = novelRepository.findByQidianBookId(qidianBookId)
                .orElseGet(Novel::new);
        novel.setQidianBookId(qidianBookId);
        novel.setTitle(title);
        novel.setAuthor(author);
        novel.setIntro(intro);
        novel.setCoverUrl(coverUrl);
        novel.setBookUrl(bookUrl);
        novel.setLastCrawledAt(OffsetDateTime.now());
        novel = novelRepository.save(novel);

        List<Chapter> chapters = tryParseChapters(bookDoc, bookUrl, novel.getId());
        int saved = 0;
        for (Chapter c : chapters) {
            Optional<Chapter> existed = chapterRepository.findFirstByNovelIdAndChapterUrl(novel.getId(), c.getChapterUrl());
            if (existed.isEmpty()) {
                chapterRepository.save(c);
                saved++;
            }
        }

        return new CrawlResult(novel.getId(), saved, chapters.size());
    }

    public int crawlFreeChapterContents(Long novelId) {
        List<Chapter> chapters = chapterRepository.findByNovelIdOrderByIdAsc(novelId);
        int crawled = 0;
        for (Chapter ch : chapters) {
            if (!ch.isFree()) {
                continue;
            }
            if (ch.isContentCrawled()) {
                continue;
            }
            if (!StringUtils.hasText(ch.getChapterUrl())) {
                continue;
            }
            Document doc = get(ch.getChapterUrl());

            // Best-effort selectors. If content not found, skip.
            Element contentEl = doc.selectFirst(".read-content, .main-text-wrap, .content-wrap, #content");
            if (contentEl == null) {
                continue;
            }
            String text = contentEl.text();
            if (!StringUtils.hasText(text) || text.length() < 20) {
                continue;
            }

            ch.setContent(text);
            ch.setContentCrawled(true);
            ch.setCrawledAt(OffsetDateTime.now());
            chapterRepository.save(ch);
            crawled++;
        }
        return crawled;
    }

    private List<Chapter> tryParseChapters(Document bookDoc, String baseUrl, Long novelId) {
        List<Chapter> chapters = new ArrayList<>();

        // Qidian catalog is often dynamic. Best-effort: try find chapter links on the page.
        Elements links = bookDoc.select("a[href*=/chapter/], a[href*=read.qidian.com/chapter]");
        for (Element a : links) {
            String href = a.attr("href");
            if (!StringUtils.hasText(href)) {
                continue;
            }
            String chapterUrl = absUrl(baseUrl, href);
            String title = a.text();
            if (!StringUtils.hasText(title)) {
                continue;
            }

            boolean free = true;
            // Heuristic: if element contains lock/subscribe hints, mark non-free.
            String cls = a.className();
            if (cls != null && cls.toLowerCase().contains("lock")) {
                free = false;
            }

            Chapter c = new Chapter();
            c.setNovelId(novelId);
            c.setTitle(title);
            c.setChapterUrl(chapterUrl);
            c.setFree(free);
            c.setContentCrawled(false);
            chapters.add(c);
        }

        // Deduplicate by URL (keep order)
        List<Chapter> dedup = new ArrayList<>();
        java.util.HashSet<String> seen = new java.util.HashSet<>();
        for (Chapter c : chapters) {
            if (seen.add(c.getChapterUrl())) {
                dedup.add(c);
            }
        }
        return dedup;
    }

    private Document get(String url) {
        rateLimiter.sleepBetweenRequests();
        try {
            Connection conn = Jsoup.connect(url)
                    .userAgent(props.getUserAgent())
                    .timeout(props.getRequestTimeoutMs())
                    .followRedirects(true)
                    .ignoreHttpErrors(true);
            Connection.Response resp = conn.execute();
            if (resp.statusCode() >= 400) {
                throw new IllegalArgumentException("HTTP " + resp.statusCode() + " for url: " + url);
            }
            return resp.parse();
        } catch (Exception e) {
            throw new IllegalArgumentException("Fetch failed for url: " + url + ", error: " + e.getMessage(), e);
        }
    }

    private static String textFirst(Document doc, String css) {
        Element el = doc.selectFirst(css);
        if (el == null) {
            return null;
        }
        String t = el.text();
        return StringUtils.hasText(t) ? t.trim() : null;
    }

    private static String absUrl(String base, String href) {
        if (!StringUtils.hasText(href)) {
            return null;
        }
        String h = href.trim();
        if (h.startsWith("http://") || h.startsWith("https://")) {
            return h;
        }
        if (h.startsWith("//")) {
            return "https:" + h;
        }
        try {
            URI baseUri = URI.create(base);
            return baseUri.resolve(h).toString();
        } catch (Exception e) {
            return h;
        }
    }

    public record CrawlResult(Long novelId, int newChaptersSaved, int chaptersFound) {

    }
}
