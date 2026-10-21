package com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(
        name = "roles",
        uniqueConstraints = @UniqueConstraint(name = "uk_roles_name", columnNames = "name")
)
public class RoleEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "role_id")
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(length = 255)
    private String description;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    // Roles del catálogo base (USER, ADMIN): no se deshabilitan, eliminan ni renombran.
    @Builder.Default
    @Column(name = "is_system", nullable = false)
    private boolean system = false;

    @Builder.Default
    @BatchSize(size = 50)
    @OneToMany(mappedBy = "role", fetch = FetchType.EAGER)
    private Set<RolePermissionEntity> rolePermissions = new HashSet<>();

    // Solo vínculos activos hacia permisos vigentes (activos y no eliminados).
    public Set<PermissionEntity> getPermissionEntities() {
        return rolePermissions.stream()
                .filter(RolePermissionEntity::isActive)
                .map(RolePermissionEntity::getPermission)
                .filter(permission -> permission.isActive() && !permission.isDeleted())
                .collect(Collectors.toSet());
    }
}
