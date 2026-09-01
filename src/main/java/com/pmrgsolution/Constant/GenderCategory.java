package com.pmrgsolution.Constant;

public enum GenderCategory {
    WOMEN("Women's Collection"),
    MEN("Men's Collection"),
    KIDS("Kids' Collection"),
    UNISEX("Unisex Heritage");

    private final String displayName;

    GenderCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
