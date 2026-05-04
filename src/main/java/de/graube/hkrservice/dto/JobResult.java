package de.graube.hkrservice.dto;

import lombok.Data;

import java.util.List;

/**
 * Transportobjekt fuer aggregierte Jobergebnisse.
 */
@Data
public class JobResult {

    private String jobId;
    private JobStatus status;

    private int total;
    private int success;
    private int error;

    private List<String> errorMessages;

    /**
     * Standardkonstruktor fuer Serialisierung.
     */
    public JobResult() {}

    /**
     * Erstellt ein Ergebnis fuer einen Jobstatus.
     *
     * @param jobId Job-ID
     * @param status Jobstatus
     */
    public JobResult(String jobId, JobStatus status) {
        this.jobId = jobId;
        this.status = status;
    }

}