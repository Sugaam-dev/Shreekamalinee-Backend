package com.PMRGSolution.RENAISSANCE.features.catalog.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;
@Data @Builder @AllArgsConstructor @NoArgsConstructor
	public class DocumentResponse {
	    private UUID id;
	    private String title;
	    private String categoryName;
	    private String documentType; 
	    private String requiredTier;
	    private Integer displayOrder;
	    private boolean locked;
	    private LocalDateTime createdAt;
	    private boolean interactive; // Discriminator: true for Exam, false for PDF
	    
	    // NEW: Added Duration in Minutes
	    private Integer duration;
}