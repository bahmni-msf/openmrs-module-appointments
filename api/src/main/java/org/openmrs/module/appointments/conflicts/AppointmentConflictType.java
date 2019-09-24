package org.openmrs.module.appointments.conflicts;

import org.openmrs.module.appointments.model.Appointment;
import org.openmrs.module.appointments.model.AppointmentConflict;


public interface AppointmentConflictType {

    AppointmentConflict getAppointmentConflicts(Appointment appointment);
}
