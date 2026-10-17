
package com.hospital.booking.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.hospital.booking.dao.Appointment;
import com.hospital.booking.dao.AppointmentStatus;
import com.hospital.booking.dao.Doctor;
import com.hospital.booking.dao.Patient;
import com.hospital.booking.dto.AppointmentRequest;
import com.hospital.booking.repository.AppointmentRepository;
import com.hospital.booking.repository.DoctorRepository;
import com.hospital.booking.repository.PatientRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import com.hospital.booking.dao.AppointmentStatus;


@Service
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;

    public AppointmentService(
            AppointmentRepository appointmentRepository,
            PatientRepository patientRepository,
            DoctorRepository doctorRepository) {

        this.appointmentRepository = appointmentRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
    }

    // Book an appointment
    public Appointment bookAppointment(AppointmentRequest request) {

        Patient patient = patientRepository.findById(request.getPatientId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Patient not found"));

        Doctor doctor = doctorRepository.findById(request.getDoctorId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Doctor not found"));

        validateAppointmentDateTime(
                request.getAppointmentDate(),
                request.getAppointmentTime());

        checkAppointmentConflict(
                doctor.getId(),
                request.getAppointmentDate(),
                request.getAppointmentTime(),
                null);

        checkPatientAppointmentConflict(
                patient.getId(),
                request.getAppointmentDate(),
                request.getAppointmentTime(),
                null);

        Appointment appointment = new Appointment();
        appointment.setPatient(patient);
        appointment.setDoctor(doctor);
        appointment.setAppointmentDate(request.getAppointmentDate());
        appointment.setAppointmentTime(request.getAppointmentTime());
        appointment.setReason(request.getReason());
        appointment.setStatus(AppointmentStatus.BOOKED);

        return appointmentRepository.save(appointment);
    }

 // Get appointments for the logged-in user
    public List<Appointment> getMyAppointments(
            String username, String role) {

        if ("PATIENT".equals(role)) {
            Patient patient = patientRepository
                    .findByAppUser_Username(username)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Patient profile is not linked to this account"));

            return appointmentRepository.findByPatient_Id(patient.getId());
        }

        if ("DOCTOR".equals(role)) {
            Doctor doctor = doctorRepository
                    .findByAppUser_Username(username)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Doctor profile is not linked to this account"));

            return appointmentRepository.findByDoctor_Id(doctor.getId());
        }

        if ("ADMIN".equals(role)) {
            return appointmentRepository.findAll();
        }

        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN, "Access denied");
    }
    
    // Get all appointments
    public List<Appointment> getAllAppointments() {
        return appointmentRepository.findAll();
    }

    // Get all appointments with pagination
    public Page<Appointment> getAllAppointments(Pageable pageable) {
        return appointmentRepository.findAll(pageable);
    }

    // Get appointment by ID
    public Appointment getAppointmentById(Long id) {
        return appointmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Appointment not found"));
    }

    // Get appointments by patient
    public List<Appointment> getAppointmentsByPatient(Long patientId) {

        if (!patientRepository.existsById(patientId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Patient not found");
        }

        return appointmentRepository.findByPatient_Id(patientId);
    }

    // Get appointments by doctor
    public List<Appointment> getAppointmentsByDoctor(Long doctorId) {

        if (!doctorRepository.existsById(doctorId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Doctor not found");
        }

        return appointmentRepository.findByDoctor_Id(doctorId);
    }

    // Update appointment details
    public Appointment updateAppointment(
            Long id, AppointmentRequest request) {

        Appointment appointment = getAppointmentById(id);

        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cancelled appointment cannot be updated");
        }

        if (appointment.getStatus() == AppointmentStatus.COMPLETED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Completed appointment cannot be updated");
        }

        Patient patient = patientRepository.findById(request.getPatientId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Patient not found"));

        Doctor doctor = doctorRepository.findById(request.getDoctorId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Doctor not found"));

        validateAppointmentDateTime(
                request.getAppointmentDate(),
                request.getAppointmentTime());

        checkPatientAppointmentConflict(
                patient.getId(),
                request.getAppointmentDate(),
                request.getAppointmentTime(),
                appointment.getId());

        checkAppointmentConflict(
                doctor.getId(),
                request.getAppointmentDate(),
                request.getAppointmentTime(),
                appointment.getId());

        appointment.setPatient(patient);
        appointment.setDoctor(doctor);
        appointment.setAppointmentDate(request.getAppointmentDate());
        appointment.setAppointmentTime(request.getAppointmentTime());
        appointment.setReason(request.getReason());

        return appointmentRepository.save(appointment);
    }

    // Cancel appointment
    public Appointment cancelAppointment(Long id) {

        Appointment appointment = getAppointmentById(id);

        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Appointment is already cancelled");
        }

        if (appointment.getStatus() == AppointmentStatus.COMPLETED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Completed appointment cannot be cancelled");
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);

        return appointmentRepository.save(appointment);
    }

    // Get booked appointments
    public List<Appointment> getBookedAppointments() {
        return appointmentRepository.findByStatus(
                AppointmentStatus.BOOKED);
    }

    // Get appointments by date
    public List<Appointment> getAppointmentsByDate(LocalDate date) {

        if (date == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Appointment date is required");
        }

        return appointmentRepository.findByAppointmentDate(date);
    }

    // Get appointments by doctor and date
    public List<Appointment> getAppointmentsByDoctorAndDate(
            Long doctorId, LocalDate date) {

        if (!doctorRepository.existsById(doctorId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Doctor not found");
        }

        if (date == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Appointment date is required");
        }

        return appointmentRepository.findByDoctor_IdAndAppointmentDate(
                doctorId, date);
    }

    // Get appointments by status with pagination
    public Page<Appointment> getAppointmentsByStatus(
            String status, Pageable pageable) {

        if (status == null || status.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Appointment status is required");
        }

        AppointmentStatus appointmentStatus;

        try {
            appointmentStatus = AppointmentStatus.valueOf(
                    status.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid status. Allowed values: BOOKED, CANCELLED, COMPLETED");
        }

        return appointmentRepository.findByStatus(
                appointmentStatus, pageable);
    }

    // Mark appointment as completed
    public Appointment completeAppointment(Long id) {

        Appointment appointment = getAppointmentById(id);

        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cancelled appointment cannot be completed");
        }

        if (appointment.getStatus() == AppointmentStatus.COMPLETED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Appointment is already completed");
        }

        appointment.setStatus(AppointmentStatus.COMPLETED);

        return appointmentRepository.save(appointment);
    }

    // Validate appointment date and time
    private void validateAppointmentDateTime(
            LocalDate date, LocalTime time) {

        if (date == null || time == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Appointment date and time are required");
        }

        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        if (date.isBefore(today)
                || (date.equals(today) && time.isBefore(now))) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cannot book an appointment in the past.");
        }
    }

    // Check doctor booking conflict
    private void checkAppointmentConflict(
            Long doctorId,
            LocalDate appointmentDate,
            LocalTime appointmentTime,
            Long excludeAppointmentId) {

        List<Appointment> appointments =
                appointmentRepository.findByDoctor_IdAndAppointmentDate(
                        doctorId, appointmentDate);

        boolean conflict = appointments.stream().anyMatch(existing ->
                existing.getAppointmentTime().equals(appointmentTime)
                        && existing.getStatus() == AppointmentStatus.BOOKED
                        && !existing.getId().equals(excludeAppointmentId));

        if (conflict) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This doctor already has a booked appointment at this date and time.");
        }
    }

    // Check patient booking conflict
    private void checkPatientAppointmentConflict(
            Long patientId,
            LocalDate appointmentDate,
            LocalTime appointmentTime,
            Long excludeAppointmentId) {

        List<Appointment> appointments =
                appointmentRepository.findByPatient_Id(patientId);

        boolean conflict = appointments.stream().anyMatch(existing ->
                existing.getAppointmentDate().equals(appointmentDate)
                        && existing.getAppointmentTime().equals(appointmentTime)
                        && existing.getStatus() == AppointmentStatus.BOOKED
                        && !existing.getId().equals(excludeAppointmentId));

        if (conflict) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This patient already has a booked appointment at this date and time.");
        }
    }
    
    @Transactional
    public Appointment cancelMyAppointment(
            Long appointmentId,
            String username,
            String role) {

        Appointment appointment = appointmentRepository.findById(appointmentId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Appointment not found"));

        // ADMIN can cancel any appointment
        if ("ADMIN".equals(role)) {
            return cancelAppointment(appointmentId);
        }

        // PATIENT can cancel only their own appointment
        if ("PATIENT".equals(role)) {
            Patient patient = patientRepository
                .findByAppUser_Username(username)
                .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Patient profile is not linked to this account"));

            if (!appointment.getPatient().getId().equals(patient.getId())) {
                throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You can cancel only your own appointments");
            }
        }

        // DOCTOR can cancel only appointments assigned to them
        else if ("DOCTOR".equals(role)) {
            Doctor doctor = doctorRepository
                .findByAppUser_Username(username)
                .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Doctor profile is not linked to this account"));

            if (!appointment.getDoctor().getId().equals(doctor.getId())) {
                throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You can cancel only appointments assigned to you");
            }
        } else {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN, "Access denied");
        }

        if (appointment.getStatus() != AppointmentStatus.BOOKED) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Only booked appointments can be cancelled");
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        return appointmentRepository.save(appointment);
    }
}
