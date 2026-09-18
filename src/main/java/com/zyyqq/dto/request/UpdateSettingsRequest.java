package com.zyyqq.dto.request;

import lombok.Data;

@Data
public class UpdateSettingsRequest {

    private Boolean dietReminder;
    private String reminderTime;
    private Boolean goalReminder;
    private Boolean aiSuggestion;
    private String theme;
    private String language;
    private Boolean collapsedSidebar;
    private Boolean dataSharing;
    private Boolean publicRecords;
}