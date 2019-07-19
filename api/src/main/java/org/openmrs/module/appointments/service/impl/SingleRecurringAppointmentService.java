package org.openmrs.module.appointments.service.impl;

import org.openmrs.module.appointments.model.Appointment;
import org.openmrs.module.appointments.model.AppointmentRecurringPattern;
import org.openmrs.module.appointments.service.RecurringAppointmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Transactional
public class SingleRecurringAppointmentService implements RecurringAppointmentService {

    public RecurringAppointmentService getRecurringAppointmentService() {
        return recurringAppointmentService;
    }

    public void setRecurringAppointmentService(RecurringAppointmentService recurringAppointmentService) {
        this.recurringAppointmentService = recurringAppointmentService;
    }

    private RecurringAppointmentService recurringAppointmentService;

    public AppointmentRecurringPattern getAppointmentRecurringPatternFrom(Appointment appointment) {
        return null;
    }

    @Override
    public List<Appointment> validateAndSave(AppointmentRecurringPattern appointmentRecurringPattern) {
       return recurringAppointmentService.validateAndSave(appointmentRecurringPattern);
    }

    @Override
    public List<Appointment> validateAndUpdate(Appointment appointment, String clientTimeZone) {
        return recurringAppointmentService.validateAndUpdate(appointment,clientTimeZone);
    }

    @Override
    public void changeStatus(Appointment appointment, String toStatus, Date onDate, String clientTimeZone) {
        recurringAppointmentService.changeStatus(appointment,toStatus,onDate,clientTimeZone);
    }

    public List<Appointment> validateAndUpdate(AppointmentRecurringPattern appointmentRecurringPattern) {
        return null;
    }
}
