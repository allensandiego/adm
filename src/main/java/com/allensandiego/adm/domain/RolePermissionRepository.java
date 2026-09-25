package com.allensandiego.adm.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface RolePermissionRepository extends JpaRepository<RolePermission, RolePermissionId> {

    List<RolePermission> findByRoleId(UUID roleId);

    List<RolePermission> findByPermissionId(UUID permissionId);

    void deleteByRoleId(UUID roleId);

    void deleteByPermissionId(UUID permissionId);

    boolean existsByRoleIdAndPermissionId(UUID roleId, UUID permissionId);

    void deleteByRoleIdAndPermissionId(UUID roleId, UUID permissionId);

    @Modifying
    @Query("DELETE FROM RolePermission rp WHERE rp.role.id = :roleId")
    void deleteByRoleIdPure(@Param("roleId") UUID roleId);

    @Query("SELECT rp.permission.code FROM RolePermission rp " +
           "JOIN rp.role r JOIN rp.permission p " +
           "WHERE r.id = :roleId AND p.active = true")
    List<String> findActivePermissionCodesByRoleId(@Param("roleId") UUID roleId);
}
