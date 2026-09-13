package com.github.angellariosacosta.bookingapp.infrastructure.config;


import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.entity.PermissionEntity;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.entity.RoleEntity;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.repository.JpaRepositoryPermission;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.repository.JpaRepositoryRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final JpaRepositoryPermission permissionRepository;
    private final JpaRepositoryRole roleRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        log.info("Initializing roles and permissions...");

        PermissionEntity appointmentRead   = findOrCreate("APPOINTMENT_READ",   "Read user data");
        PermissionEntity appointmentCreate   = findOrCreate("APPOINTMENT_CREATE",   "Read user data");
        PermissionEntity appointmentDelete   = findOrCreate("APPOINTMENT_DELETE",   "Read user data");

        PermissionEntity userRead   = findOrCreate("USER_READ",   "Read user data");
        PermissionEntity userWrite  = findOrCreate("USER_WRITE",  "Create and update users");
        PermissionEntity userDelete = findOrCreate("USER_DELETE", "Delete users");
        PermissionEntity adminAll   = findOrCreate("ADMIN_ALL",   "Full administrative access");

        findOrCreateRole("USER", "Standard user", Set.of(userRead, userWrite, appointmentCreate, appointmentDelete, appointmentRead));
        findOrCreateRole("ADMIN", "Administrator", Set.of(userRead, userWrite, userDelete, adminAll));

        log.info("Roles and permissions initialized.");
    }

    private PermissionEntity findOrCreate(String name, String description) {
        return permissionRepository.findByName(name).orElseGet(() -> {
            log.info("Creating permission: {}", name);
            return permissionRepository.save(
                    PermissionEntity.builder().name(name).description(description).build()
            );
        });
    }

    private void findOrCreateRole(String name, String description, Set<PermissionEntity> permissions) {
        roleRepository.findByName(name).orElseGet(() -> {
            log.info("Creating role: {}", name);
            return roleRepository.save(
                    RoleEntity.builder().name(name).description(description).permissions(permissions).build()
            );
        });
    }
}
