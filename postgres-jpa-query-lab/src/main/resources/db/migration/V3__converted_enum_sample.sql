CREATE TABLE converted_sample (
    id uuid PRIMARY KEY,
    status_code text
);

INSERT INTO converted_sample (id, status_code) VALUES
('30000000-0000-0000-0000-000000000001', 'N'),
('30000000-0000-0000-0000-000000000002', 'D'),
('30000000-0000-0000-0000-000000000003', NULL);

