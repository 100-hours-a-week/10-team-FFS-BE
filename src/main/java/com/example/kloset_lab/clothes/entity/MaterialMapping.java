package com.example.kloset_lab.clothes.entity;

import com.example.kloset_lab.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "material_mapping")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MaterialMapping extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clothes_id", nullable = false)
    private Clothes clothes;

    @Column(name = "name", nullable = false, length = 15)
    private String name;

    @Builder
    public MaterialMapping(Clothes clothes, String name) {
        this.clothes = clothes;
        this.name = name;
    }
}
