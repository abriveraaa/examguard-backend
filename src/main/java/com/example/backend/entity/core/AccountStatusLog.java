package com.example.backend.entity.core;

import jakarta.persistence.*;
import jakarta.websocket.server.ServerEndpoint;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "account_status_log")
public class AccountStatusLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Getter
    @Setter
    @Column(name = "school_id", nullable = false)
    private String schoolId;

    @Getter
    @Setter
    @Column(name = "role", nullable = false)
    private String role;

    @Getter
    @Setter
    @Column(name = "action", nullable = false)
    private String action; // DEACTIVATED / REACTIVATED / BLOCKED / UNBLOCKED

    @Getter
    @Setter
    @Column(name = "reason")
    private String reason;

    @Getter
    @Setter
    @Column(name = "performed_by")
    private String performedBy;

    @Getter
    @Setter
    @Column(name = "previous_status")
    private String previousStatus;

    @Getter
    @Setter
    @Column(name = "new_status")
    private String newStatus;

    @Getter
    @Setter
    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = OffsetDateTime.now();
    }
}