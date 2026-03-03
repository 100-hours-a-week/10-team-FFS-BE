package com.example.kloset_lab.media.dto;

import com.example.kloset_lab.media.entity.Purpose;
import java.util.List;

public record FileUploadRequest(Purpose purpose, List<FileUploadInfo> files) {}
