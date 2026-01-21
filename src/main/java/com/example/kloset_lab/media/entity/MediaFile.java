package com.example.kloset_lab.media.entity;

import com.example.kloset_lab.global.entity.BaseTimeEntity;
import com.example.kloset_lab.user.entity.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "media_file")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MediaFile extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", length = 10, nullable = false)
    private Purpose purpose;

    @Column(name = "object_key", length = 50, nullable = false)
    private String objectKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 10, nullable = false)
    private FileType fileType;

    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 10, nullable = false)
    private FileStatus status = FileStatus.PENDING;

    @Builder
    private MediaFile(
            User user,
            Purpose purpose,
            String objectKey,
            FileType fileType,
            FileStatus status,
            LocalDateTime uploadedAt) {
        this.user = user;
        this.purpose = purpose;
        this.objectKey = objectKey;
        this.fileType = fileType;
        this.status = status != null ? status : FileStatus.PENDING;
        this.uploadedAt = uploadedAt;
    }
}
