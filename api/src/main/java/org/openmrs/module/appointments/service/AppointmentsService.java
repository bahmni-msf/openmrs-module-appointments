package org.openmrs.module.appointments.service;


import org.openmrs.annotation.Authorized;
import org.openmrs.module.appointments.model.Appointment;
import org.openmrs.module.appointments.model.AppointmentProvider;
import org.openmrs.module.appointments.model.AppointmentServiceDefinition;
import org.openmrs.module.appointments.model.AppointmentServiceType;
import org.openmrs.module.appointments.model.AppointmentStatus;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

import static org.openmrs.module.appointments.constants.PrivilegeConstants.MANAGE_APPOINTMENTS;
import static org.openmrs.module.appointments.constants.PrivilegeConstants.MANAGE_OWN_APPOINTMENTS;
import static org.openmrs.module.appointments.constants.PrivilegeConstants.VIEW_APPOINTMENTS;

public interface AppointmentsService {

    @Transactional
    @Authorized({MANAGE_APPOINTMENTS, MANAGE_OWN_APPOINTMENTS})
    Appointment validateAndSave(Appointment appointment);

    @Transactional
    @Authorized({VIEW_APPOINTMENTS})
    List<Appointment> getAllAppointments(Date forDate);

    @Transactional
    @Authorized({VIEW_APPOINTMENTS})
    List<Appointment> search(Appointment appointment);

    @Transactional
    @Authorized({VIEW_APPOINTMENTS})
    List<Appointment> getAllFutureAppointmentsForService(AppointmentServiceDefinition appointmentServiceDefinition);

    @Transactional
    @Authorized({VIEW_APPOINTMENTS})
    List<Appointment> getAllFutureAppointmentsForServiceType(AppointmentServiceType appointmentServiceType);

    @Transactional
    @Authorized({VIEW_APPOINTMENTS})
    List<Appointment> getAppointmentsForService(AppointmentServiceDefinition appointmentServiceDefinition, Date startDate, Date endDate, List<AppointmentStatus> appointmentStatusList);

    @Transactional
    @Authorized({VIEW_APPOINTMENTS})
    Appointment getAppointmentByUuid(String uuid);

    @Transactional
    @Authorized({MANAGE_APPOINTMENTS, MANAGE_OWN_APPOINTMENTS})
    void changeStatus(Appointment appointment, String status, Date onDate);

    @Transactional
    @Authorized({VIEW_APPOINTMENTS})
    List<Appointment> getAllAppointmentsInDateRange(Date startDate, Date endDate);

    @Transactional
    @Authorized({MANAGE_APPOINTMENTS, MANAGE_OWN_APPOINTMENTS})
    void undoStatusChange(Appointment appointment);

    @Transactional
    @Authorized({"Manage Appointments"})
    void updateAppointmentProviderResponse(AppointmentProvider appointmentProviderProvider);

    @Transactional
    @Authorized({"Manage Appointments"})
    Appointment reschedule(String originalAppointmentUuid, Appointment appointment, boolean retainAppointmentNumber);

    @Transactional
    @Authorized({MANAGE_APPOINTMENTS, MANAGE_OWN_APPOINTMENTS})
    Appointment update(Appointment appointment);
}

