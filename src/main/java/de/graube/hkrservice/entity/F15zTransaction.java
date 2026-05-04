package de.graube.hkrservice.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Persistierte Einzeltransaktion eines Jobs.
 */
@Data
@Table("f15z_transactions")
public class F15zTransaction implements Persistable<String> {

    @Id
    @Column("id")
    private String id;

    @Column("job_id")
    private String jobId;

    @Column("type")
    private String type;

    @Column("belegnummer")
    private String belegnummer;

    @Column("faelligkeit")
    private LocalDate faelligkeit;

    @Column("betrag")
    private BigDecimal betrag;

    @Column("iban")
    private String iban;

    @Column("bic")
    private String bic;

    @Column("titel")
    private String titel;

    @Column("objektkonto")
    private String objektkonto;

    @Column("status")
    private String status;

    @Column("error_message")
    private String errorMessage;

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