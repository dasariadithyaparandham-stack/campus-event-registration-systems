package com.campus.events.service;

import com.campus.events.dto.AnalyticsSummaryDto;
import com.campus.events.dto.AnalyticsSummaryDto.EventMetricItem;
import com.campus.events.model.AttendanceStatus;
import com.campus.events.model.CampusEvent;
import com.campus.events.model.RegistrationStatus;
import com.campus.events.repository.AttendanceRepository;
import com.campus.events.repository.EventRepository;
import com.campus.events.repository.RegistrationRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AnalyticsService {

    private final RegistrationRepository registrationRepository;
    private final EventRepository eventRepository;
    private final AttendanceRepository attendanceRepository;

    public AnalyticsService(RegistrationRepository registrationRepository,
                            EventRepository eventRepository,
                            AttendanceRepository attendanceRepository) {
        this.registrationRepository = registrationRepository;
        this.eventRepository = eventRepository;
        this.attendanceRepository = attendanceRepository;
    }

    /**
     * Calculates analytics overview aggregating directly from the database.
     */
    @Transactional(readOnly = true)
    public AnalyticsSummaryDto calculateAnalytics(String category, LocalDate fromDate, LocalDate toDate) {
        LocalDateTime startDateTime = fromDate != null ? fromDate.atStartOfDay() : null;
        LocalDateTime endDateTime = toDate != null ? toDate.atTime(LocalTime.MAX) : null;
        String cleanCategory = (category != null && !category.trim().isEmpty() && !"ALL".equalsIgnoreCase(category)) ? category.trim() : null;

        AnalyticsSummaryDto summary = new AnalyticsSummaryDto();

        // 1. High-level counts
        long totalReg = registrationRepository.countFilteredRegistrations(cleanCategory, startDateTime, endDateTime);
        long confirmedReg = registrationRepository.countFilteredByStatus(RegistrationStatus.CONFIRMED, cleanCategory, startDateTime, endDateTime);
        long cancelledReg = registrationRepository.countFilteredByStatus(RegistrationStatus.CANCELLED, cleanCategory, startDateTime, endDateTime);
        long presentCount = registrationRepository.countFilteredPresentAttendance(cleanCategory, startDateTime, endDateTime);
        long totalCapacity = eventRepository.sumCapacityByCategory(cleanCategory);

        summary.setTotalRegistrations(totalReg);
        summary.setTotalConfirmed(confirmedReg);
        summary.setTotalCancelled(cancelledReg);
        summary.setTotalPresent(presentCount);
        summary.setTotalCapacity(totalCapacity);

        // Safe division formulas
        double attendanceRate = confirmedReg > 0 ? (presentCount * 100.0) / confirmedReg : 0.0;
        double capacityUtilization = totalCapacity > 0 ? (totalReg * 100.0) / totalCapacity : 0.0;
        double cancellationRate = totalReg > 0 ? (cancelledReg * 100.0) / totalReg : 0.0;
        double noShowRate = confirmedReg > 0 ? (Math.max(0, confirmedReg - presentCount) * 100.0) / confirmedReg : 0.0;

        summary.setAttendanceRate(Math.round(attendanceRate * 10.0) / 10.0);
        summary.setCapacityUtilization(Math.round(capacityUtilization * 10.0) / 10.0);
        summary.setCancellationRate(Math.round(cancellationRate * 10.0) / 10.0);
        summary.setNoShowRate(Math.round(noShowRate * 10.0) / 10.0);

        // 2. Category Distribution
        List<Object[]> catRows = registrationRepository.countRegistrationsByCategory(startDateTime, endDateTime);
        Map<String, Long> categoryMap = new LinkedHashMap<>();
        for (Object[] row : catRows) {
            String cat = (String) row[0];
            Long cnt = (Long) row[1];
            categoryMap.put(cat, cnt);
        }
        summary.setCategoryDistribution(categoryMap);

        // 3. Department Distribution
        List<Object[]> deptRows = registrationRepository.countRegistrationsByDepartment(startDateTime, endDateTime);
        Map<String, Long> deptMap = new LinkedHashMap<>();
        for (Object[] row : deptRows) {
            String dept = (String) row[0];
            Long cnt = (Long) row[1];
            deptMap.put(dept, cnt);
        }
        summary.setDepartmentDistribution(deptMap);

        // 4. Event-by-event breakdown
        List<CampusEvent> events = cleanCategory != null ?
                eventRepository.findAll().stream().filter(e -> cleanCategory.equalsIgnoreCase(e.getCategory())).toList() :
                eventRepository.findAll();

        List<EventMetricItem> metricsList = new ArrayList<>();
        for (CampusEvent ev : events) {
            EventMetricItem item = new EventMetricItem();
            item.setEventId(ev.getId());
            item.setTitle(ev.getTitle());
            item.setCategory(ev.getCategory());
            item.setVenue(ev.getVenue());
            item.setCapacity(ev.getCapacity());
            item.setRegisteredCount(ev.getRegisteredCount());

            long confirmed = registrationRepository.countByEventIdAndRegistrationStatus(ev.getId(), RegistrationStatus.CONFIRMED);
            long present = attendanceRepository.countByEventIdAndStatus(ev.getId(), AttendanceStatus.PRESENT);
            long absent = Math.max(0, confirmed - present);

            item.setConfirmedCount(confirmed);
            item.setPresentCount(present);
            item.setAbsentCount(absent);

            double attRate = confirmed > 0 ? (present * 100.0) / confirmed : 0.0;
            double utilRate = ev.getCapacity() > 0 ? (ev.getRegisteredCount() * 100.0) / ev.getCapacity() : 0.0;

            item.setAttendanceRate(Math.round(attRate * 10.0) / 10.0);
            item.setUtilizationRate(Math.round(utilRate * 10.0) / 10.0);

            metricsList.add(item);
        }
        summary.setEventMetrics(metricsList);

        return summary;
    }

    /**
     * Generates CSV export of analytics matching currently applied filters.
     */
    @Transactional(readOnly = true)
    public byte[] exportAnalyticsCsv(String category, LocalDate fromDate, LocalDate toDate) {
        AnalyticsSummaryDto summary = calculateAnalytics(category, fromDate, toDate);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (PrintWriter writer = new PrintWriter(out, true, StandardCharsets.UTF_8)) {
            writer.println("CAMPUS EVENT REGISTRATION SYSTEM - ANALYTICS REPORT");
            writer.println("Generated At," + LocalDateTime.now());
            writer.println("Category Filter," + (category != null ? category : "ALL"));
            writer.println("From Date," + (fromDate != null ? fromDate : "All Time"));
            writer.println("To Date," + (toDate != null ? toDate : "All Time"));
            writer.println();

            writer.println("KEY METRIC,VALUE");
            writer.println("Total Registrations," + summary.getTotalRegistrations());
            writer.println("Confirmed Registrations," + summary.getTotalConfirmed());
            writer.println("Cancelled Registrations," + summary.getTotalCancelled());
            writer.println("Verified Attendees (Present)," + summary.getTotalPresent());
            writer.println("Overall Capacity Utilization (%)," + summary.getCapacityUtilization() + "%");
            writer.println("Attendance Turnout Rate (%)," + summary.getAttendanceRate() + "%");
            writer.println("Cancellation Rate (%)," + summary.getCancellationRate() + "%");
            writer.println("No-Show Rate (%)," + summary.getNoShowRate() + "%");
            writer.println();

            writer.println("EVENT-WISE PERFORMANCE COMPARISON");
            writer.println("Event ID,Title,Category,Venue,Capacity,Registrations,Confirmed,Present,Absent,Turnout (%),Capacity Utilization (%)");

            for (EventMetricItem item : summary.getEventMetrics()) {
                writer.printf("%d,\"%s\",\"%s\",\"%s\",%d,%d,%d,%d,%d,%.1f%%,%.1f%%%n",
                        item.getEventId(),
                        escapeCsv(item.getTitle()),
                        escapeCsv(item.getCategory()),
                        escapeCsv(item.getVenue()),
                        item.getCapacity(),
                        item.getRegisteredCount(),
                        item.getConfirmedCount(),
                        item.getPresentCount(),
                        item.getAbsentCount(),
                        item.getAttendanceRate(),
                        item.getUtilizationRate()
                );
            }
        }
        return out.toByteArray();
    }

    /**
     * Generates Excel workbook export of analytics matching currently applied filters.
     */
    @Transactional(readOnly = true)
    public byte[] exportAnalyticsExcel(String category, LocalDate fromDate, LocalDate toDate) {
        AnalyticsSummaryDto summary = calculateAnalytics(category, fromDate, toDate);

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            // Sheet 1: Executive KPI Summary
            Sheet kpiSheet = workbook.createSheet("Executive Summary");
            CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Row r0 = kpiSheet.createRow(0);
            r0.createCell(0).setCellValue("Metric");
            r0.createCell(1).setCellValue("Value");
            r0.getCell(0).setCellStyle(headerStyle);
            r0.getCell(1).setCellStyle(headerStyle);

            Object[][] kpis = {
                    {"Total Registrations", summary.getTotalRegistrations()},
                    {"Confirmed Registrations", summary.getTotalConfirmed()},
                    {"Cancelled Registrations", summary.getTotalCancelled()},
                    {"Verified Attendees (Present)", summary.getTotalPresent()},
                    {"Capacity Utilization (%)", summary.getCapacityUtilization() + "%"},
                    {"Attendance Turnout Rate (%)", summary.getAttendanceRate() + "%"},
                    {"Cancellation Rate (%)", summary.getCancellationRate() + "%"},
                    {"No-Show Rate (%)", summary.getNoShowRate() + "%"}
            };

            for (int i = 0; i < kpis.length; i++) {
                Row row = kpiSheet.createRow(i + 1);
                row.createCell(0).setCellValue((String) kpis[i][0]);
                row.createCell(1).setCellValue(kpis[i][1].toString());
            }

            kpiSheet.autoSizeColumn(0);
            kpiSheet.autoSizeColumn(1);

            // Sheet 2: Event Performance Breakdown
            Sheet eventSheet = workbook.createSheet("Event Performance");
            Row er0 = eventSheet.createRow(0);
            String[] headers = {"Event ID", "Title", "Category", "Venue", "Capacity", "Registrations", "Confirmed", "Present", "Absent", "Turnout %", "Utilization %"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = er0.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (EventMetricItem item : summary.getEventMetrics()) {
                Row row = eventSheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(item.getEventId());
                row.createCell(1).setCellValue(item.getTitle());
                row.createCell(2).setCellValue(item.getCategory());
                row.createCell(3).setCellValue(item.getVenue());
                row.createCell(4).setCellValue(item.getCapacity());
                row.createCell(5).setCellValue(item.getRegisteredCount());
                row.createCell(6).setCellValue(item.getConfirmedCount());
                row.createCell(7).setCellValue(item.getPresentCount());
                row.createCell(8).setCellValue(item.getAbsentCount());
                row.createCell(9).setCellValue(item.getAttendanceRate() + "%");
                row.createCell(10).setCellValue(item.getUtilizationRate() + "%");
            }

            for (int i = 0; i < headers.length; i++) {
                eventSheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate analytics Excel export: " + e.getMessage(), e);
        }
    }

    private String escapeCsv(String input) {
        if (input == null) return "";
        return input.replace("\"", "\"\"");
    }
}

