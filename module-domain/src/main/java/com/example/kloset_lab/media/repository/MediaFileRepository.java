package com.example.kloset_lab.media.repository;

import com.example.kloset_lab.media.entity.MediaFile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MediaFileRepository extends JpaRepository<MediaFile, Long> {}
