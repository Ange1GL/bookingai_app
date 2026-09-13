package com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    /* ===================== AUDITORÍA ===================== */

    /**
     * Usuario que creó el registro
     * Se asigna SOLO en INSERT
     */
    @CreatedBy
    @Column(name = "created_by", updatable = false, length = 100)
    protected String createdBy;

    /**
     * Usuario que modificó por última vez
     * Se asigna SOLO en UPDATE
     */
    @LastModifiedBy
    @Column(name = "updated_by", length = 100)
    protected String updatedBy;

    /**
     * Fecha de creación (inmutable)
     */
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    protected Instant createdAt;

    /**
     * Fecha de última modificación
     */
    @LastModifiedDate
    @Column(name = "updated_at")
    protected Instant updatedAt;

    /* ===================== SOFT DELETE ===================== */

    /**
     * false = activo
     * true  = eliminado lógicamente
     */
    @Column(name = "deleted", nullable = false)
    protected boolean deleted = false;

    /**
     * Marca el registro como eliminado
     */
    public void markAsDeleted() {
        this.deleted = true;
    }

    public boolean isDeleted() {
        return deleted;
    }
}
