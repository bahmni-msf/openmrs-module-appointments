package org.openmrs.module.appointments.dao.impl;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.openmrs.Encounter;
import org.openmrs.module.appointments.dao.AppointmentRecurringPatternDao;
import org.openmrs.module.appointments.model.Appointment;
import org.openmrs.module.appointments.model.RecurringPattern;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public class AppointmentRecurringPatternDaoImpl implements AppointmentRecurringPatternDao {

    private SessionFactory sessionFactory;

    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Transactional
    @Override
    public void save(RecurringPattern recurringPattern) {
        session().save(recurringPattern);
    }

    @Override
    public RecurringPattern getRecurringPatternById(Integer id) {
        return (RecurringPattern) session().get(RecurringPattern.class, id);
    }

    @Override
    public List<RecurringPattern> getAllRecurringPatterns() {
        return session().createCriteria(RecurringPattern.class).list();
    }

    private Session session() {
        return sessionFactory.getCurrentSession();
    }
}
