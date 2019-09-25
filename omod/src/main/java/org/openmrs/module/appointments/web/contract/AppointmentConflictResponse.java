package org.openmrs.module.appointments.web.contract;

import java.util.List;

public class AppointmentConflictResponse {
    private String type;
    private List<AppointmentDefaultResponse> appointmentDefaultResponse;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<AppointmentDefaultResponse> getAppointmentDefaultResponse() {
        return appointmentDefaultResponse;
    }

    public void setAppointmentDefaultResponse(List<AppointmentDefaultResponse> appointmentDefaultResponse) {
        this.appointmentDefaultResponse = appointmentDefaultResponse;
    }
}
