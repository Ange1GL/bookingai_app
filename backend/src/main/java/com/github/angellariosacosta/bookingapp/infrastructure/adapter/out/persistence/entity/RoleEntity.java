package com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "roles")
public class RoleEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "role_id")
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String name;

    @Column(length = 255)
    private String description;

    @Builder.Default
    @OneToMany(mappedBy = "role", fetch = FetchType.EAGER)
    private Set<RolePermissionEntity> rolePermissions = new HashSet<>();

    public Set<PermissionEntity> getPermissionEntities() {
        return rolePermissions.stream()
                .map(RolePermissionEntity::getPermission)
                .collect(Collectors.toSet());
    }
}
