package com.allensandiego.adm.dto;

import java.util.UUID;

public class UserRoleAssignmentRequest {

    private UUID roleId;

    public UserRoleAssignmentRequest() {}

    public UserRoleAssignmentRequest(UUID roleId) {
        this.roleId = roleId;
    }

    public UUID getRoleId() { return roleId; }
    public void setRoleId(UUID roleId) { this.roleId = roleId; }
}
