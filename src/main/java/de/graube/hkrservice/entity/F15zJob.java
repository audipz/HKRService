package de.graube.hkrservice.entity;

import de.graube.hkrservice.dto.JobStatus;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

/**
 * Persistierte Job-Entitaet fuer den F15Z-Prozess.
 */
@Data
@Table("f15z_jobs")
public class F15zJob {

    @Id
    @Column("job_id")
    private String jobId;

    @Column("status")
    private JobStatus status;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Version
    @Column("version")
    private Long version;
}