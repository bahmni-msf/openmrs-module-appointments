package org.openmrs.module.appointments.web.mapper;

import org.apache.commons.lang.StringUtils;
import org.openmrs.Location;
import org.openmrs.Patient;
import org.openmrs.Provider;
import org.openmrs.api.LocationService;
import org.openmrs.api.PatientService;
import org.openmrs.api.ProviderService;
import org.openmrs.module.appointments.model.Appointment;
import org.openmrs.module.appointments.model.AppointmentKind;
import org.openmrs.module.appointments.model.AppointmentProvider;
import org.openmrs.module.appointments.model.AppointmentProviderResponse;
import org.openmrs.module.appointments.model.AppointmentRecurringPattern;
import org.openmrs.module.appointments.model.AppointmentServiceDefinition;
import org.openmrs.module.appointments.model.AppointmentServiceType;
import org.openmrs.module.appointments.model.AppointmentStatus;
import org.openmrs.module.appointments.service.AppointmentServiceDefinitionService;
import org.openmrs.module.appointments.service.AppointmentsService;
import org.openmrs.module.appointments.web.contract.AppointmentDefaultResponse;
import org.openmrs.module.appointments.web.contract.AppointmentProviderDetail;
import org.openmrs.module.appointments.web.contract.AppointmentQuery;
import org.openmrs.module.appointments.web.contract.AppointmentRequest;
import org.openmrs.module.appointments.web.contract.RecurringPattern;
import org.openmrs.module.appointments.web.extension.AppointmentResponseExtension;
import org.openmrs.module.webservices.rest.web.response.ConversionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

import static org.openmrs.module.appointments.service.impl.RecurringAppointmentType.DAY;
import static org.openmrs.module.appointments.service.impl.RecurringAppointmentType.WEEK;
import static org.openmrs.module.appointments.service.impl.RecurringAppointmentType.valueOf;

@Component
public class AppointmentMapper {
    @Autowired
    LocationService locationService;

    @Autowired
    ProviderService providerService;

    @Autowired
    PatientService patientService;

    @Autowired
    AppointmentServiceDefinitionService appointmentServiceDefinitionService;

    @Autowired
    AppointmentServiceMapper appointmentServiceMapper;

    @Autowired
    AppointmentsService appointmentsService;

    @Autowired(required = false)
    AppointmentResponseExtension appointmentResponseExtension;

    public List<AppointmentDefaultResponse> constructResponse(List<Appointment> appointments) {
        return appointments.stream().map(as -> this.mapToDefaultResponse(as, new AppointmentDefaultResponse())).collect(Collectors.toList());
    }

    public AppointmentDefaultResponse constructResponse(Appointment appointment) {
        return this.mapToDefaultResponse(appointment, new AppointmentDefaultResponse());
    }

    public Appointment fromRequest(AppointmentRequest appointmentRequest) {
        Appointment appointment;
        if (!StringUtils.isBlank(appointmentRequest.getUuid())) {
            appointment = appointmentsService.getAppointmentByUuid(appointmentRequest.getUuid());
        } else {
            appointment = new Appointment();
            appointment.setPatient(patientService.getPatientByUuid(appointmentRequest.getPatientUuid()));
        }
        AppointmentServiceDefinition appointmentServiceDefinition = appointmentServiceDefinitionService.getAppointmentServiceByUuid(appointmentRequest.getServiceUuid());
        AppointmentServiceType appointmentServiceType = null;
        if(appointmentRequest.getServiceTypeUuid() != null) {
            appointmentServiceType = getServiceTypeByUuid(appointmentServiceDefinition.getServiceTypes(true), appointmentRequest.getServiceTypeUuid());
        }
        appointment.setServiceType(appointmentServiceType);
        appointment.setService(appointmentServiceDefinition);
        //appointment.setProvider(identifyAppointmentProvider(appointmentRequest.getProviderUuid()));
        appointment.setLocation(identifyAppointmentLocation(appointmentRequest.getLocationUuid()));
        appointment.setStartDateTime(appointmentRequest.getStartDateTime());
        appointment.setEndDateTime(appointmentRequest.getEndDateTime());
        appointment.setAppointmentKind(AppointmentKind.valueOf(appointmentRequest.getAppointmentKind()));
        appointment.setComments(appointmentRequest.getComments());
        mapProvidersForAppointment(appointment, appointmentRequest.getProviders());
        return appointment;
    }

