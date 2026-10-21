package com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;

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

    // Opcional: el canal de contacto real es el teléfono. Varias filas con email NULL no violan el unique.
    private String email;

    private String name;

    @Column(name = "last_name")
    private String lastName;

    private String password;
    private boolean active;

    @Column(length = 10)
    private String phone;

    @Column(name = "password_changed_at")
    private Instant passwordChangedAt;

    @Builder.Default
    @BatchSize(size = 50)
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
