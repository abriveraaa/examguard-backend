package com.example.backend.entity.cache;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "student_profile_cache")
public class StudentProfileCache {

    @Id
    @Column(name = "student_id")
    private String studentId;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "email_address")
    private String emailAddress;

    @Column(name = "birth_date")
    private String birthDate;

    @Column(name = "college_code")
    private String collegeCode;

    @Column(name = "college_name")
    private String collegeName;

    @Column(name = "program_code")
    private String programCode;

    @Column(name = "program_name")
    private String programName;

    @Column(name = "year_level")
    private Integer yearLevel;

    @Column(name = "section_name")
    private String sectionName;

    @Column(name = "scholastic_status")
    private String scholasticStatus;

    @Column(name = "last_synced_at")
    private LocalDateTime lastSyncedAt;

    @Column(name = "source_updated_at")
    private LocalDateTime sourceUpdatedAt;

    @PrePersist
    @PreUpdate
    public void beforeSave() {
        this.lastSyncedAt = LocalDateTime.now();
    }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmailAddress() { return emailAddress; }
    public void setEmailAddress(String emailAddress) { this.emailAddress = emailAddress; }

    public String getBirthDate() { return birthDate; }
    public void setBirthDate(String birthDate) { this.birthDate = birthDate; }

    public String getCollegeCode() { return collegeCode; }
    public void setCollegeCode(String collegeCode) { this.collegeCode = collegeCode; }

    public String getCollegeName() { return collegeName; }
    public void setCollegeName(String collegeName) { this.collegeName = collegeName; }

    public String getProgramCode() { return programCode; }
    public void setProgramCode(String programCode) { this.programCode = programCode; }

    public String getProgramName() { return programName; }
    public void setProgramName(String programName) { this.programName = programName; }

    public Integer getYearLevel() { return yearLevel; }
    public void setYearLevel(Integer yearLevel) { this.yearLevel = yearLevel; }

    public String getSectionName() { return sectionName; }
    public void setSectionName(String sectionName) { this.sectionName = sectionName; }

    public String getScholasticStatus() { return scholasticStatus; }
    public void setScholasticStatus(String scholasticStatus) { this.scholasticStatus = scholasticStatus; }

    public LocalDateTime getLastSyncedAt() { return lastSyncedAt; }
    public void setLastSyncedAt(LocalDateTime lastSyncedAt) { this.lastSyncedAt = lastSyncedAt; }

    public LocalDateTime getSourceUpdatedAt() { return sourceUpdatedAt; }
    public void setSourceUpdatedAt(LocalDateTime sourceUpdatedAt) { this.sourceUpdatedAt = sourceUpdatedAt; }


}