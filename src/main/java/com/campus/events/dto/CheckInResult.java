package com.campus.events.dto;

import java.time.LocalDateTime;

public class CheckInResult {

    public enum Status {
        VALID,
        ALREADY_CHECKED_IN,
        CANCELLED,
        INVALID_TOKEN,
        WRONG_EVENT,
        EXPIRED
    }

    private Status status;
    private String message;
    private String studentName;
    private String studentEmail;
    private String studentIdNumber;
    private String eventTitle;
    private String registrationReference;
    private LocalDateTime checkedInAt;
    private String checkedInBy;

    public CheckInResult() {
    }

    public static CheckInResult valid(String studentName, String studentEmail, String studentIdNumber, String eventTitle, String ref, LocalDateTime checkedInAt, String staffName) {
        CheckInResult res = new CheckInResult();
        res.status = Status.VALID;
        res.message = "Check-in successful! Welcome, " + studentName + ".";
        res.studentName = studentName;
        res.studentEmail = studentEmail;
        res.studentIdNumber = studentIdNumber;
        res.eventTitle = eventTitle;
        res.registrationReference = ref;
        res.checkedInAt = checkedInAt;
        res.checkedInBy = staffName;
        return res;
    }

    public static CheckInResult alreadyCheckedIn(String studentName, String ref, LocalDateTime checkedInAt, String staffName) {
        CheckInResult res = new CheckInResult();
        res.status = Status.ALREADY_CHECKED_IN;
        res.message = "Warning: " + studentName + " (" + ref + ") was ALREADY checked in on " + checkedInAt + " by " + staffName + ".";
        res.studentName = studentName;
        res.registrationReference = ref;
        res.checkedInAt = checkedInAt;
        res.checkedInBy = staffName;
        return res;
    }

    public static CheckInResult cancelled(String studentName, String ref) {
        CheckInResult res = new CheckInResult();
        res.status = Status.CANCELLED;
        res.message = "Check-in rejected: Registration (" + ref + ") was CANCELLED.";
        res.studentName = studentName;
        res.registrationReference = ref;
        return res;
    }

    public static CheckInResult invalidToken(String token) {
        CheckInResult res = new CheckInResult();
        res.status = Status.INVALID_TOKEN;
        res.message = "Invalid ticket: No valid registration found matching identifier '" + token + "'.";
        return res;
    }

    public static CheckInResult wrongEvent(String studentName, String ref, String registeredEventTitle, String deskEventTitle) {
        CheckInResult res = new CheckInResult();
        res.status = Status.WRONG_EVENT;
        res.message = "Wrong event! Ticket (" + ref + ") is registered for '" + registeredEventTitle + "', but this check-in desk is for '" + deskEventTitle + "'.";
        res.studentName = studentName;
        res.registrationReference = ref;
        res.eventTitle = registeredEventTitle;
        return res;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getStudentEmail() {
        return studentEmail;
    }

    public void setStudentEmail(String studentEmail) {
        this.studentEmail = studentEmail;
    }

    public String getStudentIdNumber() {
        return studentIdNumber;
    }

    public void setStudentIdNumber(String studentIdNumber) {
        this.studentIdNumber = studentIdNumber;
    }

    public String getEventTitle() {
        return eventTitle;
    }

    public void setEventTitle(String eventTitle) {
        this.eventTitle = eventTitle;
    }

    public String getRegistrationReference() {
        return registrationReference;
    }

    public void setRegistrationReference(String registrationReference) {
        this.registrationReference = registrationReference;
    }

    public LocalDateTime getCheckedInAt() {
        return checkedInAt;
    }

    public void setCheckedInAt(LocalDateTime checkedInAt) {
        this.checkedInAt = checkedInAt;
    }

    public String getCheckedInBy() {
        return checkedInBy;
    }

    public void setCheckedInBy(String checkedInBy) {
        this.checkedInBy = checkedInBy;
    }
}

