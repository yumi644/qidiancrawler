package com.example.qidian.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.qidian.domain.Chapter;

public interface ChapterRepository extends JpaRepository<Chapter, Long> {

    List<Chapter> findByNovelIdOrderByIdAsc(Long novelId);

    long deleteByNovelId(Long novelId);

    Optional<Chapter> findFirstByNovelIdAndChapterUrl(Long novelId, String chapterUrl);
}
