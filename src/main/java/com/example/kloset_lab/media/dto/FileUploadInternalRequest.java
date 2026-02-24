package com.example.kloset_lab.media.dto;

import com.example.kloset_lab.media.entity.Purpose;
import java.util.List;

public record FileUploadInternalRequest(Long userId, Purpose purpose, List<FileUploadInfo> files) {}
