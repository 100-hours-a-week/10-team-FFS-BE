package com.example.kloset_lab.clothes.dto;

import com.example.kloset_lab.clothes.entity.Category;
import com.example.kloset_lab.clothes.entity.Clothes;
import java.time.LocalDate;
import java.util.List;
import lombok.Builder;

@Builder
public record ClothesDetailResponse(
        Long clothesId,
        String clothesImageUrl,
        String name,
        String brand,
        Integer price,
        String size,
        LocalDate boughtDate,
        Category category,
        List<String> material,
        List<String> color,
        List<String> styleTag,
        boolean isOwner) {

    public static ClothesDetailResponse from(Clothes clothes, String imageUrl, boolean isOwner) {
        return ClothesDetailResponse.builder()
                .clothesId(clothes.getId())
                .clothesImageUrl(imageUrl)
                .name(clothes.getClothesName())
                .brand(clothes.getBrandName())
                .price(clothes.getPrice())
                .size(clothes.getSize())
                .boughtDate(clothes.getBoughtDate())
                .category(clothes.getCategory())
                .material(clothes.getMaterials())
                .color(clothes.getColors())
                .styleTag(clothes.getStyleTags())
                .isOwner(isOwner)
                .build();
    }
}