    public AppointmentRecurringPattern fromRequestRecurringPattern(RecurringPattern recurringPattern) {
        AppointmentRecurringPattern appointmentRecurringPattern = new AppointmentRecurringPattern();
        appointmentRecurringPattern.setEndDate(recurringPattern.getEndDate());
        appointmentRecurringPattern.setPeriod(recurringPattern.getPeriod());
        appointmentRecurringPattern.setFrequency(recurringPattern.getFrequency());
        String recurringPatternType = recurringPattern.getType();
        if (recurringPatternType == null) {
            throw new IllegalArgumentException(String
                    .format("Valid recurrence type should be provided. Valid types are %s and %s",  DAY, WEEK));
        }
        appointmentRecurringPattern.setType(valueOf(recurringPatternType.toUpperCase()));
        if (appointmentRecurringPattern.getType() == WEEK) {
            appointmentRecurringPattern.setDaysOfWeek(recurringPattern.getDaysOfWeek().stream().map(String::toUpperCase)
                    .collect(Collectors.joining(",")));
        }
        return appointmentRecurringPattern;
    }

    private Provider identifyAppointmentProvider(String providerUuid) {
        return providerService.getProviderByUuid(providerUuid);
    }

    private Location identifyAppointmentLocation(String locationUuid) {
        return locationService.getLocationByUuid(locationUuid);
    }

    private void mapProvidersForAppointment(Appointment appointment, List<AppointmentProviderDetail> newProviders) {
        Set<AppointmentProvider> existingProviders = appointment.getProviders();

        if (existingProviders != null) {
            for (AppointmentProvider appointmentProvider : existingProviders) {
                boolean exists = newProviders == null ? false :
                        newProviders.stream().anyMatch(p -> p.getUuid().equals(appointmentProvider.getProvider().getUuid()));
                if (!exists) {
                    appointmentProvider.setResponse(AppointmentProviderResponse.CANCELLED);
                    appointmentProvider.setVoided(true);
                    appointmentProvider.setVoidReason(AppointmentProviderResponse.CANCELLED.toString());
                }
            }
        }

        if (newProviders != null && !newProviders.isEmpty()) {
            if (appointment.getProviders() == null ) {
                appointment.setProviders(new HashSet<>());
            }
            for (AppointmentProviderDetail providerDetail : newProviders) {
                List<AppointmentProvider> providers = appointment.getProviders().stream().filter(p -> p.getProvider().getUuid().equals(providerDetail.getUuid())).collect(Collectors.toList());
                if (providers.isEmpty()) {
                    AppointmentProvider newAppointmentProvider = createNewAppointmentProvider(providerDetail);
                    newAppointmentProvider.setAppointment(appointment);
                    appointment.getProviders().add(newAppointmentProvider);
                } else {
                    providers.forEach(existingAppointmentProvider -> {
                        //TODO: if currentUser is same person as provider, set ACCEPTED
                        existingAppointmentProvider.setResponse(mapProviderResponse(providerDetail.getResponse()));
                    });
                }
            }
        }

    }

    private AppointmentProvider createNewAppointmentProvider(AppointmentProviderDetail providerDetail) {
        Provider provider = identifyAppointmentProvider(providerDetail.getUuid());
        if (provider == null) {
            throw new ConversionException("Bad Request. No such provider.");
        }
        AppointmentProvider appointmentProvider = new AppointmentProvider();
        appointmentProvider.setProvider(provider);
        appointmentProvider.setResponse(mapProviderResponse(providerDetail.getResponse()));
        appointmentProvider.setComments(providerDetail.getComments());
        return appointmentProvider;
    }

    public AppointmentProviderResponse mapProviderResponse(String response) {
        String namedEnum = StringUtils.isEmpty(response) ? AppointmentProviderResponse.ACCEPTED.toString()  : response.toUpperCase();
        //TODO: validation if not valid enum string
        return AppointmentProviderResponse.valueOf(namedEnum);
    }


    private AppointmentServiceType getServiceTypeByUuid(Set<AppointmentServiceType> serviceTypes, String serviceTypeUuid) {
        return serviceTypes.stream()
                .filter(avb -> avb.getUuid().equals(serviceTypeUuid)).findAny().get();
    }

    public Appointment mapQueryToAppointment(AppointmentQuery searchQuery) {
        Appointment appointment = new Appointment();
        appointment.setService(
                appointmentServiceDefinitionService.getAppointmentServiceByUuid(searchQuery.getServiceUuid()));
        appointment.setPatient(patientService.getPatientByUuid(searchQuery.getPatientUuid()));
        appointment.setProvider(identifyAppointmentProvider(searchQuery.getProviderUuid()));
        appointment.setLocation(identifyAppointmentLocation(searchQuery.getLocationUuid()));
        if (searchQuery.getStatus() != null) {
            appointment.setStatus(AppointmentStatus.valueOf(searchQuery.getStatus()));
        }
        return appointment;
    }

