package com.github.angellariosacosta.bookingapp.application.port.out;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.github.angellariosacosta.bookingapp.application.query.ListRolesQuery;
import com.github.angellariosacosta.bookingapp.application.result.PageResult;
import com.github.angellariosacosta.bookingapp.domain.model.Role;

public interface RoleRepository {

    /** No filtra por estado: lo usan el registro y el bootstrap para los roles base. */
    Optional<Role> findByName(String name);

    List<Role> findAll();

    /** Ignora los eliminados lógicamente. */
    Optional<Role> findById(Long id);

    /** Los roles (no eliminados) cuyo id está en {@code ids}; los ids desconocidos simplemente no aparecen. */
    List<Role> findAllByIds(Collection<Long> ids);

    /** Incluye eliminados: el nombre es único aunque el rol esté dado de baja. */
    boolean existsByName(String name);

    /** Alta si {@code id} es null (sin permisos); si no, actualiza nombre, descripción y estado del existente. */
    Role save(Role role);

    PageResult<Role> findPage(ListRolesQuery query);

    /** true si algún usuario no eliminado tiene el rol asignado con vínculo activo. */
    boolean isAssignedToUser(Long roleId);

    /** Deja activos exactamente los permisos indicados: revoca los demás (lógicamente) y reactiva/crea vínculos. */
    void replacePermissions(Long roleId, Set<Long> permissionIds);

    void markDeleted(Long id);
}
