package org.openmrs.module.appointments.web.controller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.APIException;
import org.openmrs.module.appointments.dao.AppointmentRecurringPatternDao;
import org.openmrs.module.appointments.model.Appointment;
import org.openmrs.module.appointments.model.AppointmentProvider;
import org.openmrs.module.appointments.model.AppointmentServiceDefinition;
import org.openmrs.module.appointments.model.AppointmentServiceType;
import org.openmrs.module.appointments.model.AppointmentStatus;
import org.openmrs.module.appointments.model.RecurringPattern;
import org.openmrs.module.appointments.service.AppointmentServiceDefinitionService;
import org.openmrs.module.appointments.service.AppointmentsService;
import org.openmrs.module.appointments.service.RecurringPatternService;
import org.openmrs.module.appointments.util.DateUtil;
import org.openmrs.module.appointments.web.contract.*;
import org.openmrs.module.appointments.web.mapper.AppointmentMapper;
import org.openmrs.module.appointments.web.mapper.AppointmentServiceMapper;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.RestUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping(value = "/rest/" + RestConstants.VERSION_1 + "/appointment")
public class AppointmentController {

    private Log log = LogFactory.getLog(this.getClass());

    @Autowired
    private AppointmentsService appointmentsService;

    @Autowired
    private RecurringPatternService recurringPatternService;

    @Autowired
    private AppointmentRecurringPatternDao recurringPatternDao;

    @Autowired
    private AppointmentServiceDefinitionService appointmentServiceDefinitionService;

    @Autowired
    private AppointmentMapper appointmentMapper;

    @Autowired
    private AppointmentServiceMapper appointmentServiceMapper;

    @RequestMapping(method = RequestMethod.GET, value = "all")
    @ResponseBody
    public List<AppointmentDefaultResponse> getAllAppointments(@RequestParam(value = "forDate", required = false) String forDate) throws ParseException {
        List<Appointment> appointments = appointmentsService.getAllAppointments(DateUtil.convertToLocalDateFromUTC(forDate));
        return appointmentMapper.constructResponse(appointments);
    }

    @RequestMapping(method = RequestMethod.POST, value = "search")
    @ResponseBody
    public List<AppointmentDefaultResponse> searchAppointments(@Valid @RequestBody AppointmentQuery searchQuery) throws IOException {
        Appointment appointment = appointmentMapper.mapQueryToAppointment(searchQuery);
        if (searchQuery.getStatus() == null) {
            appointment.setStatus(null);
        }
        List<Appointment> appointments = appointmentsService.search(appointment);
        return appointmentMapper.constructResponse(appointments);
    }


