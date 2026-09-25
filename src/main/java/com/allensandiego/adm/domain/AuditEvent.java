package com.allensandiego.adm.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "audit_event")
@Getter
@Setter
@NoArgsConstructor
public class AuditEvent {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false)
    private UUID id;

    @Column(name = "actor_username", length = 100)
    private String actorUsername;

    @Column(name = "action", nullable = false, length = 100)
    private String action;

    @Column(name = "target_type", nullable = false, length = 50)
    private String targetType;

    @Column(name = "target_id")
    private UUID targetId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "before", columnDefinition = "json")
    private String before;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "after", columnDefinition = "json")
    private String after;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private LocalDateTime occurredAt = LocalDateTime.now();
}
