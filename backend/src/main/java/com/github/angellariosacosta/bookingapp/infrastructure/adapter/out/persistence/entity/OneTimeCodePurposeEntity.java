package com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Catálogo de propósitos; sus filas las siembra la migración y se corresponden con el enum OneTimeCodePurpose. */
@Entity
@Table(
        name = "one_time_code_purpose",
        uniqueConstraints = @UniqueConstraint(name = "uk_one_time_code_purpose_name", columnNames = "name")
)
@Getter
@Setter
@NoArgsConstructor
public class OneTimeCodePurposeEntity {

    @Id
    @Column(name = "purpose_id")
    private Integer id;

    @Column(name = "name", nullable = false, length = 50)
    private String name;
}
