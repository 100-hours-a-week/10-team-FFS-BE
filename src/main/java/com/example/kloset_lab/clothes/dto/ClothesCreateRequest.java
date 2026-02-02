package com.example.kloset_lab.clothes.dto;

import com.example.kloset_lab.clothes.entity.Category;
import java.time.LocalDate;
import java.util.List;
import lombok.Builder;

@Builder
public record ClothesCreateRequest(
        String taskId,
        Long fileId,
        String name,
        String brand,
        Integer price,
        String size,
        LocalDate boughtDate,
        Category category,
        List<String> material,
        List<String> color,
        List<String> styleTag) {}
