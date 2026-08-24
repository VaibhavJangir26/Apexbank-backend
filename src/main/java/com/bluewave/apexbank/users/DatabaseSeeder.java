package com.bluewave.apexbank.users;

import com.bluewave.apexbank.utils.common.AppRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseSeeder implements CommandLineRunner {

    private final UsersRepo usersRepo;
    private final RolesRepo rolesRepo;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        log.info("[SEEDER] Checking database seeding...");

        // Ensure roles exist in DB
        Roles adminRole = rolesRepo.findByAppRole(AppRole.ROLE_ADMIN)
                .orElseGet(() -> rolesRepo.save(new Roles(null, AppRole.ROLE_ADMIN, new HashSet<>())));

        Roles managerRole = rolesRepo.findByAppRole(AppRole.ROLE_MANAGER)
                .orElseGet(() -> rolesRepo.save(new Roles(null, AppRole.ROLE_MANAGER, new HashSet<>())));

        Roles customerRole = rolesRepo.findByAppRole(AppRole.ROLE_CUSTOMER)
                .orElseGet(() -> rolesRepo.save(new Roles(null, AppRole.ROLE_CUSTOMER, new HashSet<>())));

        // Check if admin123 already exists
        if (usersRepo.findByUsername("admin123").isEmpty()) {
            log.info("[SEEDER] Seeding admin user 'admin123'...");
            Users admin = new Users();
            admin.setUsername("admin123");
            admin.setEmail("admin@apexbank.com");
            admin.setPassword(passwordEncoder.encode("123456"));
            admin.setRoles(Set.of(adminRole, managerRole));
            usersRepo.save(admin);
            log.info("[SEEDER] Admin user 'admin123' seeded successfully.");
        } else {
            log.info("[SEEDER] Admin user 'admin123' already exists. Skipping.");
        }
    }
}
