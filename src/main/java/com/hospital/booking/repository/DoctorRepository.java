package com.hospital.booking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.hospital.booking.dao.Doctor;
import java.util.Optional;


public interface DoctorRepository
        extends JpaRepository<Doctor, Long> {
	
	Optional<Doctor> findByAppUser_Username(String username);
	
	Optional<Doctor> findByEmail(String email);
	
	
}