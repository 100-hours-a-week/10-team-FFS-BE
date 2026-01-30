package com.example.kloset_lab.ai.entity;

import com.example.kloset_lab.clothes.entity.Clothes;
import com.example.kloset_lab.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tpo_result_clothes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TpoResultClothes extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tpo_result_id", nullable = false)
    private TpoResult tpoResult;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clothes_id", nullable = false)
    private Clothes clothes;

    @Builder
    public TpoResultClothes(TpoResult tpoResult, Clothes clothes) {
        this.tpoResult = tpoResult;
        this.clothes = clothes;
    }
}
