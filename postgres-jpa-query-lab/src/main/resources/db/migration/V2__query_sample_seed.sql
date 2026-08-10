INSERT INTO query_sample (
    id, name, description, status, external_id, created_at, processed_at,
    local_date, amount, active, metadata, tags, related_ids
) VALUES
(
    '00000000-0000-0000-0000-000000000001', 'sql-null', NULL, 'NEW', NULL,
    '2026-01-01T00:00:00Z', NULL, NULL, NULL, NULL, NULL, NULL, NULL
),
(
    '00000000-0000-0000-0000-000000000002', 'empty-values', '', 'PROCESSING',
    '10000000-0000-0000-0000-000000000002', '2026-02-01T10:15:30Z',
    '2026-02-02T12:00:00+03:00', '2026-02-02', 0, false, '{}'::jsonb,
    ARRAY[]::text[], ARRAY[]::uuid[]
),
(
    '00000000-0000-0000-0000-000000000003', 'ordinary', 'value', 'DONE',
    '10000000-0000-0000-0000-000000000003', '2026-03-01T10:15:30Z',
    '2026-03-02T12:00:00Z', '2026-03-02', 42.50, true,
    '{"present":"yes"}'::jsonb, ARRAY['alpha','beta']::text[],
    ARRAY['20000000-0000-0000-0000-000000000001'::uuid]
),
(
    '00000000-0000-0000-0000-000000000004', 'json-empty-array', NULL, 'NEW',
    '10000000-0000-0000-0000-000000000004', '2026-04-01T00:00:00Z', NULL,
    NULL, NULL, NULL, '[]'::jsonb, ARRAY['gamma']::text[],
    ARRAY['20000000-0000-0000-0000-000000000002'::uuid,
          '20000000-0000-0000-0000-000000000003'::uuid]
);

