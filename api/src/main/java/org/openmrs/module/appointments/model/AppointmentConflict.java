package org.openmrs.module.appointments.model;

public class AppointmentConflict {
    private String type;
    private Appointment appointment;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Appointment getAppointment() {
        return appointment;
    }

    public void setAppointment(Appointment appointment) {
        this.appointment = appointment;
    }
}
