
package com.hospital.booking.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.hospital.booking.dao.Patient;
import com.hospital.booking.service.PatientService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/patients")
public class PatientController {

    private final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    // Admin: Create a patient
    @PostMapping
    public ResponseEntity<Patient> createPatient(
            @Valid @RequestBody Patient patient) {

        Patient savedPatient = patientService.createPatient(patient);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(savedPatient);
    }

    // Admin: Get all patients
    @GetMapping
    public ResponseEntity<List<Patient>> getAllPatients() {
        return ResponseEntity.ok(patientService.getAllPatients());
    }

    // Admin: Get patient by ID
    @GetMapping("/{id}")
    public ResponseEntity<Patient> getPatientById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                patientService.getPatientById(id));
    }

    // Admin: Update any patient
    @PutMapping("/{id}")
    public ResponseEntity<Patient> updatePatient(
            @PathVariable Long id,
            @Valid @RequestBody Patient patient) {

        return ResponseEntity.ok(
                patientService.updatePatient(id, patient));
    }

    // Admin: Delete a patient
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePatient(
            @PathVariable Long id) {

        patientService.deletePatient(id);
        return ResponseEntity.noContent().build();
    }

    // Patient: Get their own profile
    @GetMapping("/me")
    public ResponseEntity<Patient> getMyProfile(
            Authentication authentication) {

        String username = authentication.getName();

        return ResponseEntity.ok(
                patientService.getMyProfile(username));
    }

    // Patient: Update their own profile
    @PutMapping("/me")
    public ResponseEntity<Patient> updateMyProfile(
            Authentication authentication,
            @Valid @RequestBody Patient patient) {

        String username = authentication.getName();

        return ResponseEntity.ok(
                patientService.updateMyProfile(username, patient));
    }
}
