package com.example.kloset_lab.user.entity;

import com.example.kloset_lab.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "user")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("deleted_at IS NULL")
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 10)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 10)
    private Provider provider;

    @Column(name = "provider_id", nullable = false)
    private String providerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private UserStatus status;

    @Builder
    public User(Provider provider, String providerId) {
        this.role = UserRole.ROLE_USER;
        this.provider = provider;
        this.providerId = providerId;
        this.status = UserStatus.PENDING;
    }

    public void completeRegistration() {
        this.status = UserStatus.COMPLETE;
    }

    public boolean isRegistrationComplete() {
        return this.status == UserStatus.COMPLETE;
    }
}
