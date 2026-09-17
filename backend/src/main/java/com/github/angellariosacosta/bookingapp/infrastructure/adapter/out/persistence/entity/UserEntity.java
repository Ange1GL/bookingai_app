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
@Entity
@Builder
@Table(
        name = "user_app",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_app_username", columnNames = "username"),
                @UniqueConstraint(name = "uk_user_app_email", columnNames = "email")
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

    @Builder.Default
    @OneToMany(mappedBy = "user", fetch = FetchType.EAGER)
    private Set<UserRoleEntity> userRoles = new HashSet<>();

    public Set<RoleEntity> getRoleEntities() {
        return userRoles.stream()
                .map(UserRoleEntity::getRole)
                .collect(Collectors.toSet());
    }
}
