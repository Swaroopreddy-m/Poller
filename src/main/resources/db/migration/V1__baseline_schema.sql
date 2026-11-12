-- =========================================================================
-- FLYWAY MIGRATION V1: BASELINE ENTERPRISE SCHEMA FOR INDEPENDENT PLATFORM
-- =========================================================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- =========================================================================
-- 1. CLIENT & SECURITY MANAGEMENT
-- =========================================================================

CREATE TABLE client_master (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_code VARCHAR(32) NOT NULL UNIQUE,
    client_name VARCHAR(128) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'SUSPENDED', 'DECOMMISSIONED')),
    tier VARCHAR(16) NOT NULL DEFAULT 'STANDARD' CHECK (tier IN ('FREE', 'STANDARD', 'ENTERPRISE')),
    max_rate_limit_per_min INT NOT NULL DEFAULT 600,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE client_api_credentials (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id UUID NOT NULL REFERENCES client_master(id) ON DELETE CASCADE,
    api_key_prefix VARCHAR(16) NOT NULL UNIQUE,
    api_key_hash VARCHAR(255) NOT NULL,
    signing_secret VARCHAR(255) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'REVOKED')),
    expires_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_used_at TIMESTAMPTZ NULL
);
CREATE INDEX idx_client_cred_lookup ON client_api_credentials(api_key_prefix) WHERE status = 'ACTIVE';

CREATE TABLE client_service_config (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id UUID NOT NULL REFERENCES client_master(id) ON DELETE CASCADE,
    service_type VARCHAR(32) NOT NULL CHECK (service_type IN ('EMAIL', 'SMS', 'PDF', 'EXCEL', 'CSV', 'REPORT')),
    is_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    max_batch_size INT NOT NULL DEFAULT 100,
    allowed_file_types VARCHAR(64) DEFAULT 'PDF,XLSX,CSV',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_client_service UNIQUE (client_id, service_type)
);

CREATE TABLE client_provider_config (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id UUID NOT NULL REFERENCES client_master(id) ON DELETE CASCADE,
    service_type VARCHAR(32) NOT NULL CHECK (service_type IN ('EMAIL', 'SMS')),
    provider_name VARCHAR(64) NOT NULL,
    priority INT NOT NULL DEFAULT 1,
    configuration_payload JSONB NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_client_provider_priority UNIQUE (client_id, service_type, priority)
);

-- =========================================================================
-- 2. TEMPLATES & REQUEST DEFINITIONS
-- =========================================================================

CREATE TABLE template_master (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id UUID NOT NULL REFERENCES client_master(id) ON DELETE RESTRICT,
    template_code VARCHAR(64) NOT NULL,
    template_type VARCHAR(32) NOT NULL CHECK (template_type IN ('EMAIL', 'SMS', 'PDF', 'STATEMENT', 'REPORT')),
    name VARCHAR(128) NOT NULL,
    description TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_client_template UNIQUE (client_id, template_code)
);

CREATE TABLE template_version (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    template_id UUID NOT NULL REFERENCES template_master(id) ON DELETE CASCADE,
    version_number INT NOT NULL,
    engine VARCHAR(32) NOT NULL DEFAULT 'FREEMARKER' CHECK (engine IN ('FREEMARKER', 'THYMELEAF')),
    subject_template TEXT NULL,
    content_template TEXT NOT NULL,
    schema_definition JSONB NOT NULL,
    is_published BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(64) NOT NULL,
    CONSTRAINT uq_template_version UNIQUE (template_id, version_number)
);

