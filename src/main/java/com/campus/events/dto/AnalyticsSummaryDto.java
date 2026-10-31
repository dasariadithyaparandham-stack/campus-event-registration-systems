package com.campus.events.dto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AnalyticsSummaryDto {

    private long totalRegistrations;
    private long totalConfirmed;
    private long totalCancelled;
    private long totalPresent;
    private long totalCapacity;
    private double attendanceRate;
    private double capacityUtilization;
    private double cancellationRate;
    private double noShowRate;

    private Map<String, Long> categoryDistribution = new LinkedHashMap<>();
    private Map<String, Long> departmentDistribution = new LinkedHashMap<>();
    private List<EventMetricItem> eventMetrics = new ArrayList<>();

    public static class EventMetricItem {
        private Long eventId;
        private String title;
        private String category;
        private String venue;
        private int capacity;
        private int registeredCount;
        private long confirmedCount;
        private long presentCount;
        private long absentCount;
        private double attendanceRate;
        private double utilizationRate;

        public EventMetricItem() {
        }

        public Long getEventId() {
            return eventId;
        }

        public void setEventId(Long eventId) {
            this.eventId = eventId;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public String getVenue() {
            return venue;
        }

        public void setVenue(String venue) {
            this.venue = venue;
        }

        public int getCapacity() {
            return capacity;
        }

        public void setCapacity(int capacity) {
            this.capacity = capacity;
        }

        public int getRegisteredCount() {
            return registeredCount;
        }

        public void setRegisteredCount(int registeredCount) {
            this.registeredCount = registeredCount;
        }

        public long getConfirmedCount() {
            return confirmedCount;
        }

        public void setConfirmedCount(long confirmedCount) {
            this.confirmedCount = confirmedCount;
        }

        public long getPresentCount() {
            return presentCount;
        }

        public void setPresentCount(long presentCount) {
            this.presentCount = presentCount;
        }

        public long getAbsentCount() {
            return absentCount;
        }

        public void setAbsentCount(long absentCount) {
            this.absentCount = absentCount;
        }

        public double getAttendanceRate() {
            return attendanceRate;
        }

        public void setAttendanceRate(double attendanceRate) {
            this.attendanceRate = attendanceRate;
        }

        public double getUtilizationRate() {
            return utilizationRate;
        }

        public void setUtilizationRate(double utilizationRate) {
            this.utilizationRate = utilizationRate;
        }
    }

    public AnalyticsSummaryDto() {
    }

    // Getters and Setters
    public long getTotalRegistrations() {
        return totalRegistrations;
    }

    public void setTotalRegistrations(long totalRegistrations) {
        this.totalRegistrations = totalRegistrations;
    }

    public long getTotalConfirmed() {
        return totalConfirmed;
    }

    public void setTotalConfirmed(long totalConfirmed) {
        this.totalConfirmed = totalConfirmed;
    }

    public long getTotalCancelled() {
        return totalCancelled;
    }

    public void setTotalCancelled(long totalCancelled) {
        this.totalCancelled = totalCancelled;
    }

    public long getTotalPresent() {
        return totalPresent;
    }

    public void setTotalPresent(long totalPresent) {
        this.totalPresent = totalPresent;
    }

    public long getTotalCapacity() {
        return totalCapacity;
    }

    public void setTotalCapacity(long totalCapacity) {
        this.totalCapacity = totalCapacity;
    }

    public double getAttendanceRate() {
        return attendanceRate;
    }

    public void setAttendanceRate(double attendanceRate) {
        this.attendanceRate = attendanceRate;
    }

    public double getCapacityUtilization() {
        return capacityUtilization;
    }

    public void setCapacityUtilization(double capacityUtilization) {
        this.capacityUtilization = capacityUtilization;
    }

    public double getCancellationRate() {
        return cancellationRate;
    }

    public void setCancellationRate(double cancellationRate) {
        this.cancellationRate = cancellationRate;
    }

    public double getNoShowRate() {
        return noShowRate;
    }

    public void setNoShowRate(double noShowRate) {
        this.noShowRate = noShowRate;
    }

    public Map<String, Long> getCategoryDistribution() {
        return categoryDistribution;
    }

    public void setCategoryDistribution(Map<String, Long> categoryDistribution) {
        this.categoryDistribution = categoryDistribution;
    }

    public Map<String, Long> getDepartmentDistribution() {
        return departmentDistribution;
    }

    public void setDepartmentDistribution(Map<String, Long> departmentDistribution) {
        this.departmentDistribution = departmentDistribution;
    }

    public List<EventMetricItem> getEventMetrics() {
        return eventMetrics;
    }

    public void setEventMetrics(List<EventMetricItem> eventMetrics) {
        this.eventMetrics = eventMetrics;
    }
}

