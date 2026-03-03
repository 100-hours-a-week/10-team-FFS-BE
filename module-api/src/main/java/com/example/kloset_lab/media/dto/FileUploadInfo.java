package com.example.kloset_lab.media.dto;

import lombok.Builder;

@Builder
public record FileUploadInfo(String name, String type) {}
