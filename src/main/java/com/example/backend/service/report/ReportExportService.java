package com.example.backend.service.report;

import com.example.backend.report.base.PdfReportExporter;
import com.example.backend.report.exam.dto.ReportExamHeaderDTO;
import com.example.backend.report.exam.dto.ReportExportResult;
import com.example.backend.report.factory.ReportExporterFactory;
import com.example.backend.report.model.ReportExportMode;
import com.example.backend.report.model.ReportRequest;
import com.example.backend.report.model.ReportType;
import com.example.backend.repository.report.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
public class ReportExportService {

    private final ReportExporterFactory factory;
    private final ReportRepository reportRepository;

    public ReportExportResult generateExamPortfolioPdf(
            Long examId,
            ReportExportMode mode,
            String classOfferingId,
            String generatedByText
    ) {
        if (mode == ReportExportMode.SEPARATE) {

            if (classOfferingId != null && !classOfferingId.isBlank()) {
                byte[] pdf = generateExamPortfolioPdfByClass(
                        examId,
                        classOfferingId,
                        generatedByText
                );

                return new ReportExportResult(
                        pdf,
                        safeFileName(getExamTitle(examId)) +
                                "_" +
                                safeFileName(classOfferingId) +
                                ".pdf",
                        MediaType.APPLICATION_PDF
                );
            }

            return generateSeparateClassReportsZip(
                    examId,
                    generatedByText
            );
        }

        byte[] pdf = generateExamPortfolioPdfByClass(
                examId,
                null,
                generatedByText
        );

        return new ReportExportResult(
                pdf,
                safeFileName(getExamTitle(examId)) + ".pdf",
                MediaType.APPLICATION_PDF
        );
    }

    private byte[] generateExamPortfolioPdfByClass(
            Long examId,
            String classOfferingId,
            String generatedByText
    ) {
        ReportRequest request = ReportRequest.builder()
                .examId(examId)
                .classOfferingId(classOfferingId)
                .reportType(ReportType.EXAM_PORTFOLIO)
                .generatedByText(generatedByText)
                .build();

        PdfReportExporter exporter =
                factory.getExporter(request.getReportType().name());

        return exporter.export(request);
    }

    private ReportExportResult generateSeparateClassReportsZip(Long examId, String generatedByText) {

        List<String> classOfferingIds =
                reportRepository.findAssignedClassOfferingIdsForReport(examId);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        String examTitle = getExamTitle(examId);

        try (ZipOutputStream zip = new ZipOutputStream(baos)) {

            for (String classOfferingId : classOfferingIds) {

                byte[] pdf =
                        generateExamPortfolioPdfByClass(examId, classOfferingId, generatedByText);

                String entryName =
                        safeFileName(examTitle)
                                + "_"
                                + safeFileName(classOfferingId)
                                + ".pdf";

                zip.putNextEntry(new ZipEntry(entryName));
                zip.write(pdf);
                zip.closeEntry();
            }

        } catch (IOException e) {
            throw new RuntimeException("Failed to generate ZIP report.", e);
        }

        return new ReportExportResult(
                baos.toByteArray(),
                safeFileName(examTitle) + "_by_section.zip",
                MediaType.APPLICATION_OCTET_STREAM
        );
    }

    private String getExamTitle(Long examId) {
        ReportExamHeaderDTO header =
                reportRepository.findExamHeaderForReport(examId, null);

        return header == null ? "exam-portfolio" : header.examTitle();
    }

    private String safeFileName(String value) {

        if (value == null || value.isBlank()) {
            return "exam-portfolio";
        }

        return value
                .trim()
                .replaceAll("[\\\\/:*?\"<>|]", "")
                .replaceAll("\\s+", "_");
    }
}