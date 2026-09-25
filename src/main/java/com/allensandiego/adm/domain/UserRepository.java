package com.allensandiego.adm.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsername(String username);

    Optional<User> findByUsernameIgnoreCase(String username);

    boolean existsByUsername(String username);

    @Query("SELECT u FROM User u WHERE LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(u.displayName) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<User> search(@Param("search") String search, Pageable pageable);
}
