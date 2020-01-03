package org.openmrs.module.appointments.advice;

import org.openmrs.api.context.Context;
import org.openmrs.module.appointments.model.Appointment;
import org.openmrs.module.appointments.model.AppointmentRecurringPattern;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.openmrs.module.appointments.constants.AppointmentsEventRecordsConstants.CATEGORY;
import static org.openmrs.module.appointments.constants.AppointmentsEventRecordsConstants.DEFAULT_URL_PATTERN;
import static org.openmrs.module.appointments.constants.AppointmentsEventRecordsConstants.RAISE_EVENT_GLOBAL_PROPERTY;
import static org.openmrs.module.appointments.constants.AppointmentsEventRecordsConstants.URL_PATTERN_GLOBAL_PROPERTY;

public class RecurringAppointmentsAdvice extends AbstractBaseAdvice {

    private static final String TITLE = "RecurringAppointment";
    private static final String VALIDATE_AND_SAVE = "validateAndSave";
    private static final String UPDATE = "update";
    private static final String CHANGE_STATUS = "changeStatus";
    private static final List<String> METHOD_NAMES = Arrays.asList(VALIDATE_AND_SAVE, UPDATE, CHANGE_STATUS);

    @Override
    public void afterReturning(Object returnValue, Method method, Object[] arguments, Object target) throws Throwable {
        if (VALIDATE_AND_SAVE.equals(method.getName()) || isAllRecurringAppointmentsUpdate(method, returnValue)) {
            AppointmentRecurringPattern appointmentRecurringPattern = (AppointmentRecurringPattern) returnValue;
            for (Appointment appointment : appointmentRecurringPattern.getAppointments()) {
                super.afterReturning(appointment, method, arguments, target);
            }
        } else if (isSingleRecurringAppointmentUpdate(method, returnValue)) {
            List<Appointment> updatedAppointments = (List<Appointment>) arguments[1];
            for (Appointment appointment : updatedAppointments) {
                super.afterReturning(appointment, method, arguments, target);
            }
        } else if (CHANGE_STATUS.equals(method.getName())) {
            List<Appointment> appointments = (List<Appointment>) returnValue;
            for (Appointment appointment : appointments) {
                super.afterReturning(appointment, method, arguments, target);
            }
        }
    }

    private boolean isSingleRecurringAppointmentUpdate(Method method, Object processedReturnValue) {
        return UPDATE.equals(method.getName()) && (processedReturnValue instanceof Appointment);
    }

    private boolean isAllRecurringAppointmentsUpdate(Method method, Object processedReturnValue) {
        return UPDATE.equals(method.getName()) && (processedReturnValue instanceof AppointmentRecurringPattern);
    }

    @Override
    protected String getContents(Object returnValue) {
        return getUrlPattern(URL_PATTERN_GLOBAL_PROPERTY, DEFAULT_URL_PATTERN)
                .replace("{uuid}", ((Appointment) returnValue).getUuid());
    }

    @Override
    protected String getTitle() {
        return TITLE;
    }

    @Override
    protected String getCategory() {
        return CATEGORY;
    }

    @Override
    protected boolean shouldRaiseEventForMethod(String methodName) {
        return METHOD_NAMES.contains(methodName);
    }

    @Override
    protected boolean shouldRaiseEvent() {
        return Boolean.valueOf(Context.getAdministrationService().getGlobalProperty(RAISE_EVENT_GLOBAL_PROPERTY));
    }
}