CREATE TABLE IF NOT EXISTS f15z_jobs (
    job_id VARCHAR(80) PRIMARY KEY,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    version BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS f15z_transactions (
    id VARCHAR(80) PRIMARY KEY,
    job_id VARCHAR(80) NOT NULL,
    type VARCHAR(8),
    belegnummer VARCHAR(40) NOT NULL,
    faelligkeit DATE,
    betrag DECIMAL(15,2),
    iban VARCHAR(34),
    bic VARCHAR(11),
    titel VARCHAR(64),
    objektkonto VARCHAR(64),
    status VARCHAR(32) NOT NULL,
    error_message VARCHAR(255),
    CONSTRAINT fk_f15z_transactions_job FOREIGN KEY (job_id) REFERENCES f15z_jobs(job_id) ON DELETE CASCADE,
    CONSTRAINT uk_f15z_job_belegnummer UNIQUE (job_id, belegnummer)
);

CREATE INDEX IF NOT EXISTS idx_f15z_transactions_job_id ON f15z_transactions(job_id);

CREATE TABLE IF NOT EXISTS idempotency_records (
    idempotency_key VARCHAR(120) PRIMARY KEY,
    result VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS f15z_bookings (
    id VARCHAR(80) PRIMARY KEY,
    job_id VARCHAR(80) NOT NULL,
    type VARCHAR(8),
    amount DECIMAL(15,2),
    belegnummer VARCHAR(40),
    status VARCHAR(32)
);

