package org.openmrs.module.appointments.service.impl;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import org.openmrs.Patient;
import org.openmrs.api.APIException;
import org.openmrs.module.appointments.dao.AppointmentAuditDao;
import org.openmrs.module.appointments.dao.AppointmentDao;
import org.openmrs.module.appointments.helper.AppointmentServiceHelper;
import org.openmrs.module.appointments.model.Appointment;
import org.openmrs.module.appointments.model.AppointmentAudit;
import org.openmrs.module.appointments.model.AppointmentKind;
import org.openmrs.module.appointments.model.AppointmentServiceDefinition;
import org.openmrs.module.appointments.model.AppointmentServiceType;
import org.openmrs.module.appointments.model.AppointmentStatus;
import org.openmrs.module.appointments.util.DateUtil;
import org.openmrs.module.appointments.validator.AppointmentStatusChangeValidator;
import org.openmrs.module.appointments.validator.AppointmentValidator;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AppointmentsServiceImplTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Mock
    private AppointmentDao appointmentDao;

    @Mock
    private AppointmentValidator appointmentValidator;

    @Mock
    private AppointmentStatusChangeValidator statusChangeValidator;

    @Spy
    private List<AppointmentStatusChangeValidator> statusChangeValidators = new ArrayList<>();

    @Spy
    private List<AppointmentValidator> appointmentValidators = new ArrayList<>();

    @Mock
    private AppointmentServiceHelper appointmentServiceHelper;

    @Mock
    private AppointmentAuditDao appointmentAuditDao;

    @InjectMocks
    private AppointmentsServiceImpl appointmentsService;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);

        appointmentValidators.add(appointmentValidator);
        statusChangeValidators.add(statusChangeValidator);
    }

    @Test
    public void testCreateAppointment() throws Exception {
        Appointment appointment = new Appointment();
        appointment.setPatient(new Patient());
        appointment.setService(new AppointmentServiceDefinition());
        appointment.setStartDateTime(new Date());
        appointment.setEndDateTime(new Date());
        appointment.setAppointmentKind(AppointmentKind.Scheduled);
        appointmentsService.validateAndSave(appointment);
        verify(appointmentDao, times(1)).save(appointment);
    }

    @Test
    public void shouldCallCreateAuditEventOnSaveOfAppointment() throws IOException {
        Appointment appointment = new Appointment();
        String notes = "";
        AppointmentAudit appointmentAuditMock = mock(AppointmentAudit.class);
        when(appointmentServiceHelper.getAppointmentAuditEvent(appointment, notes))
                .thenReturn(appointmentAuditMock);
        when(appointmentServiceHelper.getAppointmentAsJsonString(appointment)).thenReturn(notes);

        appointmentsService.validateAndSave(appointment);

        verify(appointmentServiceHelper, times(1)).getAppointmentAuditEvent(appointment, notes);
        verify(appointmentAuditDao, times(1)).save(appointmentAuditMock);
    }

    @Test
    public void testGetAllAppointments() throws Exception {
        appointmentsService.getAllAppointments(null);
        verify(appointmentDao, times(1)).getAllAppointments(null);
    }

    @Test
    public void shouldNotGetAppointmentsWithVoidedService() throws Exception {
        List<Appointment> appointments = new ArrayList<>();
        Appointment appointment1 = new Appointment();
        AppointmentServiceDefinition appointmentServiceDefinition1 = new AppointmentServiceDefinition();
        appointmentServiceDefinition1.setVoided(true);
        appointment1.setService(appointmentServiceDefinition1);
        Appointment appointment2 = new Appointment();
        appointment2.setService(new AppointmentServiceDefinition());
        appointments.add(appointment1);
        appointments.add(appointment2);
        when(appointmentDao.getAllAppointments(null)).thenReturn(appointments);
        List<Appointment> appointmentList = appointmentsService.getAllAppointments(null);
        verify(appointmentDao, times(1)).getAllAppointments(null);
        assertEquals(appointmentList.size(), 1);
    }
    
    @Test
    public void shouldNotGetAppointmentsWithVoidedServiceType() throws Exception {
        List<Appointment> appointments = new ArrayList<>();
        Appointment appointment1 = new Appointment();
        AppointmentServiceDefinition appointmentServiceDefinition1 = new AppointmentServiceDefinition();
        AppointmentServiceType appointmentServiceType1 = new AppointmentServiceType();
        appointmentServiceType1.setVoided(true);
        appointmentServiceDefinition1.setServiceTypes(Collections.singleton(appointmentServiceType1));
        appointment1.setService(appointmentServiceDefinition1);
        appointment1.setServiceType(appointmentServiceType1);
        appointments.add(appointment1);
        Appointment appointment2 = new Appointment();
        appointment2.setService(new AppointmentServiceDefinition());
        appointments.add(appointment2);
        when(appointmentDao.getAllAppointments(null)).thenReturn(appointments);
        List<Appointment> appointmentList = appointmentsService.getAllAppointments(null);
        verify(appointmentDao, times(1)).getAllAppointments(null);
        assertEquals(appointmentList.size(), 1);
    }
    
    @Test
    public void shouldGetAllFutureAppointmentsForTheGivenAppointmentService() throws Exception {
        AppointmentServiceDefinition appointmentServiceDefinition = new AppointmentServiceDefinition();
        appointmentServiceDefinition.setUuid("uuid");
        when(appointmentDao.getAllFutureAppointmentsForService(appointmentServiceDefinition)).thenReturn(new ArrayList<>());

        appointmentsService.getAllFutureAppointmentsForService(appointmentServiceDefinition);

        verify(appointmentDao, times(1)).getAllFutureAppointmentsForService(appointmentServiceDefinition);
    }

    @Test
    public void shouldGetAllFutureAppointmentsForTheGivenAppointmentServiceType() throws Exception {
        AppointmentServiceType appointmentServiceType = new AppointmentServiceType();
        appointmentServiceType.setUuid("typeUuid");
        when(appointmentDao.getAllFutureAppointmentsForServiceType(appointmentServiceType)).thenReturn(new ArrayList<>());

        appointmentsService.getAllFutureAppointmentsForServiceType(appointmentServiceType);

        verify(appointmentDao, times(1)).getAllFutureAppointmentsForServiceType(appointmentServiceType);
    }

    @Test
    public void shouldGetAppointmentsForAService() throws ParseException {
        AppointmentServiceDefinition appointmentServiceDefinition = new AppointmentServiceDefinition();
        appointmentServiceDefinition.setUuid("uuid");
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date startDate = simpleDateFormat.parse("2017-08-08");
        Date endDate = simpleDateFormat.parse("2017-08-09");

        List<Appointment> appointments = new ArrayList<>();
        Appointment appointment = new Appointment();
        appointment.setId(2);
        appointment.setUuid("someUuid");

        when(appointmentDao.getAppointmentsForService(appointmentServiceDefinition, startDate, endDate, null)).thenReturn(appointments);

        appointmentsService.getAppointmentsForService(appointmentServiceDefinition, startDate, endDate, null);
        verify(appointmentDao, times(1)).getAppointmentsForService(appointmentServiceDefinition, startDate, endDate, null);
    }

    @Test
    public void shouldSearchForAnAppointment() {
        Appointment appointment = new Appointment();
        appointment.setUuid("Uuid");
        List<Appointment> appointmentList = new ArrayList<>();
        appointmentList.add(appointment);
        when(appointmentDao.search(appointment)).thenReturn(appointmentList);
        appointmentsService.search(appointment);
        verify(appointmentDao, times(1)).search(appointment);
    }

    @Test
    public void shouldThrowExceptionIfValidationFailsOnAppointmentSave() {
        String errorMessage = "Appointment cannot be created without Patient";
        doAnswer(invocation -> {
            Object[] args = invocation.getArguments();
            List<String> errors = (List) args[2];
            errors.add(errorMessage);
            return null;
        }).when(appointmentServiceHelper).validate(any(Appointment.class), anyListOf(AppointmentValidator.class),
                anyListOf(String.class));

        expectedException.expect(APIException.class);
        expectedException.expectMessage(errorMessage);
        appointmentsService.validateAndSave(new Appointment());
        verify(appointmentDao, never()).save(any(Appointment.class));
    }

    @Test
    public void shouldRunDefaultValidatorOnStatusChange(){
        Appointment appointment = new Appointment();
        appointment.setStatus(AppointmentStatus.Scheduled);
        appointmentsService.changeStatus(appointment, "CheckedIn", null);
        verify(statusChangeValidator, times(1)).validate(any(Appointment.class), any(AppointmentStatus.class), anyListOf(String.class));
    }

    @Test
    public void shouldThrowExceptionIfValidationFailsOnStatusChange(){
        String errorMessage = "Appointment status cannot be changed from Completed to Missed";
        doAnswer(invocation -> {
            Object[] args = invocation.getArguments();
            List<String> errors = (List) args[2];
            errors.add(errorMessage);
            return null;
        }).when(statusChangeValidator).validate(any(Appointment.class), any(AppointmentStatus.class), anyListOf(String.class));

        Appointment appointment = new Appointment();
        appointment.setStatus(AppointmentStatus.Completed);
        expectedException.expect(APIException.class);
        expectedException.expectMessage(errorMessage);
        appointmentsService.changeStatus(appointment, "Missed", null);
        verify(appointmentAuditDao, never()).save(any(AppointmentAudit.class));
    }

    @Test
    public void shouldCreateAuditEventOnStatusChangeWithOutDate() {
        Appointment appointment = new Appointment();
        appointment.setStatus(AppointmentStatus.Scheduled);
        AppointmentAudit appointmentAudit = new AppointmentAudit();
        appointmentAudit.setNotes(null);
        appointmentAudit.setAppointment(appointment);
        appointmentAudit.setStatus(AppointmentStatus.CheckedIn);
        when(appointmentServiceHelper.getAppointmentAuditEvent(appointment, null)).thenReturn(appointmentAudit);

        appointmentsService.changeStatus(appointment, "CheckedIn", null);

        verify(appointmentServiceHelper).getAppointmentAuditEvent(appointment, null);
        verify(appointmentAuditDao, times(1)).save(appointmentAudit);
    }

    @Test
    public void shouldCreateAuditEventOnStatusChangeWithDateAsNotes() throws ParseException {
        Date onDate = DateUtil.convertToDate("2108-08-15T00:00:00.0Z", DateUtil.DateFormatType.UTC);
        Appointment appointment = new Appointment();
        appointment.setStatus(AppointmentStatus.Scheduled);
        AppointmentAudit appointmentAudit = mock(AppointmentAudit.class);
        String notes = onDate.toInstant().toString();
        when(appointmentServiceHelper.getAppointmentAuditEvent(appointment, notes)).thenReturn(appointmentAudit);

        appointmentsService.changeStatus(appointment, "CheckedIn", onDate);

        verify(appointmentServiceHelper).getAppointmentAuditEvent(appointment, notes);
        verify(appointmentAuditDao, times(1)).save(appointmentAudit);
    }

    @Test
    public void shouldCallAppointmentDaoOnce() {
        appointmentsService.getAllAppointmentsInDateRange(null, null);
        verify(appointmentDao, times(1)).getAllAppointmentsInDateRange(null, null);
    }

    @Test
    public void shouldGetAllNonVoidedAppointmentsForAGivenDateRange() throws ParseException {
        List<Appointment> appointments = new ArrayList<>();
        Appointment appointment1 = new Appointment();
        AppointmentServiceDefinition appointmentServiceDefinition1 = new AppointmentServiceDefinition();
        appointmentServiceDefinition1.setVoided(true);
        appointment1.setService(appointmentServiceDefinition1);
        LocalDate yesterday = LocalDate.now().minusDays(1);
        appointment1.setStartDateTime(new SimpleDateFormat("yyyy-MM-dd").parse(yesterday.toString()));
        Appointment appointment2 = new Appointment();
        appointment2.setService(new AppointmentServiceDefinition());
        appointment1.setStartDateTime(new Date());
        appointments.add(appointment1);
        appointments.add(appointment2);
        when(appointmentDao.getAllAppointmentsInDateRange(null, null)).thenReturn(appointments);
        List<Appointment> appointmentList = appointmentsService.getAllAppointmentsInDateRange(null, null);
        verify(appointmentDao, times(1)).getAllAppointmentsInDateRange(null, null);
        assertEquals(appointmentList.size(), 1);
    }

    @Test
    public void shouldGetAppointmentByUuid() {
        String appointmentUuid = "appointmentUuid";
        appointmentsService.getAppointmentByUuid(appointmentUuid);
        verify(appointmentDao, times(1)).getAppointmentByUuid(appointmentUuid);
    }

    @Test
    public void shouldUndoStatusChange() throws ParseException {
        Appointment appointment = new Appointment();
        appointment.setStatus(AppointmentStatus.Completed);
        AppointmentAudit appointmentAudit = new AppointmentAudit();
        appointmentAudit.setAppointment(appointment);
        appointmentAudit.setStatus(AppointmentStatus.CheckedIn);
        appointmentAudit.setNotes("2108-08-15T11:30:00.0Z");
        when(appointmentAuditDao.getPriorStatusChangeEvent(appointment)).thenReturn(appointmentAudit);
        appointmentsService.undoStatusChange(appointment);
        verify(appointmentAuditDao, times(1)).getPriorStatusChangeEvent(appointment);
        ArgumentCaptor<Appointment> captor = ArgumentCaptor.forClass(Appointment.class);
        ;
        verify(appointmentDao, times(1)).save(captor.capture());
        assertEquals(appointmentAudit.getStatus(), captor.getValue().getStatus());
    }

    @Test
    public void shouldCallStatusChangeAuditEventOnUndoStatusChange() {
        Appointment appointment = new Appointment();
        appointment.setStatus(AppointmentStatus.Completed);

        AppointmentAudit appointmentAudit = new AppointmentAudit();
        appointmentAudit.setAppointment(appointment);
        appointmentAudit.setStatus(AppointmentStatus.CheckedIn);
        String notes = "2108-08-15T11:30:00.0Z";
        appointmentAudit.setNotes(notes);
        when(appointmentAuditDao.getPriorStatusChangeEvent(appointment)).thenReturn(appointmentAudit);
        when(appointmentServiceHelper.getAppointmentAuditEvent(appointment, appointmentAudit.getNotes()))
                .thenReturn(appointmentAudit);
        appointmentsService.undoStatusChange(appointment);
        ArgumentCaptor<AppointmentAudit> captor = ArgumentCaptor.forClass(AppointmentAudit.class);
        verify(appointmentAuditDao, times(1)).save(captor.capture());
        verify(appointmentServiceHelper).getAppointmentAuditEvent(appointment, notes);
        AppointmentAudit savedEvent = captor.getValue();
        assertEquals(appointmentAudit.getNotes(), savedEvent.getNotes());
        assertEquals(appointment.getStatus(), savedEvent.getStatus());
        assertEquals(appointment, savedEvent.getAppointment());
    }

    @Test
    public void shouldThrowExceptionWhenThereIsNoPriorStatusChangeExists() throws ParseException {
        Appointment appointment = new Appointment();
        appointment.setStatus(AppointmentStatus.Scheduled);
        when(appointmentAuditDao.getPriorStatusChangeEvent(appointment)).thenReturn(null);
        expectedException.expect(APIException.class);
        expectedException.expectMessage("No status change actions to undo");
        appointmentsService.undoStatusChange(appointment);
        verify(appointmentAuditDao, times(0)).getPriorStatusChangeEvent(appointment);
        verify(appointmentDao, times(0)).save(appointment);
    }

}
