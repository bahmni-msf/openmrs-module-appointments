package org.openmrs.module.appointments.web.contract;

import java.util.List;

public class RecurringAppointmentsResponse {

    private RecurringPattern recurringPattern;
    private List<AppointmentDefaultResponse> appointments;

    public RecurringAppointmentsResponse(RecurringPattern recurringPattern, List<AppointmentDefaultResponse> appointments) {
        this.recurringPattern = recurringPattern;
        this.appointments = appointments;
    }

    public RecurringPattern getRecurringPattern() {
        return recurringPattern;
    }

    public List<AppointmentDefaultResponse> getAppointments() {
        return appointments;
    }
}
