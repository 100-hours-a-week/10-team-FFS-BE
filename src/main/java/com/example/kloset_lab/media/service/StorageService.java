package com.example.kloset_lab.media.service;

import com.example.kloset_lab.media.dto.PresignedUrlInfo;
import com.example.kloset_lab.media.entity.FileType;

public interface StorageService {
    PresignedUrlInfo generatePresignedUrl(String fileName, FileType fileType);

    void validateUpload(String objectKey, FileType expectedFileType);

    String getFullImageUrl(String objectKey);
}
