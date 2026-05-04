package de.graube.hkrservice.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;

/**
 * Persistierte Buchung aus der fachlichen Verarbeitung.
 */
@Data
@Table("f15z_bookings")
public class F15zBooking implements Persistable<String> {

    @Id
    @Column("id")
    private String id;

    @Column("job_id")
    private String jobId;

    @Column("type")
    private String type;

    @Column("amount")
    private BigDecimal amount;

    @Column("belegnummer")
    private String belegnummer;

    @Column("status")
    private String status;

    @Transient
    private boolean newEntity;

    /**
     * Liefert die technische ID fuer Persistable.
     */
    @Override
    public String getId() {
        return id;
    }

    /**
     * Markiert Entitaeten als neu fuer INSERT-Semantik.
     */
    @Override
    public boolean isNew() {
        return newEntity;
    }
}