CREATE TABLE request_type_master (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    request_type_code VARCHAR(64) NOT NULL UNIQUE,
    description VARCHAR(255) NOT NULL,
    validation_schema JSONB NOT NULL,
    version INT NOT NULL DEFAULT 1,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- =========================================================================
-- 3. CENTRAL TRANSACTION MASTER (OUT_MESSAGE)
-- =========================================================================

CREATE TABLE out_message (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    ref_no VARCHAR(36) NOT NULL,
    client_id UUID NOT NULL REFERENCES client_master(id) ON DELETE RESTRICT,
    request_type VARCHAR(64) NOT NULL,
    request_source VARCHAR(64) NOT NULL DEFAULT 'REST_API',
    idempotency_key VARCHAR(128) NULL,
    processing_mode VARCHAR(16) NOT NULL CHECK (processing_mode IN ('SYNC', 'ASYNC')),
    priority INT NOT NULL DEFAULT 5 CHECK (priority BETWEEN 1 AND 10),
    
    -- Precise Service Counters
    sms_count INT NOT NULL DEFAULT 0,
    email_count INT NOT NULL DEFAULT 0,
    pdf_count INT NOT NULL DEFAULT 0,
    excel_count INT NOT NULL DEFAULT 0,
    csv_count INT NOT NULL DEFAULT 0,
    report_count INT NOT NULL DEFAULT 0,
    total_service_count INT NOT NULL DEFAULT 0,
    
    -- Processing Metrics
    success_count INT NOT NULL DEFAULT 0,
    failed_count INT NOT NULL DEFAULT 0,
    in_progress_count INT NOT NULL DEFAULT 0,
    completed_count INT NOT NULL DEFAULT 0,
    expired_count INT NOT NULL DEFAULT 0,
    paused_count INT NOT NULL DEFAULT 0,
    
    -- Status Lifecycle
    overall_status VARCHAR(8) NOT NULL CHECK (overall_status IN ('IP', 'S', 'F', 'P', 'E', 'C')),
    completion_outcome VARCHAR(16) NULL CHECK (completion_outcome IN ('FULL_SUCCESS', 'PARTIAL_SUCCESS', 'TOTAL_FAILURE')),
    
    -- Payload Reference
    request_payload_reference VARCHAR(512) NULL,
    
    -- Temporal Milestones
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at TIMESTAMPTZ NULL,
    completed_at TIMESTAMPTZ NULL,
    last_updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMPTZ NOT NULL,
    retention_until TIMESTAMPTZ NOT NULL,
    archived_at TIMESTAMPTZ NULL,

    PRIMARY KEY (id, created_at),
    CONSTRAINT uq_out_message_ref UNIQUE (ref_no, created_at)
) PARTITION BY RANGE (created_at);

CREATE INDEX idx_out_msg_ref_lookup ON out_message(ref_no);
CREATE INDEX idx_out_msg_client_status ON out_message(client_id, overall_status, created_at DESC);
CREATE INDEX idx_out_msg_idempotency ON out_message(client_id, idempotency_key) WHERE idempotency_key IS NOT NULL;
CREATE INDEX idx_out_msg_retention ON out_message(retention_until) WHERE archived_at IS NULL;

-- =========================================================================
-- 4. CHILD OPERATIONAL SERVICES (PARTITIONED)
-- =========================================================================

CREATE TABLE out_message_sms (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    out_message_id BIGINT NOT NULL,
    ref_no VARCHAR(36) NOT NULL,
    client_id UUID NOT NULL,
    recipient_phone VARCHAR(32) NOT NULL,
    template_id UUID NULL,
    rendered_content TEXT NOT NULL,
    provider_name VARCHAR(64) NULL,
    provider_message_id VARCHAR(128) NULL,
    status VARCHAR(8) NOT NULL CHECK (status IN ('IP', 'S', 'F', 'P', 'E')),
    retry_count INT NOT NULL DEFAULT 0,
    max_retries INT NOT NULL DEFAULT 3,
    scheduled_at TIMESTAMPTZ NULL,
    sent_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    retention_until TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (id, created_at)
) PARTITION BY RANGE (created_at);
CREATE INDEX idx_sms_ref_no ON out_message_sms(ref_no);
CREATE INDEX idx_sms_status ON out_message_sms(status) WHERE status IN ('IP', 'P');

CREATE TABLE out_message_email (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    out_message_id BIGINT NOT NULL,
    ref_no VARCHAR(36) NOT NULL,
    client_id UUID NOT NULL,
    recipient_email VARCHAR(255) NOT NULL,
    cc_list TEXT NULL,
    bcc_list TEXT NULL,
    subject VARCHAR(512) NOT NULL,
    rendered_body TEXT NOT NULL,
    is_html BOOLEAN NOT NULL DEFAULT TRUE,
    provider_name VARCHAR(64) NULL,
    provider_message_id VARCHAR(128) NULL,
    status VARCHAR(8) NOT NULL CHECK (status IN ('IP', 'S', 'F', 'P', 'E')),
    retry_count INT NOT NULL DEFAULT 0,
    max_retries INT NOT NULL DEFAULT 3,
    sent_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    retention_until TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (id, created_at)
) PARTITION BY RANGE (created_at);
CREATE INDEX idx_email_ref_no ON out_message_email(ref_no);
CREATE INDEX idx_email_status ON out_message_email(status) WHERE status IN ('IP', 'P');

CREATE TABLE out_message_document (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    out_message_id BIGINT NOT NULL,
    ref_no VARCHAR(36) NOT NULL,
    client_id UUID NOT NULL,
    document_type VARCHAR(16) NOT NULL CHECK (document_type IN ('PDF', 'EXCEL', 'CSV', 'STATEMENT')),
    file_name VARCHAR(255) NOT NULL,
    storage_bucket VARCHAR(128) NOT NULL,
    storage_path VARCHAR(512) NOT NULL,
    mime_type VARCHAR(64) NOT NULL,
    file_size_bytes BIGINT NULL,
    checksum_sha256 VARCHAR(64) NULL,
    status VARCHAR(8) NOT NULL CHECK (status IN ('IP', 'S', 'F', 'E')),
    generated_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    retention_until TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (id, created_at)
) PARTITION BY RANGE (created_at);
CREATE INDEX idx_doc_ref_no ON out_message_document(ref_no);

CREATE TABLE out_message_attachment (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    out_message_id BIGINT NOT NULL,
    ref_no VARCHAR(36) NOT NULL,
    email_id BIGINT NULL,
    document_id BIGINT NULL,
    attachment_name VARCHAR(255) NOT NULL,
    storage_bucket VARCHAR(128) NOT NULL,
    storage_path VARCHAR(512) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id, created_at)
) PARTITION BY RANGE (created_at);

