package com.pmrgsolution.features.settings.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnnouncementSettingsResponse {
    private Boolean isAnnouncementActive;
    private String announcementText;
    private String announcementLink;
    private String announcementsJson;
}