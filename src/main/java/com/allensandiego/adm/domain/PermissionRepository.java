package com.allensandiego.adm.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PermissionRepository extends JpaRepository<Permission, UUID> {

    Optional<Permission> findByCode(String code);

    boolean existsByCode(String code);

    List<Permission> findByActiveTrue();

    @Query("SELECT p FROM Permission p WHERE p.active = true")
    List<Permission> findAllActive();

    @Query("SELECT p FROM Permission p WHERE LOWER(p.code) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(p.label) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Permission> search(@Param("search") String search, Pageable pageable);
}
