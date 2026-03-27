package com.PMRGSolution.RENAISSANCE.Constant;

import lombok.Getter;

@Getter
public enum TierType {
    UNIVERSAL_FREE(0), 
    STARTER(1), 
    STANDARD(2), 
    PROFESSIONAL(3);

    private final int rank;
    TierType(int rank) { this.rank = rank; }
}