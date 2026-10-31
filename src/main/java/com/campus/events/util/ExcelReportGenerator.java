package com.campus.events.util;

import com.campus.events.model.CampusEvent;
import com.campus.events.model.EventRegistration;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class ExcelReportGenerator {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private ExcelReportGenerator() {
    }

    public static byte[] generateParticipantsExcel(CampusEvent event, List<EventRegistration> registrations) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Participants");

            // Header Style
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            // Title block
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("Participant List: " + event.getTitle());

            Row subRow = sheet.createRow(1);
            subRow.createCell(0).setCellValue("Venue: " + event.getVenue() + " | Start: " + event.getStartDatetime().format(DATE_FORMATTER));

            // Headers
            String[] headers = {
                    "Registration Ref", "Student Name", "Email", "Student ID", 
                    "Department", "Phone", "Registered At", "Status", "Attendance", "Check-in Time"
            };

            Row headerRow = sheet.createRow(3);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 4;
            for (EventRegistration reg : registrations) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(reg.getRegistrationReference());
                row.createCell(1).setCellValue(reg.getStudent().getFullName());
                row.createCell(2).setCellValue(reg.getStudent().getEmail());
                row.createCell(3).setCellValue(reg.getStudent().getStudentIdNumber() != null ? reg.getStudent().getStudentIdNumber() : "-");
                row.createCell(4).setCellValue(reg.getStudent().getDepartment() != null ? reg.getStudent().getDepartment() : "-");
                row.createCell(5).setCellValue(reg.getStudent().getPhone() != null ? reg.getStudent().getPhone() : "-");
                row.createCell(6).setCellValue(reg.getRegisteredAt() != null ? reg.getRegisteredAt().format(DATE_FORMATTER) : "-");
                row.createCell(7).setCellValue(reg.getRegistrationStatus().name());

                String att = (reg.getAttendance() != null && reg.getAttendance().getAttendanceStatus() != null)
                        ? reg.getAttendance().getAttendanceStatus().name() : "ABSENT";
                row.createCell(8).setCellValue(att);

                String checkIn = (reg.getAttendance() != null && reg.getAttendance().getCheckedInAt() != null)
                        ? reg.getAttendance().getCheckedInAt().format(DATE_FORMATTER) : "-";
                row.createCell(9).setCellValue(checkIn);
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate Excel report", e);
        }
    }

    public static byte[] generateEventsSummaryExcel(List<CampusEvent> events) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Events Summary");

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            String[] headers = {
                    "Event ID", "Title", "Category", "Venue", "Start Date & Time", 
                    "Deadline", "Capacity", "Registered Count", "Available Seats", "Occupancy %", "Status"
            };

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (CampusEvent e : events) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(e.getId());
                row.createCell(1).setCellValue(e.getTitle());
                row.createCell(2).setCellValue(e.getCategory());
                row.createCell(3).setCellValue(e.getVenue());
                row.createCell(4).setCellValue(e.getStartDatetime().format(DATE_FORMATTER));
                row.createCell(5).setCellValue(e.getRegistrationDeadline().format(DATE_FORMATTER));
                row.createCell(6).setCellValue(e.getCapacity());
                row.createCell(7).setCellValue(e.getRegisteredCount());
                row.createCell(8).setCellValue(e.getAvailableSeats());
                row.createCell(9).setCellValue(String.format("%.1f%%", e.getOccupancyPercentage()));
                row.createCell(10).setCellValue(e.getStatus().name());
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate Excel report", e);
        }
    }
}

