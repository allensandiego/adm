package com.allensandiego.adm.repository;

import com.allensandiego.adm.domain.AuthEvent;
import com.allensandiego.adm.domain.AuthOutcome;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface AuthEventRepository extends JpaRepository<AuthEvent, UUID> {

    @Query("SELECT COUNT(a) FROM AuthEvent a WHERE a.username = :username AND a.outcome = :outcome AND a.createdAt >= :since")
    int countByUsernameAndOutcomeAfter(@Param("username") String username,
                                       @Param("outcome") AuthOutcome outcome,
                                       @Param("since") LocalDateTime since);
}
