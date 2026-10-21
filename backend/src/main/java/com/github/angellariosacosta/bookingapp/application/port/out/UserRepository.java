package com.github.angellariosacosta.bookingapp.application.port.out;

import java.util.Optional;
import java.util.Set;

import com.github.angellariosacosta.bookingapp.application.query.ListUsersQuery;
import com.github.angellariosacosta.bookingapp.application.result.PageResult;
import com.github.angellariosacosta.bookingapp.domain.model.User;

public interface UserRepository {

    /** Las búsquedas por id, email y username ignoran a los usuarios eliminados lógicamente. */
    Optional<User> findById(Long id);

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    /** Alta si {@code id} es null; si no, actualiza los datos. Solo añade/reactiva roles, nunca los revoca. */
    User save(User user);

    /** Las comprobaciones de existencia incluyen eliminados: email, username y teléfono son únicos aun así. */
    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByPhone(String phone);

    PageResult<User> findPage(ListUsersQuery query);

    /** Usuarios activos, no eliminados, con el rol (activo) indicado, sin contar a {@code excludedUserId}. */
    long countActiveUsersWithRole(String roleName, Long excludedUserId);

    /** Deja activos exactamente los roles indicados: revoca los demás (lógicamente) y reactiva/crea vínculos. */
    void replaceRoles(Long userId, Set<Long> roleIds);

    void markDeleted(Long id);
}