-- =========================================================================
-- 5. REPORTING ENGINE TABLES
-- =========================================================================

CREATE TABLE report_master (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id UUID NOT NULL REFERENCES client_master(id) ON DELETE RESTRICT,
    report_code VARCHAR(64) NOT NULL,
    report_name VARCHAR(128) NOT NULL,
    description TEXT,
    query_template TEXT NOT NULL,
    supported_formats VARCHAR(64) NOT NULL DEFAULT 'PDF,EXCEL,CSV',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_client_report UNIQUE (client_id, report_code)
);

CREATE TABLE report_parameter_config (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    report_id UUID NOT NULL REFERENCES report_master(id) ON DELETE CASCADE,
    parameter_name VARCHAR(64) NOT NULL,
    data_type VARCHAR(32) NOT NULL CHECK (data_type IN ('STRING', 'INTEGER', 'DATE', 'BOOLEAN')),
    is_mandatory BOOLEAN NOT NULL DEFAULT TRUE,
    default_value VARCHAR(128) NULL,
    CONSTRAINT uq_report_param UNIQUE (report_id, parameter_name)
);

CREATE TABLE out_message_report (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    out_message_id BIGINT NOT NULL,
    ref_no VARCHAR(36) NOT NULL,
    client_id UUID NOT NULL,
    report_id UUID NOT NULL REFERENCES report_master(id),
    requested_format VARCHAR(16) NOT NULL CHECK (requested_format IN ('PDF', 'EXCEL', 'CSV')),
    filter_parameters JSONB NOT NULL,
    document_id BIGINT NULL,
    status VARCHAR(8) NOT NULL CHECK (status IN ('IP', 'S', 'F', 'E')),
    generated_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    retention_until TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (id, created_at)
) PARTITION BY RANGE (created_at);
CREATE INDEX idx_report_ref_no ON out_message_report(ref_no);

