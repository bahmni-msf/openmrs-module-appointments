package org.openmrs.module.appointments.web.contract;

import org.openmrs.module.appointments.model.Appointment;

public class AppointmentConflictResponse {
    private String conflictType;
    private Appointment appointment;

    public String getConflictType() {
        return conflictType;
    }

    public void setConflictType(String conflictType) {
        this.conflictType = conflictType;
    }

    public Appointment getAppointment() {
        return appointment;
    }

    public void setAppointment(Appointment appointment) {
        this.appointment = appointment;
    }
}