    @RequestMapping(method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<Object> saveAppointment(@Valid @RequestBody AppointmentRequest appointmentRequest) throws IOException {
        try {
            if (appointmentRequest.getRecurringPattern() == null) {
                Appointment appointment = appointmentMapper.fromRequest(appointmentRequest);
                appointmentsService.validateAndSave(appointment);
                return new ResponseEntity<>(appointmentMapper.constructResponse(appointment), HttpStatus.OK);
            }
//            else {
//                Appointment appointment = appointmentMapper.fromRequest(appointmentRequest);
//                RecurringPattern recurringPattern = appointmentMapper.fromRecurrenceRequest(appointmentRequest);
//                List<Appointment> appointments = recurringPatternService.saveRecurringAppointments(appointment, recurringPattern);
//                return new ResponseEntity<>(appointmentMapper.constructResponse(appointments), HttpStatus.OK);
//            }
            else {
                RecurringPattern recurringPattern = appointmentMapper.fromRecurrenceRequest(appointmentRequest);
                List<Date> recurringDates = recurringPatternService.getRecurringDates(appointmentRequest.getStartDateTime(), recurringPattern);
                List<Appointment> appointments = generateAppointments(recurringDates, appointmentRequest);
                recurringPattern.setAppointments(new HashSet<>(appointments));
                recurringPatternDao.save(recurringPattern);

                return new ResponseEntity<>(appointmentMapper.constructResponse(recurringPattern), HttpStatus.OK);
            }
        } catch (RuntimeException e) {
            log.error("Runtime error while trying to create new appointment", e);
            return new ResponseEntity<>(RestUtil.wrapErrorResponse(e, e.getMessage()), HttpStatus.BAD_REQUEST);
        }
    }

    List<Appointment>   generateAppointments(List<Date> appointmentDates, AppointmentRequest appointmentRequest){
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            String appointmentSlotStartTime = timeFormat.format(appointmentRequest.getStartDateTime());
            String appointmentSlotEndTime = timeFormat.format(appointmentRequest.getEndDateTime());
            List<Appointment> appointments = new ArrayList<>();

            for (Date appointmentDate : appointmentDates) {
                String formattedDate = dateFormat.format(appointmentDate);
                Appointment appointment = appointmentMapper.fromRequest(appointmentRequest);
                try {
                    appointment.setStartDateTime(getFormattedDateTime(formattedDate, appointmentSlotStartTime));
                    appointment.setEndDateTime(getFormattedDateTime(formattedDate, appointmentSlotEndTime));

                } catch (ParseException e) {
                    throw new APIException(e);
                }
                appointments.add(appointmentsService.validateAndSave(appointment));
            }

            return appointments;
    }


    public Date getFormattedDateTime(String formattedDate, String time) throws ParseException {
        String TIME_FORMAT = "HH:mm:ss";
        String DATE_FORMAT = "yyyy-MM-dd";
        SimpleDateFormat dateTimeFormat = new SimpleDateFormat(DATE_FORMAT + " " + TIME_FORMAT);
        return dateTimeFormat.parse(formattedDate + " " + time);
    }

    @RequestMapping(method = RequestMethod.GET, value = "futureAppointmentsForServiceType")
    @ResponseBody
    public List<AppointmentDefaultResponse> getAllFututreAppointmentsForGivenServiceType(@RequestParam(value = "appointmentServiceTypeUuid", required = true) String serviceTypeUuid) {
        AppointmentServiceType appointmentServiceType = appointmentServiceDefinitionService.getAppointmentServiceTypeByUuid(serviceTypeUuid);
        List<Appointment> appointments = appointmentsService.getAllFutureAppointmentsForServiceType(appointmentServiceType);
        return appointmentMapper.constructResponse(appointments);
    }

    @RequestMapping(method = RequestMethod.GET, value = "appointmentSummary")
    @ResponseBody
    public List<AppointmentsSummary> getAllAppointmentsSummary(@RequestParam(value = "startDate") String startDateString, @RequestParam(value = "endDate") String endDateString) throws ParseException {
        List<AppointmentsSummary> appointmentsSummaryList = new ArrayList<>();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date startDate = DateUtil.convertToLocalDateFromUTC(startDateString);
        Date endDate = DateUtil.convertToLocalDateFromUTC(endDateString);
        List<AppointmentServiceDefinition> appointmentServiceDefinitions = appointmentServiceDefinitionService.getAllAppointmentServices(false);
        for (AppointmentServiceDefinition appointmentServiceDefinition : appointmentServiceDefinitions) {
            List<Appointment> appointmentsForService =
                    appointmentsService.getAppointmentsForService(
                            appointmentServiceDefinition, startDate, endDate,
                            Arrays.asList(
                                    AppointmentStatus.Completed,
                                    AppointmentStatus.Scheduled,
                                    AppointmentStatus.CheckedIn,
                                    AppointmentStatus.Missed));

            Map<Date, List<Appointment>> appointmentsGroupedByDate =
                    appointmentsForService.stream().collect(Collectors.groupingBy(Appointment::getDateFromStartDateTime));

            Map<String, DailyAppointmentServiceSummary> appointmentCountMap = new LinkedHashMap<>();
            for (Map.Entry<Date, List<Appointment>> appointmentDateMap : appointmentsGroupedByDate.entrySet()) {
                List<Appointment> appointments = appointmentDateMap.getValue();
                Long missedAppointmentsCount = appointments.stream().filter(s -> s.getStatus().equals(AppointmentStatus.Missed)).count();
                DailyAppointmentServiceSummary dailyAppointmentServiceSummary = new DailyAppointmentServiceSummary(
                        appointmentDateMap.getKey(), appointmentServiceDefinition.getUuid(), appointments.size(), Math.toIntExact(missedAppointmentsCount));
                appointmentCountMap.put(simpleDateFormat.format(appointmentDateMap.getKey()), dailyAppointmentServiceSummary);
            }

            AppointmentsSummary appointmentsSummary = new AppointmentsSummary(appointmentServiceMapper.constructDefaultResponse(appointmentServiceDefinition), appointmentCountMap);
            appointmentsSummaryList.add(appointmentsSummary);
        }
        return appointmentsSummaryList;
    }

    @RequestMapping(method = RequestMethod.POST, value = "/{appointmentUuid}/changeStatus")
    @ResponseBody
    public ResponseEntity<Object> transitionAppointment(@PathVariable("appointmentUuid") String appointmentUuid, @RequestBody Map<String, String> statusDetails) throws ParseException {
        try {
            String toStatus = statusDetails.get("toStatus");
            Date onDate = DateUtil.convertToLocalDateFromUTC(statusDetails.get("onDate"));
            Appointment appointment = appointmentsService.getAppointmentByUuid(appointmentUuid);
            if (appointment != null) {
                appointmentsService.changeStatus(appointment, toStatus, onDate);
                return new ResponseEntity<>(appointmentMapper.constructResponse(appointment), HttpStatus.OK);
            } else
                throw new RuntimeException("Appointment does not exist");
        } catch (RuntimeException e) {
            log.error("Runtime error while trying to update appointment status", e);
            return new ResponseEntity<>(RestUtil.wrapErrorResponse(e, e.getMessage()), HttpStatus.BAD_REQUEST);
        }
    }

    @RequestMapping(method = RequestMethod.POST, value = "undoStatusChange/{appointmentUuid}")
    @ResponseBody
    public ResponseEntity<Object> undoStatusChange(@PathVariable("appointmentUuid") String appointmentUuid) throws ParseException {
        try {
            Appointment appointment = appointmentsService.getAppointmentByUuid(appointmentUuid);
            if (appointment == null) {
                throw new RuntimeException("Appointment does not exist");
            }
            appointmentsService.undoStatusChange(appointment);
            return new ResponseEntity<>(appointmentMapper.constructResponse(appointment), HttpStatus.OK);
        } catch (RuntimeException e) {
            log.error("Runtime error while trying to undo appointment status", e);
            return new ResponseEntity<>(RestUtil.wrapErrorResponse(e, e.getMessage()), HttpStatus.BAD_REQUEST);
        }
    }

    @RequestMapping(method = RequestMethod.GET)
    @ResponseBody
    public AppointmentDefaultResponse getAppointmentByUuid(@RequestParam(value = "uuid") String uuid) {
        Appointment appointment = appointmentsService.getAppointmentByUuid(uuid);
        if (appointment == null) {
            log.error("Invalid. Appointment does not exist. UUID - " + uuid);
            throw new RuntimeException("Appointment does not exist");
        }
        return appointmentMapper.constructResponse(appointment);
    }

    @RequestMapping(method = RequestMethod.POST, value = "/{appointmentUuid}/providerResponse")
    @ResponseBody
    public ResponseEntity<Object> updateAppointmentProviderResponse(@PathVariable("appointmentUuid") String appointmentUuid, @RequestBody AppointmentProviderDetail providerResponse) throws ParseException {
        try {
            Appointment appointment = appointmentsService.getAppointmentByUuid(appointmentUuid);
            if (appointment == null) {
                throw new RuntimeException("Appointment does not exist");
            }
            AppointmentProvider appointmentProviderProvider = appointmentMapper.mapAppointmentProvider(providerResponse);
            appointmentsService.updateAppointmentProviderResponse(appointmentProviderProvider);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (RuntimeException e) {
            log.error("Runtime error while trying to update appointment provider response", e);
            return new ResponseEntity<>(RestUtil.wrapErrorResponse(e, e.getMessage()), HttpStatus.BAD_REQUEST);
        }
    }

    //TODO write test
    @RequestMapping(method = RequestMethod.POST, value = "/{uuid}/reschedule")
    @ResponseBody
    public ResponseEntity<Object> rescheduleAppointment(@PathVariable("uuid") String prevAppointmentUuid,
                                                        @Valid @RequestBody AppointmentRequest appointmentRequest,
                                                        @RequestParam(value = "retainNumber", required = false, defaultValue = "false") boolean retainAppointmentNumber)
            throws ParseException {
        try {
            appointmentRequest.setUuid(null);
            Appointment appointment = appointmentMapper.fromRequest(appointmentRequest);
            Appointment rescheduledAppointment = appointmentsService.reschedule(prevAppointmentUuid, appointment, false);
            return new ResponseEntity<>(appointmentMapper.constructResponse(rescheduledAppointment), HttpStatus.OK);
        } catch (RuntimeException e) {
            log.error("Runtime error while trying to create new appointment", e);
            return new ResponseEntity<>(RestUtil.wrapErrorResponse(e, e.getMessage()), HttpStatus.BAD_REQUEST);
        }
    }

}
