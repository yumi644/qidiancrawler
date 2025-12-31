package com.example.qidian.service;

import com.example.qidian.domain.CrawlJob;
import com.example.qidian.domain.CrawlJobStatus;
import com.example.qidian.repo.CrawlJobRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
public class CrawlOrchestrator {

    private final CrawlJobRepository jobRepository;
    private final QidianCrawlerService crawlerService;

    public CrawlOrchestrator(CrawlJobRepository jobRepository, QidianCrawlerService crawlerService) {
        this.jobRepository = jobRepository;
        this.crawlerService = crawlerService;
    }

    public CrawlJob createJob(String qidianBookId) {
        OffsetDateTime now = OffsetDateTime.now();
        CrawlJob job = new CrawlJob();
        job.setQidianBookId(qidianBookId);
        job.setStatus(CrawlJobStatus.PENDING);
        job.setMessage(null);
        job.setCreatedAt(now);
        job.setUpdatedAt(now);
        return jobRepository.save(job);
    }

    @Async
    public void runJob(Long jobId) {
        CrawlJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("job not found: " + jobId));

        job.setStatus(CrawlJobStatus.RUNNING);
        job.setUpdatedAt(OffsetDateTime.now());
        jobRepository.save(job);

        try {
            QidianCrawlerService.CrawlResult result = crawlerService.crawlNovelAndCatalog(job.getQidianBookId());
            int crawled = crawlerService.crawlFreeChapterContents(result.novelId());

            job.setStatus(CrawlJobStatus.SUCCESS);
            job.setMessage("chaptersFound=" + result.chaptersFound() + ", newChaptersSaved=" + result.newChaptersSaved() + ", freeContentsCrawled=" + crawled);
            job.setUpdatedAt(OffsetDateTime.now());
            jobRepository.save(job);
        } catch (Exception e) {
            job.setStatus(CrawlJobStatus.FAILED);
            job.setMessage(e.getMessage());
            job.setUpdatedAt(OffsetDateTime.now());
            jobRepository.save(job);
        }
    }
}
