package org.openmrs.module.appointments.dao;

import org.openmrs.module.appointments.model.RecurringPattern;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface AppointmentRecurringPatternDao {
    @Transactional
    void save(RecurringPattern recurringPattern);

    List<RecurringPattern> getAllRecurringPatterns();

    RecurringPattern getRecurringPatternById(Integer id);
}
