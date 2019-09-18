package org.openmrs.module.appointments.web.service;
import org.apache.commons.lang3.tuple.Pair;
import org.openmrs.api.APIException;
import org.openmrs.module.appointments.model.Appointment;
import org.openmrs.module.appointments.model.AppointmentConflict;
import org.openmrs.module.appointments.model.AppointmentRecurringPattern;
import org.openmrs.module.appointments.model.AppointmentServiceDefinition;
import org.openmrs.module.appointments.model.AppointmentStatus;
import org.openmrs.module.appointments.model.ServiceWeeklyAvailability;
import org.openmrs.module.appointments.service.AppointmentServiceDefinitionService;
import org.openmrs.module.appointments.service.AppointmentsService;
import org.openmrs.module.appointments.web.contract.AppointmentRequest;
import org.openmrs.module.appointments.web.contract.RecurringAppointmentRequest;
import org.openmrs.module.appointments.web.mapper.AppointmentMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public abstract class AbstractRecurringAppointmentsService {

    @Autowired
    private AppointmentMapper appointmentMapper;

    @Autowired
    private AppointmentServiceDefinitionService appointmentServiceDefinitionService;

    @Autowired
    private AppointmentsService appointmentsService;

    public abstract List<Pair<Date, Date>> generateAppointmentDates(RecurringAppointmentRequest recurringAppointmentRequest);

    public abstract List<Appointment> addAppointments(AppointmentRecurringPattern appointmentRecurringPattern,
                                                      RecurringAppointmentRequest recurringAppointmentRequest);

    public List<Appointment> createAppointments(List<Pair<Date, Date>> appointmentDates,
                                                   AppointmentRequest appointmentRequest) {
        List<Appointment> appointments = new ArrayList<>();
        appointmentDates.forEach(appointmentDate -> {
            Appointment appointment = appointmentMapper.fromRequest(appointmentRequest);
            appointment.setStartDateTime(appointmentDate.getLeft());
            appointment.setEndDateTime(appointmentDate.getRight());
            appointments.add(appointment);
        });
        return appointments;
    }

    public List<Appointment> sort(List<Appointment> appointments) {
        appointments.sort(Comparator.comparing(Appointment::getStartDateTime));
        return appointments;
    }

    public List<Appointment> removeRecurringAppointments(AppointmentRecurringPattern appointmentRecurringPattern,
                                                         RecurringAppointmentRequest recurringAppointmentRequest) {
        List<Appointment> appointments = sort(new ArrayList<>(appointmentRecurringPattern.getActiveAppointments()));
        if (appointmentRecurringPattern.getEndDate() == null)
            appointmentRecurringPattern.setFrequency(appointmentRecurringPattern.getFrequency() -
                    recurringAppointmentRequest.getRecurringPattern().getFrequency());
        else
            appointmentRecurringPattern.setEndDate(recurringAppointmentRequest.getRecurringPattern().getEndDate());
        recurringAppointmentRequest.getAppointmentRequest().setStartDateTime(appointments.get(0).getStartDateTime());
        recurringAppointmentRequest.getAppointmentRequest().setEndDateTime(appointments.get(0).getEndDateTime());
        Calendar startCalender = Calendar.getInstance();
        Date currentDate = startCalender.getTime();
        List<Integer> removableAppointments = new ArrayList<>();
        removableAppointments = getRemovableAppointments(appointmentRecurringPattern, recurringAppointmentRequest,
                appointments, currentDate, removableAppointments);
        checkAppointmentStatus(appointments, removableAppointments);
        for (int appointmentIndex = 0; appointmentIndex < removableAppointments.size(); appointmentIndex++) {
            appointments.get(removableAppointments.get(appointmentIndex)).setVoided(true);
        }
        return sort(new ArrayList<>(appointmentRecurringPattern.getActiveAppointments()));
    }

    private void checkAppointmentStatus(List<Appointment> appointments, List<Integer> removableAppointments) {
        for (int index = 0; index < removableAppointments.size(); index++) {
            if (appointments.get(removableAppointments.get(index)).getStatus().equals(AppointmentStatus.CheckedIn))
                throw new APIException("Changes cannot be made as the appointments are already Checked-In");
            if (appointments.get(removableAppointments.get(index)).getStatus().equals(AppointmentStatus.Missed))
                throw new APIException("Changes cannot be made as the appointments are already Missed");
        }
    }

    private List<Integer> getRemovableAppointments(AppointmentRecurringPattern appointmentRecurringPattern,
                                                   RecurringAppointmentRequest recurringAppointmentRequest,
                                                   List<Appointment> appointments, Date currentDate,
                                                   List<Integer> removableAppointments) {
        if (appointmentRecurringPattern.getEndDate() == null) {
            if (appointments.get(recurringAppointmentRequest.getRecurringPattern().getFrequency()).getStartDateTime().before(currentDate))
                throw new APIException(String.format("Changes cannot be made as the appointments are from past date"));
            for (int index = recurringAppointmentRequest.getRecurringPattern().getFrequency(); index < appointments.size(); index++)
                removableAppointments.add(index);
        } else{
            if(currentDate.after(recurringAppointmentRequest.getRecurringPattern().getEndDate()))
                throw new APIException(String.format("Changes cannot be made as the appointments are from past date"));
            for (int index = 0; index < appointments.size(); index++) {
                if (appointments.get(index).getStartDateTime().after(appointmentRecurringPattern.getEndDate())) {
                    removableAppointments.add(index);
                }
            }
        }
        return removableAppointments;
    }

    public List<AppointmentConflict> getAppointmentConflicts(List<Pair<Date, Date>> appointmentDates, AppointmentRequest appointmentRequest, List<String> daysOfWeek) {
        List<AppointmentConflict> appointmentConflicts = new ArrayList<>();
        AppointmentServiceDefinition appointmentServiceDefinition = appointmentServiceDefinitionService
                .getAppointmentServiceByUuid(appointmentRequest.getServiceUuid());
        appointmentDates.forEach(appointmentDate -> {
            Appointment appointment = appointmentMapper.fromRequest(appointmentRequest);
            appointment.setStartDateTime(appointmentDate.getLeft());
            appointment.setEndDateTime(appointmentDate.getRight());
            AppointmentConflict appointmentConflict = checkConflicts(appointment, appointmentServiceDefinition);
            if (!Objects.isNull(appointmentConflict))
                appointmentConflicts.add(appointmentConflict);
        });
        return appointmentConflicts;
    }

    private AppointmentConflict checkConflicts(Appointment appointment, AppointmentServiceDefinition appointmentServiceDefinition) {
        AppointmentConflict appointmentConflict = new AppointmentConflict();
        if (haveServiceConflicts(appointment, appointmentConflict, appointmentServiceDefinition))
            return appointmentConflict;

        if (haveConcurrentBookings(appointment, appointmentConflict))
            return appointmentConflict;


        return null;
    }

    private boolean haveConcurrentBookings(Appointment appointment, AppointmentConflict appointmentConflict) {

        List<Appointment> patientAppointments = appointmentsService.getAppointmentsForPatient(
                appointment.getAppointmentId(),appointment.getPatient().getPatientId());

        if (patientAppointments.stream().anyMatch(patientAppointment ->
                isConflicting(appointment.getStartDateTime(), appointment.getEndDateTime(), patientAppointment))) {
            appointmentConflict.setType("Double Booking");
            appointmentConflict.setAppointment(appointment);
            return true;
        }
        return false;
    }

    private boolean isConflicting(Date startTime, Date endTime, Appointment patientAppointment) {
        return (startTime.after(patientAppointment.getStartDateTime())
                && startTime.before(patientAppointment.getEndDateTime())) ||
        (endTime.after(patientAppointment.getStartDateTime())
                && endTime.before(patientAppointment.getStartDateTime()));
    }

    private boolean haveServiceConflicts(Appointment appointment, AppointmentConflict appointmentConflict,
                                         AppointmentServiceDefinition appointmentServiceDefinition) {
        Set<ServiceWeeklyAvailability> availableDays = appointmentServiceDefinition.getWeeklyAvailability();
        Calendar appointmentCal = Calendar.getInstance();
        appointmentCal.setTime(appointment.getStartDateTime());
        int appointmentDay = appointmentCal.get(Calendar.DAY_OF_WEEK);

        boolean isServiceAvailable = availableDays.stream().anyMatch(day -> day.getDayOfWeek().getValue() == appointmentDay);
        if (!isServiceAvailable) {
            appointmentConflict.setType("Service Unavailable");
            appointmentConflict.setAppointment(appointment);
            return true;
        }
        return false;
    }
}
