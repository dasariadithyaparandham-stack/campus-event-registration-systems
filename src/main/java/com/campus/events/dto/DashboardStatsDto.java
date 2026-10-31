package com.campus.events.dto;

import java.util.List;
import com.campus.events.model.CampusEvent;
import com.campus.events.model.EventRegistration;

public class DashboardStatsDto {

    private long totalStudents;
    private long totalEvents;
    private long openEvents;
    private long closedEvents;
    private long cancelledEvents;
    private long completedEvents;
    private long totalRegistrations;
    private long confirmedRegistrations;
    private long totalAttendees;
    private double overallAttendancePercentage;

    private List<CampusEvent> popularEvents;
    private List<EventRegistration> recentRegistrations;

    public DashboardStatsDto() {
    }

    public long getTotalStudents() {
        return totalStudents;
    }

    public void setTotalStudents(long totalStudents) {
        this.totalStudents = totalStudents;
    }

    public long getTotalEvents() {
        return totalEvents;
    }

    public void setTotalEvents(long totalEvents) {
        this.totalEvents = totalEvents;
    }

    public long getOpenEvents() {
        return openEvents;
    }

    public void setOpenEvents(long openEvents) {
        this.openEvents = openEvents;
    }

    public long getClosedEvents() {
        return closedEvents;
    }

    public void setClosedEvents(long closedEvents) {
        this.closedEvents = closedEvents;
    }

    public long getCancelledEvents() {
        return cancelledEvents;
    }

    public void setCancelledEvents(long cancelledEvents) {
        this.cancelledEvents = cancelledEvents;
    }

    public long getCompletedEvents() {
        return completedEvents;
    }

    public void setCompletedEvents(long completedEvents) {
        this.completedEvents = completedEvents;
    }

    public long getTotalRegistrations() {
        return totalRegistrations;
    }

    public void setTotalRegistrations(long totalRegistrations) {
        this.totalRegistrations = totalRegistrations;
    }

    public long getConfirmedRegistrations() {
        return confirmedRegistrations;
    }

    public void setConfirmedRegistrations(long confirmedRegistrations) {
        this.confirmedRegistrations = confirmedRegistrations;
    }

    public long getTotalAttendees() {
        return totalAttendees;
    }

    public void setTotalAttendees(long totalAttendees) {
        this.totalAttendees = totalAttendees;
    }

    public double getOverallAttendancePercentage() {
        return overallAttendancePercentage;
    }

    public void setOverallAttendancePercentage(double overallAttendancePercentage) {
        this.overallAttendancePercentage = overallAttendancePercentage;
    }

    public List<CampusEvent> getPopularEvents() {
        return popularEvents;
    }

    public void setPopularEvents(List<CampusEvent> popularEvents) {
        this.popularEvents = popularEvents;
    }

    public List<EventRegistration> getRecentRegistrations() {
        return recentRegistrations;
    }

    public void setRecentRegistrations(List<EventRegistration> recentRegistrations) {
        this.recentRegistrations = recentRegistrations;
    }
}

