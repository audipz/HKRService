package de.graube.hkrservice.repositorys;

import de.graube.hkrservice.dto.JobStatus;
import de.graube.hkrservice.entity.F15zJob;
import de.graube.hkrservice.entity.F15zTransaction;
import de.graube.hkrservice.entity.IdempotencyRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class R2dbcRepositoryIntegrationTest {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private F15zTransactionRepository transactionRepository;

    @Autowired
    private IdempotencyRepository idempotencyRepository;

    @Test
    void optimisticLockingRejectsStaleJobUpdate() {
        String jobId = "JOB-LOCK-" + UUID.randomUUID();

        F15zJob created = createJob(jobId);
        jobRepository.save(created).block();

        F15zJob firstRead = jobRepository.findById(jobId).block();
        F15zJob secondRead = jobRepository.findById(jobId).block();

        if (firstRead == null || secondRead == null) {
            throw new IllegalStateException("Expected existing job for optimistic locking test");
        }

        firstRead.setStatus(JobStatus.COLLECTING);
        jobRepository.save(firstRead).block();

        secondRead.setStatus(JobStatus.PROCESSING);

        StepVerifier.create(jobRepository.save(secondRead))
                .expectError(OptimisticLockingFailureException.class)
                .verify();
    }

    @Test
    void uniqueConstraintRejectsDuplicateBelegnummerPerJob() {
        String jobId = "JOB-UNIQUE-" + UUID.randomUUID();
        jobRepository.save(createJob(jobId)).block();

        F15zTransaction first = createTransaction(jobId, "BLG-DUP", "T-1");
        F15zTransaction second = createTransaction(jobId, "BLG-DUP", "T-2");

        transactionRepository.save(first).block();

        StepVerifier.create(transactionRepository.save(second))
                .expectError(DataIntegrityViolationException.class)
                .verify();
    }

    @Test
    void idempotencyPrimaryKeyRejectsDuplicateKey() {
        String key = "IDEMP-" + UUID.randomUUID();

        IdempotencyRecord first = createIdempotencyRecord(key, "OK");
        IdempotencyRecord second = createIdempotencyRecord(key, "SHOULD-FAIL");

        idempotencyRepository.save(first).block();

        StepVerifier.create(idempotencyRepository.save(second))
                .expectError(DataIntegrityViolationException.class)
                .verify();
    }

    private F15zJob createJob(String jobId) {
        F15zJob job = new F15zJob();
        job.setJobId(jobId);
        job.setStatus(JobStatus.CREATED);
        job.setCreatedAt(LocalDateTime.now());
        return job;
    }

    private F15zTransaction createTransaction(String jobId, String belegnummer, String suffix) {
        F15zTransaction tx = new F15zTransaction();
        tx.setId("TX-" + suffix + "-" + UUID.randomUUID());
        tx.setJobId(jobId);
        tx.setType("AUSZ");
        tx.setBelegnummer(belegnummer);
        tx.setFaelligkeit(LocalDate.of(2026, 5, 1));
        tx.setBetrag(new BigDecimal("10.00"));
        tx.setIban("DE89370400440532013000");
        tx.setBic("COBADEFFXXX");
        tx.setTitel("TEST");
        tx.setObjektkonto("OBJ");
        tx.setStatus("NEW");
        tx.setNewEntity(true);
        return tx;
    }

    private IdempotencyRecord createIdempotencyRecord(String key, String result) {
        IdempotencyRecord record = new IdempotencyRecord();
        record.setIdempotencyKey(key);
        record.setResult(result);
        record.setCreatedAt(LocalDateTime.now());
        record.setNewEntity(true);
        return record;
    }
}

