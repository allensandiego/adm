package com.allensandiego.adm.dto;

import java.util.UUID;

public class RoleResponse {

    private UUID id;
    private String name;
    private String description;
    private boolean isProtected;
    private Long version;
    private String createdAt;
    private String updatedAt;

    public RoleResponse() {}

    public RoleResponse(UUID id, String name, String description, boolean isProtected,
                        Long version, String createdAt, String updatedAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.isProtected = isProtected;
        this.version = version;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public boolean isProtected() { return isProtected; }
    public void setProtected(boolean protectedFlag) { isProtected = protectedFlag; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
