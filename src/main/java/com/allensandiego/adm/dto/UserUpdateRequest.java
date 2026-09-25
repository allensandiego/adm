package com.allensandiego.adm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UserUpdateRequest {

    @NotBlank(message = "Display name is required")
    @Size(max = 120)
    private String displayName;

    public UserUpdateRequest() {}

    public UserUpdateRequest(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
}
