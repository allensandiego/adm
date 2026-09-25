package com.allensandiego.adm.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {

    List<AuditEvent> findByTargetTypeAndActorUsername(String targetType, String actorUsername);

    List<AuditEvent> findByActorUsername(String actorUsername);

    List<AuditEvent> findByTargetType(String targetType);

    List<AuditEvent> findByTargetId(UUID targetId);

    List<AuditEvent> findByAction(String action);
}
