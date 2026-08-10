-- Hibernate PostgreSQLEnumJdbcType derives the unquoted SQL type name from
-- QueryStatus.simpleName, which PostgreSQL folds to "querystatus".
CREATE TYPE querystatus AS ENUM ('NEW', 'PROCESSING', 'DONE');

CREATE TABLE query_sample (
    id uuid PRIMARY KEY,
    name text NOT NULL,
    description text,
    status querystatus NOT NULL,
    external_id uuid,
    created_at timestamptz NOT NULL,
    processed_at timestamptz,
    local_date date,
    amount numeric(19, 4),
    active boolean,
    metadata jsonb,
    tags text[],
    related_ids uuid[]
);

CREATE INDEX idx_query_sample_external_id ON query_sample (external_id);
CREATE INDEX idx_query_sample_status ON query_sample (status);
CREATE INDEX idx_query_sample_created_at ON query_sample (created_at);
CREATE INDEX idx_query_sample_tags_gin ON query_sample USING gin (tags);
CREATE INDEX idx_query_sample_related_ids_gin ON query_sample USING gin (related_ids);
