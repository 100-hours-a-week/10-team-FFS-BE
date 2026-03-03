package com.example.kloset_lab.clothes.dto;

import com.example.kloset_lab.clothes.entity.Category;
import java.time.LocalDate;
import java.util.List;

public record ClothesUpdateRequest(
        String name,
        String brand,
        Integer price,
        String size,
        LocalDate boughtDate,
        Category category,
        List<String> material,
        List<String> color) {}
