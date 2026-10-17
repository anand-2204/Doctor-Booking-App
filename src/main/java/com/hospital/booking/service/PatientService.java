
package com.hospital.booking.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.hospital.booking.dao.Patient;
import com.hospital.booking.repository.PatientRepository;

@Service
public class PatientService {

    private final PatientRepository patientRepository;

    public PatientService(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    // Admin: Create a patient
    public Patient createPatient(Patient patient) {
        patient.setId(null);
        patient.setAppUser(null);
        return patientRepository.save(patient);
    }

    // Admin: Get all patients
    public List<Patient> getAllPatients() {
        return patientRepository.findAll();
    }

    // Admin: Get a patient by ID
    public Patient getPatientById(Long id) {
        return patientRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Patient not found with ID: " + id));
    }

    // Admin: Update any patient
    public Patient updatePatient(Long id, Patient updatedPatient) {
        Patient existingPatient = getPatientById(id);

        existingPatient.setName(updatedPatient.getName());
        existingPatient.setEmail(updatedPatient.getEmail());
        existingPatient.setPhone(updatedPatient.getPhone());
        existingPatient.setAge(updatedPatient.getAge());
        existingPatient.setGender(updatedPatient.getGender());
        existingPatient.setAddress(updatedPatient.getAddress());

        return patientRepository.save(existingPatient);
    }

    // Admin: Delete a patient
    public void deletePatient(Long id) {
        Patient patient = getPatientById(id);
        patientRepository.delete(patient);
    }

    // Patient: Get only their own profile
    public Patient getMyProfile(String username) {
        return patientRepository.findByAppUser_Username(username)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No patient profile is linked to this account"));
    }

    // Patient: Update only their own profile
    public Patient updateMyProfile(
            String username, Patient updatedPatient) {

        Patient existingPatient = getMyProfile(username);

        existingPatient.setName(updatedPatient.getName());
        existingPatient.setPhone(updatedPatient.getPhone());
        existingPatient.setAge(updatedPatient.getAge());
        existingPatient.setGender(updatedPatient.getGender());
        existingPatient.setAddress(updatedPatient.getAddress());

        // Email and account linkage are deliberately not changed here.
        return patientRepository.save(existingPatient);
    }
}
