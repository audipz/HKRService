package de.graube.hkrservice.dto;

/**
 * Lebenszyklusstatus eines F15Z-Jobs.
 */
public enum JobStatus {
    CREATED,
    COLLECTING,
    PROCESSING,
    EXPORTED,
    SENT,
    COMPLETED,
    FAILED
}