-- =========================================================================
-- 6. EXECUTION, AUDIT, DELIVERY & ERROR LOGS (PARTITIONED)
-- =========================================================================

CREATE TABLE out_message_execution (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    out_message_id BIGINT NOT NULL,
    ref_no VARCHAR(36) NOT NULL,
    client_id UUID NOT NULL,
    service_type VARCHAR(32) NOT NULL,
    operation_id BIGINT NOT NULL,
    attempt_number INT NOT NULL,
    worker_instance_id VARCHAR(64) NOT NULL,
    execution_time_ms BIGINT NOT NULL,
    status VARCHAR(8) NOT NULL CHECK (status IN ('S', 'F')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    retention_until TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (id, created_at)
) PARTITION BY RANGE (created_at);
CREATE INDEX idx_exec_ref_no ON out_message_execution(ref_no);

CREATE TABLE out_message_error (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    out_message_id BIGINT NOT NULL,
    ref_no VARCHAR(36) NOT NULL,
    client_id UUID NOT NULL,
    service_type VARCHAR(32) NOT NULL,
    operation_id BIGINT NULL,
    error_code VARCHAR(64) NOT NULL,
    error_category VARCHAR(32) NOT NULL CHECK (error_category IN ('VALIDATION', 'SECURITY', 'PROVIDER', 'SYSTEM', 'STORAGE', 'TIMEOUT')),
    error_message VARCHAR(1024) NOT NULL,
    error_details JSONB NULL,
    retryable BOOLEAN NOT NULL DEFAULT FALSE,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMPTZ NULL,
    retention_until TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (id, created_at)
) PARTITION BY RANGE (created_at);
CREATE INDEX idx_err_ref_no ON out_message_error(ref_no);
CREATE INDEX idx_err_retention ON out_message_error(retention_until);

CREATE TABLE out_message_delivery_log (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    out_message_id BIGINT NOT NULL,
    ref_no VARCHAR(36) NOT NULL,
    client_id UUID NOT NULL,
    service_type VARCHAR(32) NOT NULL,
    provider_name VARCHAR(64) NOT NULL,
    provider_event_type VARCHAR(64) NOT NULL,
    raw_event_payload JSONB NULL,
    received_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    retention_until TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (id, created_at)
) PARTITION BY RANGE (created_at);
CREATE INDEX idx_deliv_ref_no ON out_message_delivery_log(ref_no);

CREATE TABLE out_message_audit (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    out_message_id BIGINT NOT NULL,
    ref_no VARCHAR(36) NOT NULL,
    client_id UUID NOT NULL,
    action VARCHAR(64) NOT NULL,
    from_status VARCHAR(8) NULL,
    to_status VARCHAR(8) NOT NULL,
    reason VARCHAR(255) NULL,
    initiated_by VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    retention_until TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (id, created_at)
) PARTITION BY RANGE (created_at);
CREATE INDEX idx_audit_ref_no ON out_message_audit(ref_no);

-- =========================================================================
-- 7. INFRASTRUCTURE: TRANSACTIONAL OUTBOX & IDEMPOTENCY
-- =========================================================================

CREATE TABLE outbox_event (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    ref_no VARCHAR(36) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    routing_key VARCHAR(128) NOT NULL,
    payload JSONB NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'PUBLISHED', 'FAILED')),
    retry_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMPTZ NULL
);
CREATE INDEX idx_outbox_pending ON outbox_event(created_at) WHERE status = 'PENDING';

CREATE TABLE idempotency_record (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id UUID NOT NULL REFERENCES client_master(id),
    idempotency_key VARCHAR(128) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    ref_no VARCHAR(36) NOT NULL,
    response_code INT NOT NULL,
    response_payload JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_client_idempotency UNIQUE (client_id, idempotency_key)
);
CREATE INDEX idx_idempotency_expiry ON idempotency_record(expires_at);

-- =========================================================================
-- 8. DATA LIFECYCLE & RETENTION MANAGEMENT
-- =========================================================================

