package com.example.kloset_lab.media.service;

import com.example.kloset_lab.media.dto.PresignedUrlInfo;
import com.example.kloset_lab.media.entity.FileType;

public interface StorageService extends FileUploadValidator {
    PresignedUrlInfo generatePresignedUrl(String fileName, FileType fileType);

    String getFullImageUrl(String objectKey);
}
