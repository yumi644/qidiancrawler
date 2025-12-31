package com.example.qidian.repo;

import com.example.qidian.domain.CrawlJob;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CrawlJobRepository extends JpaRepository<CrawlJob, Long> {
}
