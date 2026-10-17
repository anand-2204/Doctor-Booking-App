
package com.hospital.booking.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hospital.booking.dao.Patient;

public interface PatientRepository
        extends JpaRepository<Patient, Long> {

    boolean existsByEmail(String email);

    Optional<Patient> findByAppUser_Username(String username);
}
