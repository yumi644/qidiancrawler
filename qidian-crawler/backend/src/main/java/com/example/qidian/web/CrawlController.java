package com.example.qidian.web;

import com.example.qidian.domain.CrawlJob;
import com.example.qidian.repo.CrawlJobRepository;
import com.example.qidian.service.CrawlOrchestrator;
import com.example.qidian.service.QidianCrawlerService;
import com.example.qidian.web.dto.CrawlNovelRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/crawl")
public class CrawlController {

    private final QidianCrawlerService crawlerService;
    private final CrawlOrchestrator orchestrator;
    private final CrawlJobRepository jobRepository;

    public CrawlController(QidianCrawlerService crawlerService, CrawlOrchestrator orchestrator, CrawlJobRepository jobRepository) {
        this.crawlerService = crawlerService;
        this.orchestrator = orchestrator;
        this.jobRepository = jobRepository;
    }

    @PostMapping("/novel")
    public CrawlJob crawlNovel(@Valid @RequestBody CrawlNovelRequest req) {
        String bookId = crawlerService.normalizeBookId(req.getBookId(), req.getUrl());
        CrawlJob job = orchestrator.createJob(bookId);
        orchestrator.runJob(job.getId());
        return job;
    }

    @GetMapping("/jobs")
    public List<CrawlJob> listJobs() {
        List<CrawlJob> jobs = jobRepository.findAll();
        jobs.sort(Comparator.comparing(CrawlJob::getCreatedAt).reversed());
        return jobs;
    }

    @GetMapping("/jobs/{id}")
    public CrawlJob getJob(@PathVariable Long id) {
        return jobRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("job not found"));
    }
}
