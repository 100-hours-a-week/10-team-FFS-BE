package com.example.kloset_lab.media.service;

import com.example.kloset_lab.media.entity.FileStatus;
import com.example.kloset_lab.media.entity.MediaFile;
import com.example.kloset_lab.media.entity.Purpose;
import com.example.kloset_lab.media.repository.MediaFileRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * AI 코디 생성 도중 실패로 방치된 고아 MediaFile 정리 서비스.
 *
 * <p>Presigned URL 만료 시간(10분)을 초과하여 어떤 경로로도 UPLOADED 상태로 전환될 수 없는
 * OUTFIT PENDING 레코드를 주기적으로 삭제한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MediaFileCleanupService {

    private static final int ORPHAN_THRESHOLD_HOURS = 1;

    private final MediaFileRepository mediaFileRepository;

    /**
     * 고아 OUTFIT PENDING MediaFile 배치 삭제 (매일 새벽 3시)
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupOrphanOutfitFiles() {
        LocalDateTime threshold = LocalDateTime.now().minusHours(ORPHAN_THRESHOLD_HOURS);

        List<MediaFile> orphans =
                new java.util.ArrayList<>(mediaFileRepository.findByPurposeAndStatusAndCreatedAtBefore(
                        Purpose.OUTFIT, FileStatus.PENDING, threshold));
        orphans.addAll(mediaFileRepository.findByPurposeAndStatusAndCreatedAtBefore(
                Purpose.VTON, FileStatus.PENDING, threshold));

        if (orphans.isEmpty()) {
            return;
        }

        log.info("고아 OUTFIT/VTON MediaFile 정리: {}건", orphans.size());
        mediaFileRepository.deleteAll(orphans);
    }
}
