package com.example.videostreaming.repository;

import com.example.videostreaming.model.VideoMetadata;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VideoRepository extends JpaRepository<VideoMetadata, Long> {} 