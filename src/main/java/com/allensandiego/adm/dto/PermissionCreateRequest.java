package com.allensandiego.adm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class PermissionCreateRequest {

    @NotBlank(message = "Permission code is required")
    @Size(min = 2, max = 80)
    private String code;

    @NotBlank(message = "Label is required")
    @Size(max = 120)
    private String label;

    public PermissionCreateRequest() {}

    public PermissionCreateRequest(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
}
