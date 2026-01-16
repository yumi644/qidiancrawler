package com.example.qidian.service;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.example.qidian.config.CrawlerProperties;
import com.example.qidian.domain.Chapter;
import com.example.qidian.domain.Novel;
import com.example.qidian.repo.ChapterRepository;
import com.example.qidian.repo.NovelRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class QidianCrawlerService {

    private static final String SITE_QIDIAN = "qidian";
    private static final String SITE_BQG = "bqg";

    private static final Pattern SITE_PREFIX_PATTERN = Pattern.compile("^([a-zA-Z0-9_-]+):(\\d+)$");

    private static final Pattern BOOK_ID_PATTERN = Pattern.compile("/book/(\\d+)");
    private static final Pattern CHAPTER_BOOK_ID_PATTERN = Pattern.compile("/chapter/(\\d+)");
    private static final Pattern INFO_BOOK_ID_PATTERN = Pattern.compile("/info/(\\d+)");

    private static final Pattern BQG_HASH_BOOK_ID_PATTERN = Pattern.compile("/book/(\\d+)");

    private static String sanitizeChineseText(String s) {
        if (!StringUtils.hasText(s)) {
            return s;
        }
        String out = s;
        // 先去掉拉丁字母/数字（含全角），避免章节名里混入广告/站点信息
        out = out.replaceAll("[A-Za-zＡ-Ｚａ-ｚ]", "");
        // 只保留：汉字 + 中日韩标点符号 + 空白+数字
        out = out.replaceAll("[^\\p{IsHan}\\p{InCJK_Symbols_And_Punctuation}\\s0-9０-９]", "");
        out = out.replaceAll("\\s+", " ").trim();
        return out;
    }

    private final NovelRepository novelRepository;
    private final ChapterRepository chapterRepository;
    private final CrawlerProperties props;
    private final RateLimiter rateLimiter;
    private final ObjectMapper objectMapper;

    public QidianCrawlerService(
            NovelRepository novelRepository,
            ChapterRepository chapterRepository,
            CrawlerProperties props
    ) {
        this.novelRepository = novelRepository;
        this.chapterRepository = chapterRepository;
        this.props = props;
        this.rateLimiter = new RateLimiter(props.getMinDelayMs(), props.getMaxDelayMs());
        this.objectMapper = new ObjectMapper();
    }

    public String normalizeBookId(String bookId, String url) {
        if (StringUtils.hasText(bookId)) {
            String b = bookId.trim();
            Matcher pref = SITE_PREFIX_PATTERN.matcher(b);
            if (pref.find()) {
                return pref.group(1).toLowerCase() + ":" + pref.group(2);
            }
            if (!StringUtils.hasText(url)) {
                return SITE_QIDIAN + ":" + b;
            }
            String site = detectSite(url);
            return site + ":" + b;
        }
        if (!StringUtils.hasText(url)) {
            throw new IllegalArgumentException("bookId or url is required");
        }
        String u = url.trim();

        String site = detectSite(u);
        if (SITE_BQG.equals(site)) {
            String id = parseBqgBookIdFromUrl(u);
            if (!StringUtils.hasText(id)) {
                throw new IllegalArgumentException("Cannot parse bookId from url: " + u);
            }
            return SITE_BQG + ":" + id;
        }

        String path = u;
        try {
            URI uri = URI.create(u);
            if (uri.getPath() != null) {
                path = uri.getPath();
            }
        } catch (Exception ignored) {
        }

        Matcher m = BOOK_ID_PATTERN.matcher(path);
        if (m.find()) {
            return SITE_QIDIAN + ":" + m.group(1);
        }
        m = CHAPTER_BOOK_ID_PATTERN.matcher(path);
        if (m.find()) {
            return SITE_QIDIAN + ":" + m.group(1);
        }
        m = INFO_BOOK_ID_PATTERN.matcher(path);
        if (m.find()) {
            return SITE_QIDIAN + ":" + m.group(1);
        }

        throw new IllegalArgumentException("Cannot parse bookId from url: " + u);
    }

    private static String detectSite(String url) {
        String u = url == null ? "" : url.trim().toLowerCase();
        if (u.contains("qidian.com")) {
            return SITE_QIDIAN;
        }
        if (u.contains("apibi.cc") || u.contains("bqg") || u.contains("biqu") || u.contains("#")) {
            return SITE_BQG;
        }
        return SITE_QIDIAN;
    }

    private static String parseBqgBookIdFromUrl(String url) {
        if (!StringUtils.hasText(url)) {
            return null;
        }
        try {
            URI uri = URI.create(url.trim());
            String frag = uri.getFragment();
            if (StringUtils.hasText(frag)) {
                Matcher m = BQG_HASH_BOOK_ID_PATTERN.matcher(frag);
                if (m.find()) {
                    return m.group(1);
                }
            }
            String q = uri.getQuery();
            if (StringUtils.hasText(q)) {
                for (String part : q.split("&")) {
                    String[] kv = part.split("=", 2);
                    if (kv.length == 2 && "id".equalsIgnoreCase(kv[0]) && StringUtils.hasText(kv[1])) {
                        return kv[1].trim();
                    }
                }
            }
        } catch (Exception ignored) {
        }
        Matcher m = Pattern.compile("book/(\\d+)").matcher(url);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }

    private static String stripSitePrefix(String bookKey) {
        if (!StringUtils.hasText(bookKey)) {
            return bookKey;
        }
        Matcher m = SITE_PREFIX_PATTERN.matcher(bookKey.trim());
        if (m.find()) {
            return m.group(2);
        }
        return bookKey.trim();
    }

    private static String siteOf(String bookKey) {
        if (!StringUtils.hasText(bookKey)) {
            return SITE_QIDIAN;
        }
        Matcher m = SITE_PREFIX_PATTERN.matcher(bookKey.trim());
        if (m.find()) {
            return m.group(1).toLowerCase();
        }
        return SITE_QIDIAN;
    }

    public CrawlResult crawlNovelAndCatalog(String bookKey) {
        String site = siteOf(bookKey);
        if (SITE_BQG.equals(site)) {
            return crawlBqgNovelAndCatalog(bookKey);
        }

        String qidianBookId = stripSitePrefix(bookKey);
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

        Novel novel = novelRepository.findByQidianBookId(bookKey)
                .orElseGet(Novel::new);
        novel.setQidianBookId(bookKey);
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
            String chapterUrl = ch.getChapterUrl();
            String text;
            if (chapterUrl.startsWith("https://apibi.cc/api/chapter")) {
                text = parseBqgChapterText(chapterUrl);
            } else {
                Document doc = get(chapterUrl);

                // Best-effort selectors. If content not found, skip.
                Element contentEl = doc.selectFirst(".read-content, .main-text-wrap, .content-wrap, #content");
                if (contentEl == null) {
                    continue;
                }
                text = contentEl.text();
            }
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

    public Chapter crawlChapterContent(Long chapterId) {
        Chapter ch = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new IllegalArgumentException("chapter not found"));
        if (ch.isContentCrawled() && StringUtils.hasText(ch.getContent())) {
            return ch;
        }
        if (!StringUtils.hasText(ch.getChapterUrl())) {
            throw new IllegalArgumentException("chapter url is empty");
        }
        String chapterUrl = ch.getChapterUrl();

        String text;
        if (chapterUrl.startsWith("https://apibi.cc/api/chapter")) {
            text = parseBqgChapterText(chapterUrl);
        } else {
            Document doc = get(chapterUrl);
            Element contentEl = doc.selectFirst(".read-content, .main-text-wrap, .content-wrap, #content");
            if (contentEl == null) {
                throw new IllegalArgumentException("Failed to parse chapter content");
            }
            text = contentEl.text();
        }

        if (!StringUtils.hasText(text) || text.length() < 20) {
            throw new IllegalArgumentException("chapter content is empty");
        }

        ch.setContent(text);
        ch.setContentCrawled(true);
        ch.setCrawledAt(OffsetDateTime.now());
        return chapterRepository.save(ch);
    }

    public BatchCrawlResult crawlAllChapterContent(Long novelId) {
        List<Chapter> chapters = chapterRepository.findByNovelIdOrderByIdAsc(novelId);
        int total = chapters.size();
        int success = 0;
        int skipped = 0;
        int failed = 0;

        for (Chapter ch : chapters) {
            if (ch.isContentCrawled() && StringUtils.hasText(ch.getContent())) {
                skipped++;
                continue;
            }
            try {
                crawlChapterContent(ch.getId());
                success++;
            } catch (Exception e) {
                failed++;
            }

            // 
            try {
                Thread.sleep(200);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        return new BatchCrawlResult(total, success, skipped, failed);
    }

    public static class BatchCrawlResult {

        private final int total;
        private final int success;
        private final int skipped;
        private final int failed;

        public BatchCrawlResult(int total, int success, int skipped, int failed) {
            this.total = total;
            this.success = success;
            this.skipped = skipped;
            this.failed = failed;
        }

        public int getTotal() {
            return total;
        }

        public int getSuccess() {
            return success;
        }

        public int getSkipped() {
            return skipped;
        }

        public int getFailed() {
            return failed;
        }
    }

    private CrawlResult crawlBqgNovelAndCatalog(String bookKey) {
        String id = stripSitePrefix(bookKey);

        String listJson = getText("https://apibi.cc/api/booklist?id=" + id);
        List<BqgChapterMeta> metas = parseBqgCatalog(listJson);
        if (metas.isEmpty()) {
            throw new IllegalArgumentException("bqg: empty catalog for id=" + id);
        }

        BqgChapterMeta first = metas.get(0);
        BqgChapterPayload firstPayload = fetchBqgChapter(id, first.chapterId());

        Novel novel = novelRepository.findByQidianBookId(bookKey)
                .orElseGet(Novel::new);
        novel.setQidianBookId(bookKey);
        String rawTitle = StringUtils.hasText(firstPayload.title()) ? firstPayload.title() : ("bqg-" + id);
        String title = sanitizeChineseText(rawTitle);
        novel.setTitle(StringUtils.hasText(title) ? title : rawTitle);
        novel.setAuthor(firstPayload.author());
        novel.setIntro(null);
        novel.setCoverUrl(null);
        novel.setBookUrl("https://www.bqg7500.xyz/#/book/" + id + "/1.html");
        novel.setLastCrawledAt(OffsetDateTime.now());
        novel = novelRepository.save(novel);

        int saved = 0;
        for (BqgChapterMeta meta : metas) {
            Chapter c = new Chapter();
            c.setNovelId(novel.getId());
            String rawChapterTitle = meta.name();
            String chapterTitle = sanitizeChineseText(rawChapterTitle);
            c.setTitle(StringUtils.hasText(chapterTitle) ? chapterTitle : rawChapterTitle);
            c.setChapterUrl("https://apibi.cc/api/chapter?id=" + id + "&chapterid=" + meta.chapterId());
            c.setFree(true);
            c.setContentCrawled(false);

            Optional<Chapter> existed = chapterRepository.findFirstByNovelIdAndChapterUrl(novel.getId(), c.getChapterUrl());
            if (existed.isEmpty()) {
                chapterRepository.save(c);
                saved++;
            }
        }

        return new CrawlResult(novel.getId(), saved, metas.size());
    }

    private static int chineseNumberToInt(String s) {
        if (!StringUtils.hasText(s)) {
            return -1;
        }

        s = s.trim()
                .replace("〇", "零")
                .replace("两", "二");

        Map<Character, Integer> d = Map.of(
                '零', 0, '一', 1, '二', 2, '三', 3, '四', 4,
                '五', 5, '六', 6, '七', 7, '八', 8, '九', 9
        );

        int result = 0;
        int current = 0;

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            Integer digit = d.get(c);
            if (digit != null) {
                current = digit;
                continue;
            }
            if (c == '十') {
                result += (current == 0 ? 1 : current) * 10;
                current = 0;
                continue;
            }
            if (c == '百') {
                result += (current == 0 ? 1 : current) * 100;
                current = 0;
                continue;
            }
            if (c == '千') {
                result += (current == 0 ? 1 : current) * 1000;
                current = 0;
                continue;
            }
            // 其他字符不认识就返回 -1 表示失败
            return -1;
        }
        result += current;

        return result;
    }

    private List<BqgChapterMeta> parseBqgCatalog(String json) {
        List<BqgChapterMeta> out = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode list = root.get("list");
            if (list == null || !list.isArray()) {
                return out;
            }
            for (JsonNode n : list) {
                if (n == null || n.isNull()) {
                    continue;
                }
                String s = n.asText(null);
                if (!StringUtils.hasText(s)) {
                    continue;
                }
                String name = s.trim();
                Matcher m = Pattern.compile("^第(\\d+|[一二三四五六七八九十百千万零〇两]+)章\\s*(.*)$").matcher(name);
                if (!m.find()) {
                    continue;
                }
                String no = m.group(1); // 可能是 "12" 或 "一"
                int chapterId;
                if (no.matches("\\d+")) {
                    chapterId = Integer.parseInt(no);
                } else {
                    chapterId = chineseNumberToInt(no);
                }
                if (chapterId <= 0) {
                    continue;
                }
                String tail = m.group(2);

                String display = StringUtils.hasText(tail) ? ("第" + chapterId + "章 " + tail.trim()) : ("第" + chapterId + "章");
                out.add(new BqgChapterMeta(chapterId, display));
            }
        } catch (Exception ignored) {
            return out;
        }
        return out;
    }

    private String parseBqgChapterText(String apiUrl) {
        BqgChapterPayload payload;
        try {
            payload = parseBqgChapterPayload(getText(apiUrl));
        } catch (Exception e) {
            return null;
        }
        return payload.txt();
    }

    private BqgChapterPayload fetchBqgChapter(String id, int chapterId) {
        String apiUrl = "https://apibi.cc/api/chapter?id=" + id + "&chapterid=" + chapterId;
        return parseBqgChapterPayload(getText(apiUrl));
    }

    private BqgChapterPayload parseBqgChapterPayload(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            String title = textFromNode(root, "title");
            String author = textFromNode(root, "author");
            String chapterName = textFromNode(root, "chaptername");
            String txt = textFromNode(root, "txt");
            if (StringUtils.hasText(txt)) {
                txt = txt.replace("\\r\\n", "\\n");
            }
            return new BqgChapterPayload(title, author, chapterName, txt);
        } catch (Exception e) {
            throw new IllegalArgumentException("bqg: invalid chapter payload");
        }
    }

    private record BqgChapterMeta(int chapterId, String name) {

    }

    private record BqgChapterPayload(String title, String author, String chapterName, String txt) {

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

        if (chapters.isEmpty()) {
            String bookId = normalizeBookId(null, baseUrl);
            chapters = tryParseChaptersFromCategoryApi(bookId, novelId);
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

    private List<Chapter> tryParseChaptersFromCategoryApi(String qidianBookId, Long novelId) {
        List<Chapter> chapters = new ArrayList<>();
        String apiUrl = "https://www.qidian.com/majax/book/category?bookId=" + qidianBookId;
        try {
            String json = getText(apiUrl);
            JsonNode root = objectMapper.readTree(json);
            JsonNode data = root.path("data");
            JsonNode vs = data.path("vs");
            if (!vs.isArray()) {
                return chapters;
            }
            for (JsonNode v : vs) {
                JsonNode cs = v.path("cs");
                if (!cs.isArray()) {
                    continue;
                }
                for (JsonNode c : cs) {
                    String title = textFromNode(c, "cN");
                    String chapterId = textFromNode(c, "cU");
                    if (!StringUtils.hasText(title) || !StringUtils.hasText(chapterId)) {
                        continue;
                    }

                    boolean free = true;
                    JsonNode ss = c.get("sS");
                    if (ss != null && ss.isInt()) {
                        free = ss.asInt() == 0;
                    }

                    Chapter ch = new Chapter();
                    ch.setNovelId(novelId);
                    ch.setTitle(title);
                    ch.setChapterUrl("https://www.qidian.com/chapter/" + qidianBookId + "/" + chapterId + "/");
                    ch.setFree(free);
                    ch.setContentCrawled(false);
                    chapters.add(ch);
                }
            }
        } catch (Exception ignored) {
            return chapters;
        }
        return chapters;
    }

    private static String textFromNode(JsonNode node, String field) {
        JsonNode v = node.get(field);
        if (v == null || v.isNull()) {
            return null;
        }
        String s = v.asText(null);
        return StringUtils.hasText(s) ? s.trim() : null;
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

    private String getText(String url) {
        rateLimiter.sleepBetweenRequests();
        try {
            Connection conn = Jsoup.connect(url)
                    .userAgent(props.getUserAgent())
                    .timeout(props.getRequestTimeoutMs())
                    .followRedirects(true)
                    .ignoreHttpErrors(true)
                    .ignoreContentType(true);
            Connection.Response resp = conn.execute();
            if (resp.statusCode() >= 400) {
                throw new IllegalArgumentException("HTTP " + resp.statusCode() + " for url: " + url);
            }
            return resp.body();
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
