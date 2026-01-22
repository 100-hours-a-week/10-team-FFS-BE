package com.example.kloset_lab.media.entity;

import java.util.Optional;
import lombok.Getter;

@Getter
public enum FileType {
    PNG("image/png", "png"),
    JPEG("image/jpeg", "jpeg");

    private final String mimeType;
    private final String extension;

    FileType(String mimeType, String extension) {
        this.mimeType = mimeType;
        this.extension = extension;
    }

    public static Optional<FileType> fromMimeType(String mimeType) {
        for (FileType type : FileType.values()) {
            if (type.mimeType.equals(mimeType)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }
}
