package com.github.angellariosacosta.bookingapp.application.port.out;

import java.util.Optional;

import com.github.angellariosacosta.bookingapp.application.query.ListPermissionsQuery;
import com.github.angellariosacosta.bookingapp.application.result.PageResult;
import com.github.angellariosacosta.bookingapp.domain.model.Permission;

public interface PermissionRepositoryPort {

	/** Ignora los eliminados lógicamente. */
	Optional<Permission> findById(Long id);

	/** Incluye eliminados: el nombre es único aunque el permiso esté dado de baja. */
	boolean existsByName(String name);

	/** Alta si {@code id} es null; si no, actualiza descripción y estado del existente. */
	Permission save(Permission permission);

	PageResult<Permission> findPage(ListPermissionsQuery query);

	/** true si algún rol vigente (no eliminado) tiene el permiso asignado con vínculo activo. */
	boolean isAssignedToRole(Long permissionId);

	void markDeleted(Long id);
}
