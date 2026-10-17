
package com.hospital.booking.repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.hospital.booking.dao.Appointment;
import com.hospital.booking.dao.AppointmentStatus;

public interface AppointmentRepository
        extends JpaRepository<Appointment, Long> {

    // Get appointments by patient
    List<Appointment> findByPatient_Id(Long patientId);

    // Get appointments by doctor
    List<Appointment> findByDoctor_Id(Long doctorId);

    // Check whether doctor already has a booked appointment
    boolean existsByDoctor_IdAndAppointmentDateAndAppointmentTimeAndStatus(
            Long doctorId,
            LocalDate appointmentDate,
            LocalTime appointmentTime,
            AppointmentStatus status);

    // Check whether patient already has a booked appointment
    boolean existsByPatient_IdAndAppointmentDateAndAppointmentTimeAndStatus(
            Long patientId,
            LocalDate appointmentDate,
            LocalTime appointmentTime,
            AppointmentStatus status);

    // Get appointments by status
    List<Appointment> findByStatus(AppointmentStatus status);

    // Get appointments by status with pagination
    Page<Appointment> findByStatus(
            AppointmentStatus status,
            Pageable pageable);

    // Get appointments by date
    List<Appointment> findByAppointmentDate(LocalDate appointmentDate);

    // Get appointments by doctor and date
    List<Appointment> findByDoctor_IdAndAppointmentDate(
            Long doctorId,
            LocalDate appointmentDate);
}
