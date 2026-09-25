package com.allensandiego.adm.security;

/**
 * Centralized permission catalog constants.
 * No magic strings — all permission codes are defined here (constitution Security Implementation Standards).
 */
public final class Permissions {

    private Permissions() {}

    // Permission management
    public static final String PERMISSION_VIEW = "permission.view";
    public static final String PERMISSION_CREATE = "permission.create";
    public static final String PERMISSION_EDIT = "permission.edit";

    // User management
    public static final String USER_VIEW = "user.view";
    public static final String USER_CREATE = "user.create";
    public static final String USER_EDIT = "user.edit";
    public static final String USER_ACTIVATE = "user.activate";
    public static final String USER_ROLES_ASSIGN = "user.roles.assign";

    // Role management
    public static final String ROLE_VIEW = "role.view";
    public static final String ROLE_CREATE = "role.create";
    public static final String ROLE_EDIT = "role.edit";
    public static final String ROLE_DELETE = "role.delete";
    public static final String ROLE_PERMISSIONS_EDIT = "role.permissions.edit";

    /**
     * Returns all permission codes used by the application.
     */
    public static String[] all() {
        return new String[]{
            PERMISSION_VIEW, PERMISSION_CREATE, PERMISSION_EDIT,
            USER_VIEW, USER_CREATE, USER_EDIT, USER_ACTIVATE, USER_ROLES_ASSIGN,
            ROLE_VIEW, ROLE_CREATE, ROLE_EDIT, ROLE_DELETE, ROLE_PERMISSIONS_EDIT
        };
    }
}
