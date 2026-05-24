package com.example.backend.report.common;

import com.lowagie.text.PageSize;
import com.lowagie.text.Rectangle;

public class ReportConfig {

    private final Rectangle pageSize;
    private final String collegeOffering;
    private final String generatedByText;

    public ReportConfig(
            Rectangle pageSize,
            String collegeOffering,
            String generatedByText
    ) {
        this.pageSize = pageSize;
        this.collegeOffering = collegeOffering;
        this.generatedByText = generatedByText;
    }

    public Rectangle getPageSize() {
        return pageSize;
    }

    public String getCollegeOffering() {
        return collegeOffering;
    }

    public String getGeneratedByText() {
        return generatedByText;
    }

    public static ReportConfig portrait(
            String collegeOffering,
            String generatedByText
    ) {
        return new ReportConfig(
                PageSize.LETTER,
                collegeOffering,
                generatedByText
        );
    }

    public static ReportConfig landscape(
            String collegeOffering,
            String generatedByText
    ) {
        return new ReportConfig(
                PageSize.LETTER.rotate(),
                collegeOffering,
                generatedByText
        );
    }
}