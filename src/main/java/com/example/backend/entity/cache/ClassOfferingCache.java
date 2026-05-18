package com.example.backend.entity.cache;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "class_offering_cache")
public class ClassOfferingCache {

    @Id
    @Column(name = "class_offering_id")
    private String classOfferingId;

    @Column(name = "course_id")
    private String courseId;

    @Column(name = "course_code")
    private String courseCode;

    @Column(name = "course_description")
    private String courseDescription;

    @Column(name = "units")
    private Integer units;

    @Column(name = "section_id")
    private String sectionId;

    @Column(name = "program_code")
    private String programCode;

    @Column(name = "year_level")
    private Integer yearLevel;

    @Column(name = "section_name")
    private String sectionName;

    @Column(name = "academic_year")
    private String academicYear;

    @Column(name = "term")
    private String term;

    @Column(name = "college_offering")
    private String collegeOffering;

    @Column(name = "status")
    private String status;

    @Column(name = "last_synced_at")
    private LocalDateTime lastSyncedAt;

    @Column(name = "source_updated_at")
    private LocalDateTime sourceUpdatedAt;

    @PrePersist
    @PreUpdate
    public void beforeSave() {
        this.lastSyncedAt = LocalDateTime.now();
    }

    public String getProgramCode() {
        if (programCode == null || yearLevel == null || sectionName == null) {
            return "-";
        }
        return programCode + " " + yearLevel + "-" + sectionName;
    }
}