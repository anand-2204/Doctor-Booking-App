
package com.hospital.booking.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.hospital.booking.dao.UserRole;
import com.hospital.booking.dao.AppUser;
import com.hospital.booking.repository.AppUserRepository;

@Configuration
public class AdminAccountInitializer {

    @Bean
    CommandLineRunner createInitialAdmin(
            AppUserRepository repository,
            PasswordEncoder passwordEncoder,
            @Value("${app.security.admin-username:}") String username,
            @Value("${app.security.admin-email:}") String email,
            @Value("${app.security.admin-password:}") String password) {

        return args -> {
            // Do not create an admin with default credentials.
            if (username.isBlank()
                    || email.isBlank()
                    || password.isBlank()) {
                return;
            }

            if (repository.existsByUsername(username)) {
                return;
            }

            if (repository.existsByEmail(email)) {
                throw new IllegalStateException(
                    "Admin email is already assigned to another account"
                );
            }

            if (password.length() < 12) {
                throw new IllegalStateException(
                    "Initial admin password must be at least 12 characters"
                );
            }

            AppUser admin = new AppUser();
            admin.setUsername(username);
            admin.setEmail(email);
            admin.setPassword(passwordEncoder.encode(password));
            admin.setRole(UserRole.ADMIN);

            repository.save(admin);
        };
    }
}
