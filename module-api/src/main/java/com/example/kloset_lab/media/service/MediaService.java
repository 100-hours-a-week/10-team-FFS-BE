package com.example.kloset_lab.media.service;

import com.example.kloset_lab.global.exception.CustomException;
import com.example.kloset_lab.global.exception.ErrorCode;
import com.example.kloset_lab.media.dto.FileUploadInfo;
import com.example.kloset_lab.media.dto.FileUploadResponse;
import com.example.kloset_lab.media.dto.PresignedUrlInfo;
import com.example.kloset_lab.media.entity.FileType;
import com.example.kloset_lab.media.entity.MediaFile;
import com.example.kloset_lab.media.entity.Purpose;
import com.example.kloset_lab.media.repository.MediaFileRepository;
import com.example.kloset_lab.user.entity.User;
import com.example.kloset_lab.user.repository.UserRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MediaService {
    private final UserRepository userRepository;
    private final MediaFileRepository mediaFileRepository;
    private final StorageService storageService;
    private final MediaFileConfirmService mediaFileConfirmService;

    @Transactional
    public List<FileUploadResponse> requestFileUpload(
            Long userId, Purpose purpose, List<FileUploadInfo> fileUploadInfoList) {
        User user = userRepository.findById(userId).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        List<FileUploadResponse> fileUploadResponseList = new ArrayList<>();

        validateFileCount(purpose, fileUploadInfoList.size());

        for (FileUploadInfo f : fileUploadInfoList) {
            FileType fileType = FileType.fromMimeType(f.type())
                    .orElseThrow(() -> new CustomException(ErrorCode.UNSUPPORTED_FILE_TYPE));

            PresignedUrlInfo presignedUrlInfo = storageService.generatePresignedUrl(f.name(), fileType);

            Long fileId = mediaFileRepository
                    .save(MediaFile.builder()
                            .user(user)
                            .purpose(purpose)
                            .objectKey(presignedUrlInfo.objectKey())
                            .fileType(fileType)
                            .build())
                    .getId();

            fileUploadResponseList.add(FileUploadResponse.builder()
                    .fileId(fileId)
                    .objectKey(presignedUrlInfo.objectKey())
                    .presignedUrl(presignedUrlInfo.presignedUrl())
                    .build());
        }
        return fileUploadResponseList;
    }

    @Transactional
    public void confirmFileUpload(Long currentUserId, Purpose purpose, List<Long> fileIdList) {
        mediaFileConfirmService.confirmFileUpload(currentUserId, purpose, fileIdList);
    }

    public List<String> getFileFullUrls(List<Long> fileIdList) {
        List<MediaFile> mediaFiles = mediaFileRepository.findAllById(fileIdList);

        if (mediaFiles.size() != fileIdList.size()) {
            throw new CustomException(ErrorCode.FILE_NOT_FOUND);
        }
        /*
               for (MediaFile file : mediaFiles) {

                   if (!file.getStatus().equals(FileStatus.UPLOADED)) {
                       throw new CustomException(ErrorCode.FILE_NOT_FOUND);
                   }
               }

        */

        return mediaFiles.stream()
                .map(MediaFile::getObjectKey)
                .map(storageService::getFullImageUrl)
                .toList();
    }

    public String getFileFullUrl(Long fileId) {
        MediaFile mediaFile =
                mediaFileRepository.findById(fileId).orElseThrow(() -> new CustomException(ErrorCode.FILE_NOT_FOUND));

        return storageService.getFullImageUrl(mediaFile.getObjectKey());
    }

    public Map<Long, String> getFileFullUrlsMap(List<Long> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            return Map.of();
        }

        List<MediaFile> mediaFiles = mediaFileRepository.findAllById(fileIds);

        return mediaFiles.stream()
                .collect(Collectors.toMap(
                        MediaFile::getId, file -> storageService.getFullImageUrl(file.getObjectKey())));
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
