package com.allensandiego.adm.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoleRepository extends JpaRepository<Role, UUID> {

    Optional<Role> findByName(String name);

    boolean existsByName(String name);

    long countByIsProtectedTrue();

    @Query("SELECT r FROM Role r WHERE r.isProtected = true")
    List<Role> findAllProtected();

    @Query("SELECT COUNT(DISTINCT ur.user) FROM UserRole ur JOIN ur.user u WHERE ur.role.isProtected = true AND u.status = 'ACTIVE'")
    int countUsersWithProtectedRole();

    @Query("SELECT COUNT(DISTINCT ur.user) FROM UserRole ur WHERE ur.role.isProtected = true")
    long countDistinctUsersWithProtectedRole();

    @Query("SELECT r FROM Role r WHERE LOWER(r.name) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Role> search(@Param("search") String search, Pageable pageable);
}
