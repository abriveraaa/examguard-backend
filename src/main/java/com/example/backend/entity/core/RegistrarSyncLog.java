package com.example.backend.entity.core;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "registrar_sync_log")
public class RegistrarSyncLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Getter
    @Setter
    @Column(name="sync_type")
    private String syncType; // STUDENT, FACULTY, ALL

    @Getter
    @Setter
    @Column(name="status")
    private String status;   // STARTED, SUCCESS, FAILED

    @Getter
    @Setter
    @Column(name="started_at")
    private OffsetDateTime startedAt;

    @Getter
    @Setter
    @Column(name="finished_at")
    private OffsetDateTime finishedAt;

    @Getter
    @Setter
    @Column(name="performed_by")
    private String performedBy;

    @Getter
    @Setter
    @Column(name="message")
    private String message;

    @Getter
    @Setter
    @Column(name="records_affected")
    private Integer recordsAffected;

}
