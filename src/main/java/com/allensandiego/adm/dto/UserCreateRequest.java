package com.allensandiego.adm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UserCreateRequest {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 64)
    private String username;

    @NotBlank(message = "Display name is required")
    @Size(max = 120)
    private String displayName;

    private boolean activate = true;

    public UserCreateRequest() {}

    public UserCreateRequest(String username, String displayName, boolean activate) {
        this.username = username;
        this.displayName = displayName;
        this.activate = activate;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public boolean isActivate() { return activate; }
    public void setActivate(boolean activate) { this.activate = activate; }
}
