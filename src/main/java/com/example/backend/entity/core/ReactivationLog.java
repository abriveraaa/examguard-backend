package com.example.backend.entity.core;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Entity
@Getter
@Setter
@Table(name = "reactivation_log")
public class ReactivationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "school_id")
    private String schoolId;

    private String role;

    @Column(columnDefinition = "TEXT")
    private String justification;

    @Column(name = "reactivated_at")
    private OffsetDateTime reactivatedAt;

    @PrePersist
    protected void onCreate() {
        reactivatedAt = OffsetDateTime.now();
    }

}