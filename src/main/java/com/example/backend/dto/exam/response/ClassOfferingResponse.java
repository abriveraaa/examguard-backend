package com.example.backend.dto.exam.response;

import java.io.Serializable;

public class ClassOfferingResponse implements Serializable {

    private String classOfferingId;
    private String programCode;
    private Integer yearLevel;
    private String sectionName;
    private String courseCode;
    private String courseDescription;

    public String getDisplayName() {
        return programCode + " | " + courseCode + " | " + courseDescription;
    }

    // getters & setters

    public String getClassOfferingId() {
        return classOfferingId;
    }

    public void setClassOfferingId(String classOfferingId) {
        this.classOfferingId = classOfferingId;
    }

    public String getProgramCode() {
        return programCode;
    }

    public void setProgramCode(String programCode) {
        this.programCode = programCode;
    }

    public Integer getYearLevel() {
        return yearLevel;
    }

    public void setYearLevel(Integer yearLevel) {
        this.yearLevel = yearLevel;
    }

    public String getSectionName() {
        return sectionName;
    }

    public void setSectionName(String sectionName) {
        this.sectionName = sectionName;
    }

    public String getCourseCode() {
        return courseCode;
    }

    public void setCourseCode(String courseCode) {
        this.courseCode = courseCode;
    }

    public String getCourseDescription() {
        return courseDescription;
    }

    public void setCourseDescription(String courseDescription) {
        this.courseDescription = courseDescription;
    }
}