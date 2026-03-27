package com.PMRGSolution.RENAISSANCE.Constant;

public enum ResultStatus {
    IN_PROGRESS,      // Test has started, timer is running
    SUBMITTED,        // User manually clicked the "Submit" button
    AUTO_SUBMITTED,   // 🆕 System forced submission because time ran out
    UNDER_EVALUATION, // Part A done, Part B (Sketch) waiting for Admin
    COMPLETED,        // Everything graded
    ABANDONED         // User closed tab/logged out without much progress
}