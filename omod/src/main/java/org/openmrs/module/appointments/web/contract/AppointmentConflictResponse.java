package org.openmrs.module.appointments.web.contract;

public class AppointmentConflictResponse {
    private String type;
    private AppointmentDefaultResponse appointmentDefaultResponse;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public AppointmentDefaultResponse getAppointmentDefaultResponse() {
        return appointmentDefaultResponse;
    }

    public void setAppointmentDefaultResponse(AppointmentDefaultResponse appointmentDefaultResponse) {
        this.appointmentDefaultResponse = appointmentDefaultResponse;
    }
}
