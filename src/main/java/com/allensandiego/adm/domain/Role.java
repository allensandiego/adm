package com.allensandiego.adm.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
public class Role {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false)
    private UUID id;

    @Column(name = "name", nullable = false, unique = true, length = 80)
    private String name;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "is_protected", nullable = false)
    private boolean isProtected = false;

    @Version
    @Column(name = "version")
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void setProtected(boolean protectedFlag) {
        if (this.isProtected && !protectedFlag) {
            throw new IllegalStateException("Cannot change protected flag on a protected role");
        }
        this.isProtected = protectedFlag;
    }
}
