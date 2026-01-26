package com.example.kloset_lab.media.entity;

import lombok.Getter;

@Getter
public enum Purpose {
    FEED(5),
    PROFILE(1),
    CLOTHES_TEMP(10),
    CLOTHES(1);

    private final int maxCount;

    Purpose(int i) {
        this.maxCount = i;
    }
}
