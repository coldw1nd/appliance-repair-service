CREATE TABLE IF NOT EXISTS app_user (
    id UUID PRIMARY KEY,
    email VARCHAR(254) NOT NULL UNIQUE,
    display_name VARCHAR(160) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    password_change_required BOOLEAN NOT NULL DEFAULT TRUE,
    failed_login_attempts INTEGER NOT NULL DEFAULT 0,
    locked_until TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS app_user_role (
    user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    role VARCHAR(32) NOT NULL,
    PRIMARY KEY (user_id, role)
);

CREATE TABLE IF NOT EXISTS refresh_session (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    replaced_by UUID,
    user_agent VARCHAR(500),
    ip_address VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL,
    last_used_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_refresh_user ON refresh_session(user_id);
CREATE INDEX IF NOT EXISTS idx_refresh_expires ON refresh_session(expires_at);

CREATE TABLE IF NOT EXISTS appliance_type (
    id UUID PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL UNIQUE,
    description VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

ALTER TABLE repair_requests ADD COLUMN IF NOT EXISTS request_uuid UUID;
UPDATE repair_requests SET request_uuid = gen_random_uuid() WHERE request_uuid IS NULL;
ALTER TABLE repair_requests ALTER COLUMN request_uuid SET DEFAULT gen_random_uuid();
ALTER TABLE repair_requests ALTER COLUMN request_uuid SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uq_repair_requests_request_uuid ON repair_requests(request_uuid);

ALTER TABLE repair_requests ADD COLUMN IF NOT EXISTS request_number VARCHAR(30);
UPDATE repair_requests SET request_number = 'ЗР-' || LPAD(id::text, GREATEST(6, LENGTH(id::text)), '0') WHERE request_number IS NULL;
ALTER TABLE repair_requests ALTER COLUMN request_number SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uq_repair_requests_request_number ON repair_requests(request_number);

ALTER TABLE repair_requests ADD COLUMN IF NOT EXISTS external_id VARCHAR(100);
CREATE UNIQUE INDEX IF NOT EXISTS uq_repair_requests_external_id ON repair_requests(external_id) WHERE external_id IS NOT NULL;

ALTER TABLE repair_requests ADD COLUMN IF NOT EXISTS appliance_type_id UUID;
ALTER TABLE repair_requests ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE repair_requests ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE repair_requests ADD COLUMN IF NOT EXISTS archived BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE repair_requests ADD COLUMN IF NOT EXISTS archived_at TIMESTAMP;
ALTER TABLE repair_requests ADD COLUMN IF NOT EXISTS archived_by UUID;
ALTER TABLE repair_requests ADD COLUMN IF NOT EXISTS created_by UUID;
ALTER TABLE repair_requests ADD COLUMN IF NOT EXISTS updated_by UUID;
ALTER TABLE repair_requests ADD COLUMN IF NOT EXISTS risk_level VARCHAR(32) NOT NULL DEFAULT 'LOW';

INSERT INTO appliance_type (id, code, name, description, active, created_at, updated_at, version) VALUES
    (gen_random_uuid(), 'WASHING_MACHINE', 'Стиральная машина', 'Стиральные машины и стирально-сушильные комплексы', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    (gen_random_uuid(), 'REFRIGERATOR', 'Холодильник', 'Холодильники и морозильные камеры', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    (gen_random_uuid(), 'VACUUM_CLEANER', 'Пылесос', 'Пылесосы и роботы-пылесосы', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    (gen_random_uuid(), 'DISHWASHER', 'Посудомоечная машина', 'Посудомоечные машины', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    (gen_random_uuid(), 'MICROWAVE', 'Микроволновая печь', 'Микроволновые печи и СВЧ', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    (gen_random_uuid(), 'TELEVISION', 'Телевизор', 'Телевизоры и панели', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    (gen_random_uuid(), 'ELECTRIC_STOVE', 'Электрическая плита', 'Электрические плиты и варочные панели', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    (gen_random_uuid(), 'DRYER', 'Сушильная машина', 'Сушильные машины', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    (gen_random_uuid(), 'COFFEE_MACHINE', 'Кофемашина', 'Кофемашины и кофеварки', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    (gen_random_uuid(), 'OVEN', 'Духовой шкаф', 'Духовые шкафы и мини-печи', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
ON CONFLICT (code) DO NOTHING;

UPDATE repair_requests r
SET appliance_type_id = t.id
FROM appliance_type t
WHERE r.appliance_type_id IS NULL AND LOWER(r.appliance_type) = LOWER(t.name);

UPDATE repair_requests
SET risk_level = CASE
    WHEN status IN ('COMPLETED', 'CANCELLED') THEN 'NOT_APPLICABLE'
    WHEN urgent = TRUE OR priority = 'URGENT' THEN 'CRITICAL'
    WHEN priority = 'HIGH' THEN 'HIGH'
    WHEN priority = 'MEDIUM' THEN 'MEDIUM'
    ELSE 'LOW'
END;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_repair_requests_appliance_type') THEN
        ALTER TABLE repair_requests ADD CONSTRAINT fk_repair_requests_appliance_type
            FOREIGN KEY (appliance_type_id) REFERENCES appliance_type(id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_repair_requests_archived_by') THEN
        ALTER TABLE repair_requests ADD CONSTRAINT fk_repair_requests_archived_by
            FOREIGN KEY (archived_by) REFERENCES app_user(id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_repair_requests_created_by') THEN
        ALTER TABLE repair_requests ADD CONSTRAINT fk_repair_requests_created_by
            FOREIGN KEY (created_by) REFERENCES app_user(id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_repair_requests_updated_by') THEN
        ALTER TABLE repair_requests ADD CONSTRAINT fk_repair_requests_updated_by
            FOREIGN KEY (updated_by) REFERENCES app_user(id);
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS repair_request_tag (
    repair_request_id UUID NOT NULL REFERENCES repair_requests(request_uuid) ON DELETE CASCADE,
    tag VARCHAR(50) NOT NULL,
    PRIMARY KEY (repair_request_id, tag)
);

CREATE INDEX IF NOT EXISTS idx_repair_request_tag_value ON repair_request_tag(tag);

CREATE TABLE IF NOT EXISTS repair_request_note (
    id UUID PRIMARY KEY,
    repair_request_id UUID NOT NULL REFERENCES repair_requests(request_uuid) ON DELETE CASCADE,
    author_id UUID NOT NULL REFERENCES app_user(id),
    body VARCHAR(4000) NOT NULL,
    pinned BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_repair_note_request ON repair_request_note(repair_request_id, pinned DESC, created_at DESC);

CREATE TABLE IF NOT EXISTS audit_event (
    id UUID PRIMARY KEY,
    actor_id UUID,
    actor_email VARCHAR(254),
    event_type VARCHAR(80) NOT NULL,
    entity_type VARCHAR(80),
    entity_id UUID,
    summary VARCHAR(1000) NOT NULL,
    correlation_id VARCHAR(100),
    ip_address VARCHAR(64),
    user_agent VARCHAR(500),
    occurred_at TIMESTAMPTZ NOT NULL,
    details_json TEXT
);

CREATE INDEX IF NOT EXISTS idx_audit_occurred ON audit_event(occurred_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_actor ON audit_event(actor_email);
CREATE INDEX IF NOT EXISTS idx_audit_entity ON audit_event(entity_type, entity_id);

CREATE INDEX IF NOT EXISTS idx_repair_requests_customer ON repair_requests(customer_id);
CREATE INDEX IF NOT EXISTS idx_repair_requests_type ON repair_requests(appliance_type_id);
CREATE INDEX IF NOT EXISTS idx_repair_requests_status ON repair_requests(status);
CREATE INDEX IF NOT EXISTS idx_repair_requests_priority ON repair_requests(priority);
CREATE INDEX IF NOT EXISTS idx_repair_requests_archived ON repair_requests(archived);
CREATE INDEX IF NOT EXISTS idx_repair_requests_risk ON repair_requests(risk_level);
CREATE INDEX IF NOT EXISTS idx_repair_requests_urgent ON repair_requests(urgent);
CREATE INDEX IF NOT EXISTS idx_repair_requests_planned_date ON repair_requests(planned_date);
CREATE INDEX IF NOT EXISTS idx_repair_requests_created_at ON repair_requests(created_at);

CREATE SEQUENCE IF NOT EXISTS repair_request_number_seq;
SELECT setval('repair_request_number_seq', GREATEST(COALESCE((SELECT MAX(id) FROM repair_requests), 0) + 1, 1), FALSE);

CREATE OR REPLACE FUNCTION maintain_repair_request_metadata() RETURNS TRIGGER AS $$
DECLARE
    request_number_value BIGINT;
BEGIN
    IF TG_OP = 'INSERT' THEN
        IF NEW.request_uuid IS NULL THEN
            NEW.request_uuid := gen_random_uuid();
        END IF;
        IF NEW.request_number IS NULL THEN
            request_number_value := nextval('repair_request_number_seq');
            NEW.request_number := 'ЗР-' || LPAD(request_number_value::TEXT, GREATEST(6, LENGTH(request_number_value::TEXT)), '0');
        END IF;
        IF NEW.updated_at IS NULL THEN
            NEW.updated_at := CURRENT_TIMESTAMP;
        END IF;
    ELSE
        NEW.updated_at := CURRENT_TIMESTAMP;
        NEW.version := OLD.version + 1;
    END IF;

    NEW.risk_level := CASE
        WHEN NEW.status IN ('COMPLETED', 'CANCELLED') THEN 'NOT_APPLICABLE'
        WHEN NEW.urgent = TRUE OR NEW.priority = 'URGENT' THEN 'CRITICAL'
        WHEN NEW.priority = 'HIGH' THEN 'HIGH'
        WHEN NEW.planned_date IS NOT NULL AND NEW.planned_date < CURRENT_DATE THEN 'HIGH'
        WHEN NEW.priority = 'MEDIUM' THEN 'MEDIUM'
        ELSE 'LOW'
    END;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_repair_request_metadata ON repair_requests;
CREATE TRIGGER trg_repair_request_metadata
    BEFORE INSERT OR UPDATE ON repair_requests
    FOR EACH ROW EXECUTE FUNCTION maintain_repair_request_metadata();
