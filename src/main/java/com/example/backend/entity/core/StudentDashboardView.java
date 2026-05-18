package com.example.backend.entity.core;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(
        name = "student_dashboard_view",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_student_dashboard_view",
                        columnNames = {"student_id", "item_type", "item_id"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentDashboardView {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "view_id")
    private Long viewId;

    @Column(name = "student_id", nullable = false)
    private String studentId;

    @Column(name = "item_type", nullable = false)
    private String itemType;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(name = "viewed_at", nullable = false)
    private OffsetDateTime viewedAt;

    @PrePersist
    public void prePersist() {
        if (viewedAt == null) {
            viewedAt = OffsetDateTime.now();
        }
    }
}