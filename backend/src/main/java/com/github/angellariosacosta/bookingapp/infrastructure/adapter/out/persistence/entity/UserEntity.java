package com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Builder
@Table(
        name = "user_app",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_app_username", columnNames = "username"),
                @UniqueConstraint(name = "uk_user_app_email", columnNames = "email"),
                @UniqueConstraint(name = "uk_user_app_phone", columnNames = "phone")
        }
)
public class UserEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private String email;

    private String name;
    private String password;
    private boolean active;

    @Column(length = 20)
    private String phone;

    @Column(name = "must_change_password", nullable = false)
    private boolean mustChangePassword;

    @Column(name = "password_changed_at")
    private Instant passwordChangedAt;

    @Builder.Default
    @OneToMany(mappedBy = "user", fetch = FetchType.EAGER)
    private Set<UserRoleEntity> userRoles = new HashSet<>();

    // Solo vínculos activos hacia roles vigentes (activos y no eliminados).
    public Set<RoleEntity> getRoleEntities() {
        return userRoles.stream()
                .filter(UserRoleEntity::isActive)
                .map(UserRoleEntity::getRole)
                .filter(role -> role.isActive() && !role.isDeleted())
                .collect(Collectors.toSet());
    }
}
