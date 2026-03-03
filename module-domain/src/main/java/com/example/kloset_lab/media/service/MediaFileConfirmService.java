package com.example.kloset_lab.media.service;

import com.example.kloset_lab.global.exception.CustomException;
import com.example.kloset_lab.global.exception.ErrorCode;
import com.example.kloset_lab.media.entity.FileStatus;
import com.example.kloset_lab.media.entity.MediaFile;
import com.example.kloset_lab.media.entity.Purpose;
import com.example.kloset_lab.media.repository.MediaFileRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 미디어 파일 업로드 확인 서비스
 *
 * <p>소유자·purpose·S3 존재 확인 후 PENDING → UPLOADED 상태 전환을 담당한다.
 * module-domain에 배치되어 API 서버와 채팅 서버 모두 사용할 수 있다.</p>
 */
@Service
@RequiredArgsConstructor
public class MediaFileConfirmService {

    private final MediaFileRepository mediaFileRepository;
    private final FileUploadValidator fileUploadValidator;

    /**
     * 파일 업로드 확인 처리
     *
     * @param currentUserId 요청 사용자 ID
     * @param purpose       파일 목적 (CHAT, PROFILE 등)
     * @param fileIdList    확인할 파일 ID 목록
     */
    @Transactional
    public void confirmFileUpload(Long currentUserId, Purpose purpose, List<Long> fileIdList) {
        validateFileCount(purpose, fileIdList.size());

        List<MediaFile> mediaFileList = mediaFileRepository.findAllById(fileIdList);

        if (mediaFileList.size() != fileIdList.size()) {
            throw new CustomException(ErrorCode.FILE_NOT_FOUND);
        }

        for (MediaFile mediaFile : mediaFileList) {
            if (!mediaFile.getUser().getId().equals(currentUserId)) {
                throw new CustomException(ErrorCode.FILE_ACCESS_DENIED);
            }
            if (!mediaFile.getPurpose().equals(purpose)) {
                throw new CustomException(ErrorCode.UPLOADED_FILE_MISMATCH);
            }
            if (!mediaFile.getStatus().equals(FileStatus.PENDING)) {
                throw new CustomException(ErrorCode.NOT_PENDING_STATE);
            }
        }
        mediaFileList.forEach(file -> fileUploadValidator.validateUpload(file.getObjectKey(), file.getFileType()));
        mediaFileList.forEach(MediaFile::updateFileStatus);
    }

    private void validateFileCount(Purpose purpose, int count) {
        if (count < 1) {
            throw new CustomException(ErrorCode.TOO_FEW_FILES);
        }
        if (count > purpose.getMaxCount()) {
            throw new CustomException(ErrorCode.TOO_MANY_FILES);
        }
    }
}
