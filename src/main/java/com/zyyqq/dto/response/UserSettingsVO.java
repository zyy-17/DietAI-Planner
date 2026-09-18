package com.zyyqq.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserSettingsVO {

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