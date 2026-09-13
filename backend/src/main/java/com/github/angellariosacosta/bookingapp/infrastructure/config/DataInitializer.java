package com.github.angellariosacosta.bookingapp.infrastructure.config;

import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.entity.PermissionEntity;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.entity.RoleEntity;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.entity.RolePermissionEntity;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.repository.JpaRepositoryPermission;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.repository.JpaRepositoryRole;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.repository.JpaRepositoryRolePermission;
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
    private final JpaRepositoryRolePermission rolePermissionRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        log.info("Initializing roles and permissions...");

        PermissionEntity appointmentRead   = findOrCreate("APPOINTMENT_READ",   "Read appointments");
        PermissionEntity appointmentCreate = findOrCreate("APPOINTMENT_CREATE", "Create appointments");
        PermissionEntity appointmentDelete = findOrCreate("APPOINTMENT_DELETE", "Delete appointments");

        PermissionEntity userRead   = findOrCreate("USER_READ",   "Read user data");
        PermissionEntity userWrite  = findOrCreate("USER_WRITE",  "Create and update users");
        PermissionEntity userDelete = findOrCreate("USER_DELETE", "Delete users");
        PermissionEntity adminAll   = findOrCreate("ADMIN_ALL",   "Full administrative access");

        findOrCreateRole("USER",  "Standard user",  Set.of(userRead, userWrite, appointmentCreate, appointmentDelete, appointmentRead));
        findOrCreateRole("ADMIN", "Administrator",  Set.of(userRead, userWrite, userDelete, adminAll));

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
        RoleEntity role = roleRepository.findByName(name).orElseGet(() -> {
            log.info("Creating role: {}", name);
            return roleRepository.save(
                    RoleEntity.builder().name(name).description(description).build()
            );
        });

        permissions.forEach(p -> {
            if (!rolePermissionRepository.existsByRoleIdAndPermissionId(role.getId(), p.getId())) {
                rolePermissionRepository.save(
                        RolePermissionEntity.builder()
                                .roleId(role.getId())
                                .permissionId(p.getId())
                                .build()
                );
            }
        });
    }
}
