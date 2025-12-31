package com.example.qidian.repo;

import com.example.qidian.domain.Novel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NovelRepository extends JpaRepository<Novel, Long> {

    Optional<Novel> findByQidianBookId(String qidianBookId);
}
