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

import com.github.angellariosacosta.bookingapp.application.query.ListUsersQuery;
import com.github.angellariosacosta.bookingapp.application.query.SortDirection;
import com.github.angellariosacosta.bookingapp.application.query.UserSortField;
import com.github.angellariosacosta.bookingapp.application.result.PageResult;
import com.github.angellariosacosta.bookingapp.domain.model.Permission;
import com.github.angellariosacosta.bookingapp.domain.model.Role;
import com.github.angellariosacosta.bookingapp.domain.model.User;

/** Ejecuta las consultas reales contra la BD de pruebas (catálogo sembrado por V23/V24); cada test hace rollback. */
@SpringBootTest
@Transactional
class JpaUserRepositoryAdapterTest {

	@Autowired
	private JpaUserRepositoryAdapter users;
	@Autowired
	private JpaRoleRepositoryAdapter roles;

	private ListUsersQuery query(String search, Boolean active, Long roleId) {
		return new ListUsersQuery(search, active, roleId, 0, 50, UserSortField.USERNAME, SortDirection.ASC);
	}

	private Role role(String name) {
		return roles.findByName(name).orElseThrow();
	}

	private User newUser(String username, Role... userRoles) {
		return users.save(User.builder()
				.username(username).email(username + "@example.test").name("Zz " + username)
				.phone("5299" + Math.abs(username.hashCode() % 100000000)).password("hash").active(true)
				.roles(Set.of(userRoles)).build());
	}

	private Set<String> usernames(PageResult<User> page) {
		return page.content().stream().map(User::getUsername).collect(Collectors.toSet());
	}

	private Set<String> roleNames(User user) {
		return user.getRoles().stream().map(Role::name).collect(Collectors.toSet());
	}

	@Test
	void userRoleNoLongerCarriesUserManagementPermissions() {
		Set<String> names = role(Role.USER_NAME).permissions().stream().map(Permission::name).collect(Collectors.toSet());

		assertFalse(names.contains("USER_READ"));
		assertFalse(names.contains("USER_WRITE"));
		assertTrue(names.contains("APPOINTMENT_READ"));
	}

	@Test
	void searchMatchesUsernameEmailNameAndPhone() {
		User created = newUser("zz_search_user", role(Role.USER_NAME));

		assertTrue(usernames(users.findPage(query("ZZ_SEARCH", null, null))).contains("zz_search_user"));
		assertTrue(usernames(users.findPage(query("zz_search_user@example", null, null))).contains("zz_search_user"));
		assertTrue(usernames(users.findPage(query("Zz zz_search", null, null))).contains("zz_search_user"));
		assertTrue(usernames(users.findPage(query(created.getPhone(), null, null))).contains("zz_search_user"));
		assertTrue(users.findPage(query("%", null, null)).content().isEmpty());
	}

	@Test
	void filtersByActiveStateAndRole() {
		Role userRole = role(Role.USER_NAME);
		Role adminRole = role(Role.ADMIN_NAME);
		User regular = newUser("zz_regular", userRole);
		User boss = newUser("zz_boss", adminRole);
		regular.setActive(false);
		users.save(regular);

		assertEquals(Set.of("zz_regular"), usernames(users.findPage(query("zz_", false, null))));
		assertEquals(Set.of("zz_boss"), usernames(users.findPage(query("zz_", true, null))));
		assertEquals(Set.of("zz_boss"), usernames(users.findPage(query("zz_", null, adminRole.id()))));
		assertEquals(Set.of("zz_regular"), usernames(users.findPage(query("zz_", null, userRole.id()))));
		assertEquals(boss.getId() != null, true);
	}

	@Test
	void replaceRolesRevokesLogicallyAndReactivates() {
		Role userRole = role(Role.USER_NAME);
		Role adminRole = role(Role.ADMIN_NAME);
		User created = newUser("zz_roles", userRole);

		users.replaceRoles(created.getId(), Set.of(userRole.id(), adminRole.id()));
		assertEquals(Set.of("USER", "ADMIN"), roleNames(users.findById(created.getId()).orElseThrow()));

		users.replaceRoles(created.getId(), Set.of(adminRole.id()));
		assertEquals(Set.of("ADMIN"), roleNames(users.findById(created.getId()).orElseThrow()));

		users.replaceRoles(created.getId(), Set.of(userRole.id()));
		assertEquals(Set.of("USER"), roleNames(users.findById(created.getId()).orElseThrow()));
	}

	@Test
	void saveAddsAndReactivatesRolesButNeverRevokes() {
		Role userRole = role(Role.USER_NAME);
		Role adminRole = role(Role.ADMIN_NAME);
		User created = newUser("zz_save", userRole);
		users.replaceRoles(created.getId(), Set.of(adminRole.id()));

		User loaded = users.findById(created.getId()).orElseThrow();
		loaded.setName("Renamed");
		loaded.setRoles(Set.of(userRole));
		User saved = users.save(loaded);

		assertEquals("Renamed", saved.getName());
		assertEquals(Set.of("USER", "ADMIN"), roleNames(saved));
	}

	@Test
	void countsOnlyActiveAdminsExcludingTheGivenUser() {
		Role adminRole = role(Role.ADMIN_NAME);
		long before = users.countActiveUsersWithRole(Role.ADMIN_NAME, -1L);
		User first = newUser("zz_admin_1", adminRole);
		newUser("zz_admin_2", adminRole);

		assertEquals(before + 2, users.countActiveUsersWithRole(Role.ADMIN_NAME, -1L));
		assertEquals(before + 1, users.countActiveUsersWithRole(Role.ADMIN_NAME, first.getId()));

		first.setActive(false);
		users.save(first);
		assertEquals(before + 1, users.countActiveUsersWithRole(Role.ADMIN_NAME, -1L));
	}

	@Test
	void logicalDeleteHidesUserButKeepsUniqueValuesReserved() {
		User created = newUser("zz_deleted", role(Role.USER_NAME));

		users.markDeleted(created.getId());

		assertTrue(users.findById(created.getId()).isEmpty());
		assertTrue(users.findByUsername("zz_deleted").isEmpty());
		assertTrue(users.findByEmail("zz_deleted@example.test").isEmpty());
		assertTrue(users.findPage(query("zz_deleted", null, null)).content().isEmpty());
		assertTrue(users.existsByUsername("zz_deleted"));
		assertTrue(users.existsByEmail("zz_deleted@example.test"));
		assertTrue(users.existsByPhone(created.getPhone()));
	}

	@Test
	void deletedUserDoesNotKeepItsRoleInUse() {
		Role adminRole = role(Role.ADMIN_NAME);
		User created = newUser("zz_gone", adminRole);
		long withUser = users.countActiveUsersWithRole(Role.ADMIN_NAME, -1L);

		users.markDeleted(created.getId());

		assertEquals(withUser - 1, users.countActiveUsersWithRole(Role.ADMIN_NAME, -1L));
	}
}
