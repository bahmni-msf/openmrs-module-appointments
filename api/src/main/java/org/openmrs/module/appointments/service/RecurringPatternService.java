package org.openmrs.module.appointments.service;

import org.openmrs.annotation.Authorized;
import org.openmrs.module.appointments.model.Appointment;
import org.openmrs.module.appointments.model.RecurringPattern;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

public interface RecurringPatternService {

    @Transactional
    @Authorized({"Manage Recurring Appointments"})
    List<Appointment> saveRecurringAppointments(Appointment appointment, RecurringPattern recurringPattern);

    @Transactional
    RecurringPattern getRecurringPatternById(int id);


    List<Date> getRecurringDates(Date appointmentStartDateTime, RecurringPattern recurringPattern);


}
