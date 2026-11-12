-- =========================================================================
-- FLYWAY MIGRATION V2: SEED DEFAULT DATA RETENTION POLICIES
-- =========================================================================

INSERT INTO data_retention_policy (
    id, data_category, table_name, retention_period_days, retention_anchor_column, archive_before_delete, archive_storage, batch_size, enabled
) VALUES 
('11111111-1111-1111-1111-111111111101', 'ERROR_LOGS', 'out_message_error', 365, 'occurred_at', TRUE, 'SUPABASE_COLD', 5000, TRUE),
('11111111-1111-1111-1111-111111111102', 'EXECUTION_LOGS', 'out_message_execution', 180, 'created_at', TRUE, 'SUPABASE_COLD', 5000, TRUE),
('11111111-1111-1111-1111-111111111103', 'DELIVERY_LOGS', 'out_message_delivery_log', 90, 'created_at', TRUE, 'SUPABASE_COLD', 5000, TRUE),
('11111111-1111-1111-1111-111111111104', 'AUDIT_LOGS', 'out_message_audit', 730, 'created_at', TRUE, 'SUPABASE_COLD', 5000, TRUE),
('11111111-1111-1111-1111-111111111105', 'SMS_HISTORY', 'out_message_sms', 180, 'created_at', TRUE, 'SUPABASE_COLD', 5000, TRUE),
('11111111-1111-1111-1111-111111111106', 'EMAIL_HISTORY', 'out_message_email', 180, 'created_at', TRUE, 'SUPABASE_COLD', 5000, TRUE),
('11111111-1111-1111-1111-111111111107', 'DOCUMENT_METADATA', 'out_message_document', 365, 'created_at', TRUE, 'SUPABASE_COLD', 5000, TRUE),
('11111111-1111-1111-1111-111111111108', 'REPORT_HISTORY', 'out_message_report', 180, 'created_at', TRUE, 'SUPABASE_COLD', 5000, TRUE),
('11111111-1111-1111-1111-111111111109', 'TRANSACTION_MASTER', 'out_message', 730, 'created_at', TRUE, 'SUPABASE_COLD', 2000, TRUE);
