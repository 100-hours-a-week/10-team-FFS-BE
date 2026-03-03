package com.example.kloset_lab.feed.entity;

import com.example.kloset_lab.clothes.entity.Clothes;
import com.example.kloset_lab.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "feed_clothes_mapping")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FeedClothesMapping extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feed_id", nullable = false)
    private Feed feed;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clothes_id", nullable = false)
    private Clothes clothes;

    @Builder
    public FeedClothesMapping(Feed feed, Clothes clothes) {
        this.feed = feed;
        this.clothes = clothes;
    }
}
