package com.example.kloset_lab.media.service;

import com.example.kloset_lab.global.exception.CustomException;
import com.example.kloset_lab.global.exception.ErrorCode;
import com.example.kloset_lab.media.dto.FileUploadInfo;
import com.example.kloset_lab.media.dto.FileUploadResponse;
import com.example.kloset_lab.media.dto.PresignedUrlInfo;
import com.example.kloset_lab.media.entity.FileStatus;
import com.example.kloset_lab.media.entity.FileType;
import com.example.kloset_lab.media.entity.MediaFile;
import com.example.kloset_lab.media.entity.Purpose;
import com.example.kloset_lab.media.repository.MediaFileRepository;
import com.example.kloset_lab.user.entity.User;
import com.example.kloset_lab.user.repository.UserRepository;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MediaService {
    private final UserRepository userRepository;
    private final MediaFileRepository mediaFileRepository;
    private final S3StorageService s3StorageService;

    @Transactional
    public List<FileUploadResponse> requestFileUpload(
            Long currentUserId, Purpose purpose, List<FileUploadInfo> fileUploadInfoList) {
        User user =
                userRepository.findById(currentUserId).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        List<FileUploadResponse> fileUploadResponseList = new ArrayList<>();

        validateFileCount(purpose, fileUploadInfoList.size());

        for (FileUploadInfo f : fileUploadInfoList) {
            FileType fileType = FileType.fromMimeType(f.getType())
                    .orElseThrow(() -> new CustomException(ErrorCode.UNSUPPORTED_FILE_TYPE));

            PresignedUrlInfo presignedUrlInfo = s3StorageService.generatePresignedUrl(f.getName(), fileType);

            Long fileId = mediaFileRepository
                    .save(MediaFile.builder()
                            .user(user)
                            .purpose(purpose)
                            .objectKey(presignedUrlInfo.getObjectKey())
                            .fileType(fileType)
                            .build())
                    .getId();

            fileUploadResponseList.add(FileUploadResponse.builder()
                    .fileId(fileId)
                    .objectKey(presignedUrlInfo.getObjectKey())
                    .presignedUrl(presignedUrlInfo.getPresignedUrl())
                    .build());
        }
        return fileUploadResponseList;
    }

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
        mediaFileList.forEach(file -> s3StorageService.validateUpload(file.getObjectKey(), file.getFileType()));
        mediaFileList.forEach(MediaFile::updateFileStatus);
        mediaFileRepository.saveAll(mediaFileList);
    }

    public List<String> getFileFullUrl(List<Long> fileIdList) {
        List<MediaFile> mediaFiles = mediaFileRepository.findAllById(fileIdList);

        if (mediaFiles.size() != fileIdList.size()) {
            throw new CustomException(ErrorCode.FILE_NOT_FOUND);
        }

        for (MediaFile file : mediaFiles) {
            if (!file.getStatus().equals(FileStatus.UPLOADED)) {
                throw new CustomException(ErrorCode.FILE_NOT_FOUND);
            }
        }

        return mediaFiles.stream()
                .map(MediaFile::getObjectKey)
                .map(s3StorageService::getFullImageUrl)
                .toList();
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
