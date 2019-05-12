package org.openmrs.module.appointments.model;

public enum AppointmentKind {
    Scheduled("Scheduled"), WalkIn("WalkIn"), Recurring("Recurring");

    private final String value;

    AppointmentKind(String value) {
        this.value = value;
    }
}


