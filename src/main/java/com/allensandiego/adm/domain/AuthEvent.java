package com.allensandiego.adm.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "auth_event")
@Getter
@Setter
@NoArgsConstructor
public class AuthEvent {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false)
    private UUID id;

    @Column(name = "username", nullable = false, length = 64)
    private String username;

    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", nullable = false, length = 10)
    private AuthOutcome outcome;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public AuthEvent(String username, AuthOutcome outcome, String ipAddress, String userAgent) {
        this.username = username;
        this.outcome = outcome;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
    }
}
