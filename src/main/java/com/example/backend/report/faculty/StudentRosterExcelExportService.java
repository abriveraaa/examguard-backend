package com.example.backend.report.faculty;

import com.example.backend.dto.faculty.students.FacultyStudentDTO;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.List;

@Service
public class StudentRosterExcelExportService {

    public byte[] generate(
            List<FacultyStudentDTO> students
    ) {
        try(
                Workbook workbook =
                        new XSSFWorkbook();

                ByteArrayOutputStream outputStream =
                        new ByteArrayOutputStream()
        ){

            Sheet sheet =
                    workbook.createSheet(
                            "Student Roster"
                    );

            int rowNumber = 0;

            Font headerFont =
                    workbook.createFont();

            headerFont.setBold(true);

            CellStyle headerStyle =
                    workbook.createCellStyle();

            headerStyle.setFont(
                    headerFont
            );

            Row header =
                    sheet.createRow(
                            rowNumber++
                    );

            String[] columns = {
                    "Student ID",
                    "Student Name",
                    "Email",
                    "College",
                    "Program",
                    "Year",
                    "Section"
            };

            for(int i=0;i<columns.length;i++){

                Cell cell =
                        header.createCell(i);

                cell.setCellValue(
                        columns[i]
                );

                cell.setCellStyle(
                        headerStyle
                );
            }

            for(FacultyStudentDTO student : students){

                Row row =
                        sheet.createRow(
                                rowNumber++
                        );

                row.createCell(0)
                        .setCellValue(
                                student.studentId()
                        );

                row.createCell(1).setCellValue(student.lastName() + ", " +  student.firstName());

                row.createCell(2)
                        .setCellValue(
                                student.emailAddress()
                        );

                row.createCell(3)
                        .setCellValue(
                                student.collegeName()
                        );

                row.createCell(4)
                        .setCellValue(
                                student.programCode()
                        );

                row.createCell(5)
                        .setCellValue(
                                student.yearLevel()
                        );

                row.createCell(6)
                        .setCellValue(
                                student.sectionName()
                        );
            }

            int[] widths = {4500, 7000, 9000, 7000, 4000, 2500, 4000};

            for (int i = 0; i < columns.length; i++) {
                sheet.setColumnWidth(i, widths[i]);
            }

            workbook.write(
                    outputStream
            );

            return outputStream.toByteArray();

        } catch(Exception e){
            throw new RuntimeException(
                    "Failed to generate Excel",
                    e
            );
        }
    }
}