package com.allensandiego.adm.dto;

import java.util.UUID;

public class PermissionResponse {

    private UUID id;
    private String code;
    private String label;
    private boolean active;
    private String createdAt;
    private String updatedAt;

    public PermissionResponse() {}

    public PermissionResponse(UUID id, String code, String label, boolean active,
                              String createdAt, String updatedAt) {
        this.id = id;
        this.code = code;
        this.label = label;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
