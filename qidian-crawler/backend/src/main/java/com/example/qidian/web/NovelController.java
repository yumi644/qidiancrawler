package com.example.qidian.web;

import com.example.qidian.domain.Chapter;
import com.example.qidian.domain.Novel;
import com.example.qidian.repo.ChapterRepository;
import com.example.qidian.repo.NovelRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class NovelController {

    private final NovelRepository novelRepository;
    private final ChapterRepository chapterRepository;

    public NovelController(NovelRepository novelRepository, ChapterRepository chapterRepository) {
        this.novelRepository = novelRepository;
        this.chapterRepository = chapterRepository;
    }

    @GetMapping("/novels")
    public List<Novel> listNovels() {
        return novelRepository.findAll();
    }

    @GetMapping("/novels/{id}")
    public Novel getNovel(@PathVariable Long id) {
        return novelRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("novel not found"));
    }

    @GetMapping("/novels/{id}/chapters")
    public List<Chapter> listChapters(@PathVariable Long id) {
        return chapterRepository.findByNovelIdOrderByIdAsc(id);
    }
}
