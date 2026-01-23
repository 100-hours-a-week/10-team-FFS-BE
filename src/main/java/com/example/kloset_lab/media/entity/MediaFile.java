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
    @Column(name = "purpose", nullable = false, columnDefinition = "varchar(10)")
    private Purpose purpose;

    @Column(name = "object_key", length = 100, nullable = false)
    private String objectKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, columnDefinition = "varchar(10)")
    private FileType fileType;

    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "varchar(10)")
    private FileStatus status = FileStatus.PENDING;

    @Builder
    private MediaFile(User user, Purpose purpose, String objectKey, FileType fileType) {
        this.user = user;
        this.purpose = purpose;
        this.objectKey = objectKey;
        this.fileType = fileType;
    }

    public void updateFileStatus() {
        this.status = FileStatus.UPLOADED;
        this.uploadedAt = LocalDateTime.now();
    }
}