    private AppointmentDefaultResponse mapToDefaultResponse(Appointment appointment, AppointmentDefaultResponse appointmentDefaultResponse) {
        appointmentDefaultResponse.setUuid(appointment.getUuid());
        appointmentDefaultResponse.setAppointmentNumber(appointment.getAppointmentNumber());
        appointmentDefaultResponse.setPatient(createPatientMap(appointment.getPatient()));
        appointmentDefaultResponse.setService(appointmentServiceMapper.constructDefaultResponse(appointment.getService()));
        appointmentDefaultResponse.setServiceType(createServiceTypeMap(appointment.getServiceType()));
        //appointmentDefaultResponse.setProvider(createProviderMap(appointment.getProvider()));
        appointmentDefaultResponse.setLocation(createLocationMap(appointment.getLocation()));
        appointmentDefaultResponse.setStartDateTime(appointment.getStartDateTime());
        appointmentDefaultResponse.setEndDateTime(appointment.getEndDateTime());
        appointmentDefaultResponse.setAppointmentKind(appointment.getAppointmentKind().name());
        appointmentDefaultResponse.setStatus(appointment.getStatus().name());
        appointmentDefaultResponse.setComments(appointment.getComments());
        if (appointmentResponseExtension != null)
            appointmentDefaultResponse.setAdditionalInfo(appointmentResponseExtension.run(appointment));
        appointmentDefaultResponse.setProviders(mapAppointmentProviders(appointment.getProviders()));
        appointmentDefaultResponse.setRecurringPattern(mapRecurringPattern(appointment, appointment.getAppointmentRecurringPattern()));
        return appointmentDefaultResponse;
    }

    private RecurringPattern mapRecurringPattern(
            Appointment appointment, AppointmentRecurringPattern appointmentRecurringPattern) {
        if(appointmentRecurringPattern == null) {
            return null;
        }
        RecurringPattern recurringPattern = new RecurringPattern();
        recurringPattern.setType(appointmentRecurringPattern.getType().toString());
        recurringPattern.setPeriod(appointmentRecurringPattern.getPeriod());
        Date endDate = appointmentRecurringPattern.getEndDate();
        if (endDate != null) {
            recurringPattern.setEndDate(endDate);
        } else {
            recurringPattern.setFrequency(getPendingOccurrences(appointment));
        }
        ;
        if (appointmentRecurringPattern.getDaysOfWeek() != null) {
            recurringPattern.setDaysOfWeek(Arrays.asList(appointmentRecurringPattern
                    .getDaysOfWeek().split(",")));
        }
        return recurringPattern;
    }

    private int getPendingOccurrences(Appointment appointment) {
        return Math.toIntExact(
                appointment.getAppointmentRecurringPattern().getAppointments()
                        .stream()
                        .filter(appointmentInList -> appointmentInList.getStartDateTime()
                                .compareTo(appointment.getStartDateTime()) >= 0
                                && appointmentInList.getStatus() == AppointmentStatus.Scheduled)
                        .count());
    }

    private List<AppointmentProviderDetail> mapAppointmentProviders(Set<AppointmentProvider> providers) {
        List<AppointmentProviderDetail> providerList = new ArrayList<>();
        if (providers != null) {
            for (AppointmentProvider apptProviderAssociation : providers) {
                AppointmentProviderDetail providerDetail = new AppointmentProviderDetail();
                providerDetail.setUuid(apptProviderAssociation.getProvider().getUuid());
                providerDetail.setComments(apptProviderAssociation.getComments());
                providerDetail.setResponse(apptProviderAssociation.getResponse().toString());
                providerDetail.setName(apptProviderAssociation.getProvider().getName());
                providerList.add(providerDetail);
            }
        }
        return providerList;
    }

    private Map createServiceTypeMap(AppointmentServiceType s) {
        Map serviceTypeMap = null;
        if (s != null) {
            serviceTypeMap = new HashMap();
            serviceTypeMap.put("name", s.getName());
            serviceTypeMap.put("uuid", s.getUuid());
            serviceTypeMap.put("duration", s.getDuration());
        }
        return serviceTypeMap;
    }

    private Map createLocationMap(Location l) {
        Map locationMap = null;
        if (l != null) {
            locationMap = new HashMap();
            locationMap.put("name", l.getName());
            locationMap.put("uuid", l.getUuid());
        }
        return locationMap;
    }

    private Map createPatientMap(Patient p) {
        Map map = new HashMap();
        map.put("name", p.getPersonName().getFullName());
        map.put("uuid", p.getUuid());
        map.put("identifier", p.getPatientIdentifier().getIdentifier());
        return map;
    }

    public AppointmentProvider mapAppointmentProvider(AppointmentProviderDetail providerDetail) {
        AppointmentProvider appointmentProvider = new AppointmentProvider();
        appointmentProvider.setProvider(identifyAppointmentProvider(providerDetail.getUuid()));
        appointmentProvider.setResponse(mapProviderResponse(providerDetail.getResponse()));
        appointmentProvider.setComments(providerDetail.getComments());
        return appointmentProvider;
    }
}
