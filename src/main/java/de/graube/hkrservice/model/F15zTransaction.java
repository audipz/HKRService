package de.graube.hkrservice.model;


import de.graube.hkrservice.dto.JobStatus;
import de.graube.hkrservice.dto.TransaktionStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * API-Modell fuer eine F15Z-Transaktion.
 */
@Data
public class F15zTransaction {

    public VslType type;
    public String belegnummer;
    public LocalDate faelligkeit;
    public BigDecimal betrag;

    public String iban;
    public String bic;

    public String titel;
    public String objektkonto;
    private TransaktionStatus status;
    private String errorMessage;

}