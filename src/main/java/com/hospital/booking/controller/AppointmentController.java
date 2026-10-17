package com.hospital.booking.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.hospital.booking.dto.AppointmentRequest;
import com.hospital.booking.dao.Appointment;
import com.hospital.booking.service.AppointmentService;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

	private final AppointmentService appointmentService;

	public AppointmentController(AppointmentService appointmentService) {
		this.appointmentService = appointmentService;
	}

	// Book appointment
	@PostMapping
	public ResponseEntity<Appointment> bookAppointment(@Valid @RequestBody AppointmentRequest request) {

		Appointment appointment = appointmentService.bookAppointment(request);

		return ResponseEntity.status(HttpStatus.CREATED).body(appointment);
	}

	// Get all appointments
	@GetMapping
	public ResponseEntity<List<Appointment>> getAllAppointments() {
		return ResponseEntity.ok(appointmentService.getAllAppointments());
	}

	// Get appointment by ID
	@GetMapping("/{id}")
	public ResponseEntity<Appointment> getAppointmentById(@PathVariable Long id) {

		return ResponseEntity.ok(appointmentService.getAppointmentById(id));
	}

	// Get appointments by patient ID
	@GetMapping("/patient/{patientId}")
	public ResponseEntity<List<Appointment>> getByPatient(@PathVariable Long patientId) {

		return ResponseEntity.ok(appointmentService.getAppointmentsByPatient(patientId));
	}

	// Get appointments by doctor ID
	@GetMapping("/doctor/{doctorId}")
	public ResponseEntity<List<Appointment>> getByDoctor(@PathVariable Long doctorId) {

		return ResponseEntity.ok(appointmentService.getAppointmentsByDoctor(doctorId));
	}

	// Update appointment
	@PutMapping("/{id}")
	public ResponseEntity<Appointment> updateAppointment(@PathVariable Long id,
			@Valid @RequestBody AppointmentRequest request) {

		return ResponseEntity.ok(appointmentService.updateAppointment(id, request));
	}

	@PatchMapping("/{id}/cancel")
	public ResponseEntity<Appointment> cancelAppointment(
	        @PathVariable Long id,
	        Authentication authentication) {

	    String username = authentication.getName();

	    String role = authentication.getAuthorities().stream()
	        .map(authority -> authority.getAuthority())
	        .filter(authority -> authority.startsWith("ROLE_"))
	        .map(authority -> authority.substring(5))
	        .findFirst()
	        .orElseThrow(() -> new ResponseStatusException(
	            HttpStatus.FORBIDDEN, "User role not found"));

	    Appointment cancelled = appointmentService.cancelMyAppointment(
	        id, username, role);

	    return ResponseEntity.ok(cancelled);
	}

	@GetMapping("/booked")
	public List<Appointment> getBookedAppointments() {
		return appointmentService.getBookedAppointments();
	}

	@GetMapping("/date/{date}")
	public List<Appointment> getAppointmentsByDate(
			@PathVariable @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate date) {

		return appointmentService.getAppointmentsByDate(date);
	}

	@GetMapping("/doctor/{doctorId}/date/{date}")
	public List<Appointment> getAppointmentsByDoctorAndDate(@PathVariable Long doctorId,
			@PathVariable @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate date) {

		return appointmentService.getAppointmentsByDoctorAndDate(doctorId, date);
	}

	@GetMapping("/page")
	public Page<Appointment> getAppointmentsWithPagination(@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "5") int size, @RequestParam(defaultValue = "appointmentDate") String sortBy,
			@RequestParam(defaultValue = "asc") String direction) {

		validatePagination(page, size, sortBy, direction);

		Sort sort = direction.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();

		Pageable pageable = PageRequest.of(page, size, sort);

		return appointmentService.getAllAppointments(pageable);
	}

	@GetMapping("/page/status")
	public Page<Appointment> getAppointmentsByStatus(@RequestParam String status,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "5") int size,
			@RequestParam(defaultValue = "appointmentDate") String sortBy,
			@RequestParam(defaultValue = "asc") String direction) {

		validatePagination(page, size, sortBy, direction);

		Sort sort = direction.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();

		Pageable pageable = PageRequest.of(page, size, sort);

		return appointmentService.getAppointmentsByStatus(status, pageable);
	}

	private void validatePagination(int page, int size, String sortBy, String direction) {

		if (page < 0) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Page number cannot be negative");
		}

		if (size < 1 || size > 100) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Page size must be between 1 and 100");
		}

		List<String> allowedSortFields = List.of("id", "appointmentDate", "appointmentTime", "status");

		if (!allowedSortFields.contains(sortBy)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"Invalid sort field. Allowed fields: " + allowedSortFields);
		}

		if (!direction.equalsIgnoreCase("asc") && !direction.equalsIgnoreCase("desc")) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sort direction must be asc or desc");
		}
	}

	// Get appointments for the logged-in user
	@GetMapping("/my")
	public ResponseEntity<List<Appointment>> getMyAppointments(Authentication authentication) {

		String username = authentication.getName();

		String role = authentication.getAuthorities().stream().map(authority -> authority.getAuthority())
				.filter(authority -> authority.startsWith("ROLE_")).map(authority -> authority.substring(5)).findFirst()
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "User role not found"));

		return ResponseEntity.ok(appointmentService.getMyAppointments(username, role));
	}

}