CREATE TABLE data_retention_policy (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    data_category VARCHAR(64) NOT NULL UNIQUE,
    table_name VARCHAR(64) NOT NULL,
    retention_period_days INT NOT NULL CHECK (retention_period_days > 0),
    retention_anchor_column VARCHAR(64) NOT NULL DEFAULT 'occurred_at',
    archive_before_delete BOOLEAN NOT NULL DEFAULT TRUE,
    archive_storage VARCHAR(32) NOT NULL DEFAULT 'SUPABASE_COLD' CHECK (archive_storage IN ('SUPABASE_COLD', 'S3_GLACIER', 'NONE')),
    batch_size INT NOT NULL DEFAULT 5000,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    last_execution_at TIMESTAMPTZ NULL,
    next_execution_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE data_cleanup_execution (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    policy_id UUID NOT NULL REFERENCES data_retention_policy(id),
    table_name VARCHAR(64) NOT NULL,
    records_evaluated BIGINT NOT NULL DEFAULT 0,
    records_archived BIGINT NOT NULL DEFAULT 0,
    records_deleted BIGINT NOT NULL DEFAULT 0,
    records_failed BIGINT NOT NULL DEFAULT 0,
    execution_status VARCHAR(16) NOT NULL CHECK (execution_status IN ('SUCCESS', 'PARTIAL_FAIL', 'FAILED')),
    execution_error TEXT NULL,
    started_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMPTZ NULL
);

CREATE TABLE data_archive_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cleanup_execution_id UUID NOT NULL REFERENCES data_cleanup_execution(id),
    table_name VARCHAR(64) NOT NULL,
    ref_no VARCHAR(36) NOT NULL,
    archive_location VARCHAR(512) NOT NULL,
    record_hash VARCHAR(64) NOT NULL,
    archived_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_archive_ref_no ON data_archive_history(ref_no);

-- Default Monthly Partitions for Initial Window
CREATE TABLE out_message_y2026m11 PARTITION OF out_message
    FOR VALUES FROM ('2026-11-01 00:00:00+00') TO ('2026-12-01 00:00:00+00');
CREATE TABLE out_message_sms_y2026m11 PARTITION OF out_message_sms
    FOR VALUES FROM ('2026-11-01 00:00:00+00') TO ('2026-12-01 00:00:00+00');
CREATE TABLE out_message_email_y2026m11 PARTITION OF out_message_email
    FOR VALUES FROM ('2026-11-01 00:00:00+00') TO ('2026-12-01 00:00:00+00');
CREATE TABLE out_message_document_y2026m11 PARTITION OF out_message_document
    FOR VALUES FROM ('2026-11-01 00:00:00+00') TO ('2026-12-01 00:00:00+00');
CREATE TABLE out_message_error_y2026m11 PARTITION OF out_message_error
    FOR VALUES FROM ('2026-11-01 00:00:00+00') TO ('2026-12-01 00:00:00+00');
CREATE TABLE out_message_execution_y2026m11 PARTITION OF out_message_execution
    FOR VALUES FROM ('2026-11-01 00:00:00+00') TO ('2026-12-01 00:00:00+00');
CREATE TABLE out_message_delivery_log_y2026m11 PARTITION OF out_message_delivery_log
    FOR VALUES FROM ('2026-11-01 00:00:00+00') TO ('2026-12-01 00:00:00+00');
CREATE TABLE out_message_audit_y2026m11 PARTITION OF out_message_audit
    FOR VALUES FROM ('2026-11-01 00:00:00+00') TO ('2026-12-01 00:00:00+00');
CREATE TABLE out_message_report_y2026m11 PARTITION OF out_message_report
    FOR VALUES FROM ('2026-11-01 00:00:00+00') TO ('2026-12-01 00:00:00+00');
CREATE TABLE out_message_attachment_y2026m11 PARTITION OF out_message_attachment
    FOR VALUES FROM ('2026-11-01 00:00:00+00') TO ('2026-12-01 00:00:00+00');
