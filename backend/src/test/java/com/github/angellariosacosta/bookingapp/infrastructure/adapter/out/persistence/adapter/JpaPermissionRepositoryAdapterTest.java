package com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.github.angellariosacosta.bookingapp.application.query.ListPermissionsQuery;
import com.github.angellariosacosta.bookingapp.application.query.PermissionSortField;
import com.github.angellariosacosta.bookingapp.application.query.SortDirection;
import com.github.angellariosacosta.bookingapp.application.result.PageResult;
import com.github.angellariosacosta.bookingapp.domain.model.Permission;

/** Ejecuta las consultas reales contra la BD de pruebas (con el catálogo sembrado por V23); cada test hace rollback. */
@SpringBootTest
@Transactional
class JpaPermissionRepositoryAdapterTest {

	@Autowired
	private JpaPermissionRepositoryAdapter adapter;

	private ListPermissionsQuery query(String search, Boolean active) {
		return new ListPermissionsQuery(search, active, 0, 50, PermissionSortField.NAME, SortDirection.ASC);
	}

	private List<String> names(PageResult<Permission> page) {
		return page.content().stream().map(Permission::name).toList();
	}

	@Test
	void searchMatchesNameIgnoringCase() {
		List<String> names = names(adapter.findPage(query("permission_", null)));

		assertTrue(names.containsAll(List.of("PERMISSION_READ", "PERMISSION_WRITE")));
		assertFalse(names.contains("USER_READ"));
	}

	@Test
	void searchTreatsWildcardsAsLiterals() {
		assertTrue(adapter.findPage(query("%", null)).content().isEmpty());
	}

	@Test
	void seededPermissionsAreSystemAndAssignedToRoles() {
		Permission userRead = adapter.findPage(query("USER_READ", null)).content().get(0);

		assertTrue(userRead.system());
		assertTrue(adapter.isAssignedToRole(userRead.id()));
	}

	@Test
	void newPermissionIsNotAssignedAndCanBeLogicallyDeleted() {
		Permission created = adapter.save(Permission.create("ZZ_TEMP_PERMISSION", "temp"));
		assertFalse(adapter.isAssignedToRole(created.id()));
		assertTrue(adapter.findPage(query("zz_temp", true)).content().stream().anyMatch(p -> p.id().equals(created.id())));

		Permission disabled = adapter.save(created.withActive(false));
		assertFalse(disabled.active());
		assertTrue(adapter.findPage(query("zz_temp", true)).content().isEmpty());
		assertEquals(1, adapter.findPage(query("zz_temp", false)).totalElements());

		adapter.markDeleted(created.id());

		assertTrue(adapter.findById(created.id()).isEmpty());
		assertTrue(adapter.findPage(query("zz_temp", null)).content().isEmpty());
		assertTrue(adapter.existsByName("ZZ_TEMP_PERMISSION"));
	}
}
