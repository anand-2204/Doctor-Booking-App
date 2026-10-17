
package com.hospital.booking.controller;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.core.userdetails.UserDetailsService;
import com.hospital.booking.dao.UserRole;
import com.hospital.booking.dto.AuthResponse;
import com.hospital.booking.dto.LinkDoctorAccountRequest;
import com.hospital.booking.dto.LoginRequest;
import com.hospital.booking.dto.LoginResponse;
import com.hospital.booking.dto.RegisterRequest;
import com.hospital.booking.dao.AppUser;
import com.hospital.booking.repository.AppUserRepository;
import com.hospital.booking.security.JwtService;
import com.hospital.booking.dto.RefreshRequest;
import com.hospital.booking.security.RefreshTokenService;
import org.springframework.transaction.annotation.Transactional;

import com.hospital.booking.dao.Patient;
import com.hospital.booking.repository.PatientRepository;
import com.hospital.booking.dao.Doctor;
import com.hospital.booking.repository.DoctorRepository;

@RestController
@RequestMapping("/api/auth")
@Validated
public class AuthController {

	private final AppUserRepository appUserRepository;
	private final PasswordEncoder passwordEncoder;
	private final AuthenticationManager authenticationManager;
	private final JwtService jwtService;
	private final RefreshTokenService refreshTokenService;
	private final DoctorRepository doctorRepository;
	private final PatientRepository patientRepository;
	private final UserDetailsService userDetailsService;

	public AuthController(AppUserRepository appUserRepository, PasswordEncoder passwordEncoder,
			AuthenticationManager authenticationManager, JwtService jwtService, RefreshTokenService refreshTokenService,
			PatientRepository patientRepository, DoctorRepository doctorRepository,
			org.springframework.security.core.userdetails.UserDetailsService userDetailsService) {
		this.appUserRepository = appUserRepository;
		this.passwordEncoder = passwordEncoder;
		this.authenticationManager = authenticationManager;
		this.jwtService = jwtService;
		this.patientRepository = patientRepository;
		this.refreshTokenService = refreshTokenService;
		this.userDetailsService = userDetailsService;
		this.doctorRepository = doctorRepository;
	}

	@PostMapping("/register")
	@Transactional
	public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {

		String username = request.getUsername().trim();
		String email = request.getEmail().trim().toLowerCase();

		if (appUserRepository.existsByUsername(username)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Username is already taken");
		}

		if (appUserRepository.existsByEmail(email)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered");
		}

		// Do not automatically claim an existing patient profile.
		if (patientRepository.existsByEmail(email)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "A patient profile already exists with this email. "
					+ "Please contact the administrator to link it securely.");
		}

		// 1. Create the login account.
		AppUser user = new AppUser();
		user.setUsername(username);
		user.setEmail(email);
		user.setPassword(passwordEncoder.encode(request.getPassword()));
		user.setRole(UserRole.PATIENT);

		AppUser savedUser = appUserRepository.save(user);

		// 2. Create the patient profile.
		Patient patient = new Patient();
		patient.setName(request.getName().trim());
		patient.setEmail(email);
		patient.setPhone(request.getPhone());
		patient.setAge(request.getAge());
		patient.setGender(request.getGender());
		patient.setAddress(request.getAddress());

		// 3. Link the patient profile to its account.
		patient.setAppUser(savedUser);

		patientRepository.save(patient);

		AuthResponse response = new AuthResponse(
				"Registration and patient profile creation successful. " + "You can now log in.",
				savedUser.getUsername(), savedUser.getRole().name());

		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@PostMapping("/login")
	public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {

		Authentication authentication = authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(request.getUsername().trim(), request.getPassword()));

		UserDetails userDetails = (UserDetails) authentication.getPrincipal();

		String accessToken = jwtService.generateToken(userDetails);

		String refreshToken = refreshTokenService.createRefreshToken(userDetails.getUsername());

		String role = userDetails.getAuthorities().stream().map(authority -> authority.getAuthority())
				.filter(authority -> authority.startsWith("ROLE_")).map(authority -> authority.substring(5)).findFirst()
				.orElseThrow(() -> new IllegalStateException("User has no role"));

		LoginResponse response = new LoginResponse(accessToken, refreshToken, jwtService.getExpirationMs() / 1000,
				userDetails.getUsername(), role);

		return ResponseEntity.ok(response);
	}

	@PostMapping("/refresh")
	public ResponseEntity<LoginResponse> refresh(@Valid @RequestBody RefreshRequest request) {

		String oldRefreshToken = request.getRefreshToken();

		// 1. Validate the existing refresh token.
		String username = refreshTokenService.validateRefreshToken(oldRefreshToken);

		// 2. Load the actual user and authorities from the database.
		UserDetails userDetails = userDetailsService.loadUserByUsername(username);

		// 3. Rotate the refresh token.
		String newRefreshToken = refreshTokenService.rotateRefreshToken(oldRefreshToken);

		// 4. Generate a new access token.
		String newAccessToken = jwtService.generateToken(userDetails);

		// 5. Extract the user's role.
		String role = userDetails.getAuthorities().stream().map(authority -> authority.getAuthority())
				.filter(authority -> authority.startsWith("ROLE_")).map(authority -> authority.substring(5)).findFirst()
				.orElseThrow(() -> new IllegalStateException("User has no role"));

		// 6. Return both new tokens.
		LoginResponse response = new LoginResponse(newAccessToken, newRefreshToken, jwtService.getExpirationMs() / 1000,
				username, role);

		return ResponseEntity.ok(response);
	}

	@PostMapping("/logout")
	public ResponseEntity<String> logout(@Valid @RequestBody RefreshRequest request) {

		refreshTokenService.revokeRefreshToken(request.getRefreshToken());

		return ResponseEntity.ok("Logout successful");
	}

	@PostMapping("/admin/link-doctor")
	@Transactional
	public ResponseEntity<AuthResponse> linkDoctorAccount(@Valid @RequestBody LinkDoctorAccountRequest request) {

		String username = request.getUsername().trim();
		String email = request.getEmail().trim().toLowerCase();

		// Check username and email availability.
		if (appUserRepository.existsByUsername(username)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Username is already taken");
		}

		if (appUserRepository.existsByEmail(email)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered");
		}

		// Find the existing doctor profile.
		Doctor doctor = doctorRepository.findById(request.getDoctorId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Doctor profile not found"));

		// Do not allow an already linked profile.
		if (doctor.getAppUser() != null) {
			throw new ResponseStatusException(HttpStatus.CONFLICT,
					"This doctor profile is already linked to an account");
		}

		// Ensure the account email matches the doctor's existing email.
		if (!doctor.getEmail().equalsIgnoreCase(email)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"Account email must match the existing doctor's email");
		}

		// Create the doctor login account.
		AppUser user = new AppUser();
		user.setUsername(username);
		user.setEmail(email);
		user.setPassword(passwordEncoder.encode(request.getPassword()));
		user.setRole(UserRole.DOCTOR);

		AppUser savedUser = appUserRepository.save(user);

		// Link the existing doctor profile.
		doctor.setAppUser(savedUser);
		doctorRepository.save(doctor);

		AuthResponse response = new AuthResponse("Doctor account created and linked successfully. You can now log in.",
				savedUser.getUsername(), savedUser.getRole().name());

		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

}
