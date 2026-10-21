package com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.github.angellariosacosta.bookingapp.application.query.ListPermissionsQuery;
import com.github.angellariosacosta.bookingapp.application.query.ListRolesQuery;
import com.github.angellariosacosta.bookingapp.application.query.PermissionSortField;
import com.github.angellariosacosta.bookingapp.application.query.RoleSortField;
import com.github.angellariosacosta.bookingapp.application.query.SortDirection;
import com.github.angellariosacosta.bookingapp.domain.model.Permission;
import com.github.angellariosacosta.bookingapp.domain.model.Role;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.entity.UserEntity;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.entity.UserRoleEntity;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.repository.JpaRepositoryUser;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.repository.JpaRepositoryUserRole;

/** Ejecuta las consultas reales contra la BD de pruebas (catálogo sembrado por V23); cada test hace rollback. */
@SpringBootTest
@Transactional
class JpaRoleRepositoryAdapterTest {

	@Autowired
	private JpaRoleRepositoryAdapter roles;
	@Autowired
	private JpaPermissionRepositoryAdapter permissions;
	@Autowired
	private JpaRepositoryUser users;
	@Autowired
	private JpaRepositoryUserRole userRoles;

	private ListRolesQuery query(String search, Boolean active) {
		return new ListRolesQuery(search, active, 0, 50, RoleSortField.NAME, SortDirection.ASC);
	}

	private Long permissionId(String name) {
		return permissions.findPage(new ListPermissionsQuery(name, null, 0, 50, PermissionSortField.NAME, SortDirection.ASC))
				.content().stream().filter(p -> p.name().equals(name)).findFirst().orElseThrow().id();
	}

	private Set<String> permissionNames(Role role) {
		return role.permissions().stream().map(Permission::name).collect(Collectors.toSet());
	}

	@Test
	void seededSystemRolesAreListedAndSearchable() {
		var page = roles.findPage(query("admin", true));

		Role admin = page.content().stream().filter(r -> r.name().equals(Role.ADMIN_NAME)).findFirst().orElseThrow();
		assertTrue(admin.system());
		assertTrue(permissionNames(admin).contains(Permission.ADMIN_ALL_NAME));
	}

	@Test
	void replacePermissionsRevokesLogicallyAndReactivates() {
		Role created = roles.save(Role.create("ZZ_TEMP_ROLE", "temp"));
		Long userRead = permissionId("USER_READ");
		Long roleRead = permissionId("ROLE_READ");

		roles.replacePermissions(created.id(), Set.of(userRead, roleRead));
		assertEquals(Set.of("USER_READ", "ROLE_READ"), permissionNames(roles.findById(created.id()).orElseThrow()));

		roles.replacePermissions(created.id(), Set.of(userRead));
		assertEquals(Set.of("USER_READ"), permissionNames(roles.findById(created.id()).orElseThrow()));

		// el vínculo revocado se reactiva (no viola el UNIQUE) y no se duplica
		roles.replacePermissions(created.id(), Set.of(userRead, roleRead));
		assertEquals(Set.of("USER_READ", "ROLE_READ"), permissionNames(roles.findById(created.id()).orElseThrow()));

		roles.replacePermissions(created.id(), Set.of());
		assertTrue(roles.findById(created.id()).orElseThrow().permissions().isEmpty());
	}

	@Test
	void permissionAssignedToActiveRoleCountsAsInUseUntilRevoked() {
		Role created = roles.save(Role.create("ZZ_TEMP_ROLE", null));
		Long permissionId = permissions.save(Permission.create("ZZ_TEMP_PERMISSION", null)).id();
		assertFalse(permissions.isAssignedToRole(permissionId));

		roles.replacePermissions(created.id(), Set.of(permissionId));
		assertTrue(permissions.isAssignedToRole(permissionId));

		roles.replacePermissions(created.id(), Set.of());
		assertFalse(permissions.isAssignedToRole(permissionId));
	}

	@Test
	void roleInUseDependsOnActiveLinkToNonDeletedUser() {
		Role created = roles.save(Role.create("ZZ_TEMP_ROLE", null));
		assertFalse(roles.isAssignedToUser(created.id()));

		UserEntity user = users.save(UserEntity.builder()
				.username("zz_tmp_user").email("zz_tmp_user@example.test").active(true).build());
		UserRoleEntity link = userRoles.save(
				UserRoleEntity.builder().userId(user.getId()).roleId(created.id()).active(true).build());
		assertTrue(roles.isAssignedToUser(created.id()));

		link.setActive(false);
		assertFalse(roles.isAssignedToUser(created.id()));

		link.setActive(true);
		user.markAsDeleted();
		assertFalse(roles.isAssignedToUser(created.id()));
	}

	@Test
	void logicalDeleteHidesRoleButKeepsNameReserved() {
		Role created = roles.save(Role.create("ZZ_TEMP_ROLE", null));

		roles.markDeleted(created.id());

		assertTrue(roles.findById(created.id()).isEmpty());
		assertTrue(roles.findPage(query("zz_temp", null)).content().isEmpty());
		assertTrue(roles.existsByName("ZZ_TEMP_ROLE"));
	}

	@Test
	void updateKeepsDeletedFlagAndPermissions() {
		Role created = roles.save(Role.create("ZZ_TEMP_ROLE", "a"));
		roles.replacePermissions(created.id(), Set.of(permissionId("USER_READ")));

		Role updated = roles.save(roles.findById(created.id()).orElseThrow().withName("zz_renamed").withActive(false));

		assertEquals("ZZ_RENAMED", updated.name());
		assertFalse(updated.active());
		assertEquals(Set.of("USER_READ"), permissionNames(updated));
		assertEquals(1, roles.findPage(query("zz_renamed", false)).totalElements());
	}
}
