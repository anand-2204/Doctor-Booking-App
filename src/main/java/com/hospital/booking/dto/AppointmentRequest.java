package com.hospital.booking.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public class AppointmentRequest {

	 @NotNull(message = "Patient ID is required")
	    @Positive(message = "Patient ID must be positive")
	    private Long patientId;

	    @NotNull(message = "Doctor ID is required")
	    @Positive(message = "Doctor ID must be positive")
	    private Long doctorId;

	    @NotNull(message = "Appointment date is required")
	    private LocalDate appointmentDate;

	    @NotNull(message = "Appointment time is required")
	    private LocalTime appointmentTime;

	    @Size(max = 500, message = "Reason cannot exceed 500 characters")
	    private String reason;

    public AppointmentRequest() {
    }

    public Long getPatientId() {
        return patientId;
    }

    public void setPatientId(Long patientId) {
        this.patientId = patientId;
    }

    public Long getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(Long doctorId) {
        this.doctorId = doctorId;
    }

    public LocalDate getAppointmentDate() {
        return appointmentDate;
    }

    public void setAppointmentDate(LocalDate appointmentDate) {
        this.appointmentDate = appointmentDate;
    }

    public LocalTime getAppointmentTime() {
        return appointmentTime;
    }

    public void setAppointmentTime(LocalTime appointmentTime) {
        this.appointmentTime = appointmentTime;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}