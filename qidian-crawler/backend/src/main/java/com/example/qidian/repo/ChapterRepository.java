package com.example.qidian.repo;

import com.example.qidian.domain.Chapter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChapterRepository extends JpaRepository<Chapter, Long> {

    List<Chapter> findByNovelIdOrderByIdAsc(Long novelId);

    Optional<Chapter> findFirstByNovelIdAndChapterUrl(Long novelId, String chapterUrl);
}
