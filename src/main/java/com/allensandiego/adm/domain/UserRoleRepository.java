package com.allensandiego.adm.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface UserRoleRepository extends JpaRepository<UserRole, UserRoleCompositeId> {

    List<UserRole> findByUserId(UUID userId);

    List<UserRole> findByRoleId(UUID roleId);

    void deleteByUserId(UUID userId);

    boolean existsByUserIdAndRoleId(UUID userId, UUID roleId);

    @Query("SELECT ur.role.id FROM UserRole ur WHERE ur.user.id = :userId")
    List<UUID> findRoleIdsByUserId(@Param("userId") UUID userId);

    @Query("SELECT COUNT(DISTINCT ur.user) FROM UserRole ur JOIN ur.user u WHERE ur.role.isProtected = true AND u.status = 'ACTIVE'")
    long countDistinctUsersWithProtectedRole();

    @Query("SELECT ur FROM UserRole ur JOIN FETCH ur.role r JOIN FETCH ur.user u " +
           "WHERE ur.user.id = :userId")
    List<UserRole> findByUserIdWithRoles(@Param("userId") UUID userId);
}
