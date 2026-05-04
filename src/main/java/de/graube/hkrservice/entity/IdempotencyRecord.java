package de.graube.hkrservice.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

/**
 * Persistierter Datensatz fuer Idempotenzpruefung.
 */
@Data
@Table("idempotency_records")
public class IdempotencyRecord implements Persistable<String> {

    @Id
    @Column("idempotency_key")
    private String idempotencyKey;

    @Column("result")
    private String result;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Transient
    private boolean newEntity;

    /**
     * Liefert den Idempotenz-Key als Primarschluessel.
     */
    @Override
    public String getId() {
        return idempotencyKey;
    }

    /**
     * Markiert Entitaeten als neu fuer INSERT-Semantik.
     */
    @Override
    public boolean isNew() {
        return newEntity;
    }
}

