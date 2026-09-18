package com.luggagestorage.store.entity;

public enum StoreCategory {
    LIGHT(3_000),
    MEDIUM(5_000),
    CLOTHES(4_000),
    OTHER(4_000);

    private final int dailyRate;

    StoreCategory(int dailyRate) {
        this.dailyRate = dailyRate;
    }

    public int getDailyRate() {
        return dailyRate;
    }
}
