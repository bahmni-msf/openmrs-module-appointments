package org.openmrs.module.appointments.conflicts.impl;

import org.openmrs.module.appointments.conflicts.AppointmentConflictType;
import org.openmrs.module.appointments.model.Appointment;
import org.openmrs.module.appointments.model.AppointmentConflict;
import org.openmrs.module.appointments.model.AppointmentServiceDefinition;
import org.openmrs.module.appointments.model.ServiceWeeklyAvailability;

import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static org.openmrs.module.appointments.constants.AppointmentConflictTypeEnum.SERVICE_UNAVAILABLE;
import static org.openmrs.module.appointments.util.DateUtil.getCalendar;


public class AppointmentServiceUnavailabilityConflict implements AppointmentConflictType {

    private final SimpleDateFormat DayFormat = new SimpleDateFormat("EEEE");

    @Override
    public AppointmentConflict getAppointmentConflicts(Appointment appointment) {
        AppointmentServiceDefinition appointmentServiceDefinition = appointment.getService();
        return checkConflicts(appointment, appointmentServiceDefinition);
    }

    private AppointmentConflict checkConflicts(Appointment appointment, AppointmentServiceDefinition appointmentServiceDefinition) {
        Set<ServiceWeeklyAvailability> serviceAvailableDays = appointmentServiceDefinition.getWeeklyAvailability();
        if (Objects.nonNull(serviceAvailableDays) && !serviceAvailableDays.isEmpty()) {
            String appointmentDay = DayFormat.format(appointment.getStartDateTime());
            Optional<ServiceWeeklyAvailability> serviceWeeklyAvailabilityField = serviceAvailableDays.stream().filter(day
                    -> day.isSameDayOfWeek(appointmentDay)).findFirst();
            ServiceWeeklyAvailability serviceWeeklyAvailability;
            if (serviceWeeklyAvailabilityField.isPresent()) {
                serviceWeeklyAvailability = serviceWeeklyAvailabilityField.get();
                return checkTimeSlot(appointment, serviceWeeklyAvailability.getStartTime(), serviceWeeklyAvailability.getEndTime());
            }
            return createConflict(appointment);
        } else {
            return checkTimeSlot(appointment,
                    appointmentServiceDefinition.getStartTime(), appointmentServiceDefinition.getEndTime());
        }
    }

    private AppointmentConflict checkTimeSlot(Appointment appointment, Time serviceStartTime, Time serviceEndTime) {
        long appointmentStartTimeMilliSeconds = getEpochTime(appointment.getStartDateTime().getTime());
        long appointmentEndTimeMilliSeconds = getEpochTime(appointment.getEndDateTime().getTime());
        long serviceStartTimeMilliSeconds = getEpochTime(serviceStartTime.getTime());
        long serviceEndTimeMilliSeconds = getEpochTime(serviceEndTime.getTime());
        boolean isConflict = (appointmentStartTimeMilliSeconds >= appointmentEndTimeMilliSeconds)
                || ((appointmentStartTimeMilliSeconds < serviceStartTimeMilliSeconds)
                || (appointmentEndTimeMilliSeconds > serviceEndTimeMilliSeconds));
        if (isConflict)
            return createConflict(appointment);
        return null;
    }

    private AppointmentConflict createConflict(Appointment appointment) {
        AppointmentConflict appointmentConflict = new AppointmentConflict();
        appointmentConflict.setType(SERVICE_UNAVAILABLE.name());
        appointmentConflict.setAppointment(appointment);
        return appointmentConflict;
    }

    private long getEpochTime(long date) {
        Calendar calendar = getCalendar(new Date(date));
        int hours = calendar.get(Calendar.HOUR_OF_DAY);
        int minutes = calendar.get(Calendar.MINUTE);
        int seconds = calendar.get(Calendar.SECOND);
        return (hours * 3600 + minutes * 60 + seconds) * 1000;
    }

}
