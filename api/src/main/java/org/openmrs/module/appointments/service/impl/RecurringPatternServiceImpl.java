package org.openmrs.module.appointments.service.impl;

import org.openmrs.api.APIException;
import org.openmrs.module.appointments.dao.AppointmentRecurringPatternDao;
import org.openmrs.module.appointments.model.Appointment;
import org.openmrs.module.appointments.model.RecurringPattern;
import org.openmrs.module.appointments.service.AppointmentsService;
import org.openmrs.module.appointments.service.RecurringPatternService;
import org.springframework.transaction.annotation.Transactional;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Transactional
public class RecurringPatternServiceImpl implements RecurringPatternService {


    private final String TIME_FORMAT = "HH:mm:ss";
    private final String DATE_FORMAT = "yyyy-MM-dd";

    private AppointmentRecurringPatternDao appointmentRecurringPatternDao;
    private AppointmentsService appointmentsService;

    public void setAppointmentsService(AppointmentsService appointmentsService) {
        this.appointmentsService = appointmentsService;
    }

    public void setAppointmentRecurringPatternDao(AppointmentRecurringPatternDao appointmentRecurringPatternDao) {
        this.appointmentRecurringPatternDao = appointmentRecurringPatternDao;
    }

    @Override
    public List<Appointment> saveRecurringAppointments(Appointment appointment, RecurringPattern recurringPattern) {
        SimpleDateFormat timeFormat = new SimpleDateFormat(TIME_FORMAT);
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
        String appointmentSlotStartTime = timeFormat.format(appointment.getStartDateTime());
        String appointmentSlotEndTime = timeFormat.format(appointment.getEndDateTime());
        List<Appointment> appointments = new ArrayList<>();

        List<Date> appointmentDates = getRecurringDates(appointment.getStartDateTime(), recurringPattern);
        for (Date appointmentDate : appointmentDates) {
            String formattedDate = dateFormat.format(appointmentDate);

            try {
                appointment.setStartDateTime(getFormattedDateTime(formattedDate, appointmentSlotStartTime));
                appointment.setEndDateTime(getFormattedDateTime(formattedDate, appointmentSlotEndTime));

            } catch (ParseException e) {
                throw new APIException(e);
            }
            Appointment currentAppointment = createAppointment(appointment);

            appointments.add(appointmentsService.validateAndSave(currentAppointment));
//            appointments.add(appointmentsService.validateAndSave(appointment));
        }
        recurringPattern.setAppointments(new HashSet<>(appointments));
        saveRecurringPattern(recurringPattern);
        return appointments;
    }

    private Appointment createAppointment(Appointment appointment) {
        Appointment currentAppointment = new Appointment();

        currentAppointment.setService(appointment.getService());
        currentAppointment.setStartDateTime(appointment.getStartDateTime());
        currentAppointment.setEndDateTime(appointment.getEndDateTime());
        currentAppointment.setPatient(appointment.getPatient());
        currentAppointment.setServiceType(appointment.getServiceType());
        currentAppointment.setAppointmentKind(appointment.getAppointmentKind());
        currentAppointment.setLocation(appointment.getLocation());
        currentAppointment.setProviders(appointment.getProviders());
        currentAppointment.setComments(appointment.getComments());

        return currentAppointment;
    }

    private void saveRecurringPattern(RecurringPattern recurringPattern) {
        appointmentRecurringPatternDao.save(recurringPattern);
    }

    private Date getFormattedDateTime(String formattedDate, String time) throws ParseException {
        SimpleDateFormat dateTimeFormat = new SimpleDateFormat(DATE_FORMAT + " " + TIME_FORMAT);
        return dateTimeFormat.parse(formattedDate + " " + time);
    }

    public List<Date> getRecurringDates(Date currentDate, RecurringPattern recurringPattern) {
        SimpleDateFormat timeFormat = new SimpleDateFormat(TIME_FORMAT);
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
        Calendar calendar = Calendar.getInstance();
        List<Date> appointmentDates = new ArrayList<>();
        Date endDate = null;

        if (recurringPattern.getEndDate() == null) {
            switch (recurringPattern.getType().toUpperCase()) {
                case "WEEK":
                    endDate = getEndDateForWeekType();
                    break;
                case "DAY":
                default:
                    endDate = getEndDateForDayType(calendar, recurringPattern);
                    break;
            }
        } else {
            endDate = recurringPattern.getEndDate();
        }

        String appointmentSlotTime = timeFormat.format(currentDate);
        while (!currentDate.after(endDate)) {
            try {
                appointmentDates.add(getFormattedDateTime(dateFormat.format(currentDate), appointmentSlotTime));
            } catch (ParseException e) {
                throw new APIException(e);
            }
            calendar = Calendar.getInstance();
            calendar.setTime(currentDate);
            calendar.add(Calendar.DAY_OF_MONTH, recurringPattern.getPeriod());
            currentDate = calendar.getTime();
        }
        return appointmentDates;
    }

    private Date getEndDateForDayType(Calendar calendar, RecurringPattern recurringPattern) {
        calendar.add(Calendar.DAY_OF_MONTH, recurringPattern.getPeriod() * (recurringPattern.getFrequency() - 1));
        return calendar.getTime();
    }

    private Date getEndDateForWeekType() {
        return null;
    }

    @Override
    public RecurringPattern getRecurringPatternById(int id) {
        return appointmentRecurringPatternDao.getRecurringPatternById(id);
    }
}
