package org.openmrs.module.appointments.web.validators;

import org.apache.commons.collections.CollectionUtils;
import org.openmrs.api.APIException;
import org.openmrs.module.appointments.service.impl.RecurringAppointmentType;
import org.openmrs.module.appointments.web.contract.RecurringPattern;
import org.springframework.stereotype.Component;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static org.openmrs.module.appointments.service.impl.RecurringAppointmentType.valueOf;

@Component
public class RecurringPatternValidator implements Validator {
    @Override
    public boolean supports(Class<?> aClass) {
        return RecurringPattern.class.equals(aClass);
    }

    @Override
    public void validate(Object o, Errors errors) {
        RecurringPattern recurringPattern = (RecurringPattern)o;
        boolean isValidRecurringPattern = isValidRecurringAppointmentsPattern(recurringPattern);
        if (!isValidRecurringPattern) {
           errors.reject(String.format("type should be %s/%s\n" +
                            "period and frequency/endDate are mandatory if type is DAY\n" +
                            "daysOfWeek, period and frequency/endDate are mandatory if type is WEEK",
                    RecurringAppointmentType.DAY, RecurringAppointmentType.WEEK));
        }
    }

    private boolean isValidRecurringAppointmentsPattern(RecurringPattern recurringPattern) {
        boolean isValidRecurringPattern = false;
        try {
            switch (valueOf(recurringPattern.getType().toUpperCase())) {
                case WEEK:
                    isValidRecurringPattern = isValidWeeklyRecurringAppointmentsPattern(recurringPattern);
                    break;
                case DAY:
                    isValidRecurringPattern = isValidDailyRecurringAppointmentsPattern(recurringPattern);
                    break;
            }
        } catch (Exception e) {
        }
        return isValidRecurringPattern;
    }

    private boolean isValidDailyRecurringAppointmentsPattern(RecurringPattern recurringPattern) {
        return recurringPattern.getPeriod() > 0
                && (hasValidFrequencyOrEndDate(recurringPattern.getFrequency(), recurringPattern.getEndDate()));
    }

    private boolean isValidWeeklyRecurringAppointmentsPattern(RecurringPattern recurringPattern) {
        return areValidDays(recurringPattern.getDaysOfWeek())
                && isValidDailyRecurringAppointmentsPattern(recurringPattern);
    }

    private boolean areValidDays(List<String> daysOfWeek) {
        List<String> WEEK_DAYS = Arrays.asList("SUNDAY", "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY");
        return CollectionUtils.isNotEmpty(daysOfWeek)
                && WEEK_DAYS.containsAll(daysOfWeek.stream().map(String::toUpperCase).collect(Collectors.toList()));
    }

    private boolean hasValidFrequencyOrEndDate(Integer frequency, Date endDate) {
        return (frequency != null && frequency > 0) || endDate != null;
    }

    public void isValidRecurringPattern(RecurringPattern recurringPattern) {
        Errors errors = new BeanPropertyBindingResult(recurringPattern, "recurringPattern");
        validate(recurringPattern, errors);
        if (!errors.getAllErrors().isEmpty()) {
            throw new APIException(errors.getAllErrors().get(0).getCodes()[1]);
        }
    }
}
