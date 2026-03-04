package com.example.kloset_lab.media.repository;

import com.example.kloset_lab.media.entity.FileStatus;
import com.example.kloset_lab.media.entity.MediaFile;
import com.example.kloset_lab.media.entity.Purpose;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MediaFileRepository extends JpaRepository<MediaFile, Long> {

    List<MediaFile> findByPurposeAndStatusAndCreatedAtBefore(Purpose purpose, FileStatus status, LocalDateTime before);
}
