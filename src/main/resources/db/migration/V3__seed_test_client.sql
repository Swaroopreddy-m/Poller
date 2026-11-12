-- Seed a default test client and active API credentials
-- Client Code: CLIENT_TEST_01
-- API Key Prefix: pk
-- Secret: test_secret_key_12345
-- Full X-API-KEY: pk_test_secret_key_12345

INSERT INTO client_master (id, client_code, client_name, status, tier, max_rate_limit_per_min)
VALUES (
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
    'CLIENT_TEST_01',
    'Swaroop Enterprise Test Client',
    'ACTIVE',
    'ENTERPRISE',
    1000
) ON CONFLICT (client_code) DO NOTHING;

INSERT INTO client_api_credentials (
    id, client_id, api_key_prefix, api_key_hash, signing_secret, status
) VALUES (
    'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
    'pk',
    'PcgH3QmRiJ+J93zu2xz8EO8gK0J8lh3E2sCqjM4mNEw=',
    'test_signing_secret_xyz',
    'ACTIVE'
) ON CONFLICT (api_key_prefix) DO NOTHING;

-- Enable Email, SMS and PDF services for CLIENT_TEST_01
INSERT INTO client_service_config (client_id, service_type, is_enabled, max_batch_size, allowed_file_types)
VALUES 
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'EMAIL', TRUE, 500, 'PDF,XLSX,CSV'),
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'SMS', TRUE, 500, 'PDF,XLSX,CSV'),
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'PDF', TRUE, 100, 'PDF')
ON CONFLICT (client_id, service_type) DO UPDATE SET is_enabled = TRUE;
