package com.allensandiego.adm.dto;

import java.util.UUID;

public class UserResponse {

    private UUID id;
    private String username;
    private String displayName;
    private String status;
    private Long version;
    private String createdAt;
    private String updatedAt;

    public UserResponse() {}

    public UserResponse(UUID id, String username, String displayName, String status,
                        Long version, String createdAt, String updatedAt) {
        this.id = id;
        this.username = username;
        this.displayName = displayName;
        this.status = status;
        this.version = version;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
