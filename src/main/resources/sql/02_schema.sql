CREATE TABLE IF NOT EXISTS customers (
    id BIGSERIAL PRIMARY KEY,
    full_name VARCHAR(120) NOT NULL,
    phone VARCHAR(30) NOT NULL UNIQUE,
    email VARCHAR(150) NOT NULL UNIQUE,
    address VARCHAR(250) NOT NULL
);

CREATE TABLE IF NOT EXISTS repair_requests (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    appliance_type VARCHAR(80) NOT NULL,
    appliance_brand VARCHAR(80) NOT NULL,
    issue_title VARCHAR(200) NOT NULL,
    issue_description VARCHAR(1000) NOT NULL,
    status VARCHAR(30) NOT NULL,
    priority VARCHAR(20) NOT NULL,
    estimated_cost NUMERIC(12, 2) NOT NULL CHECK (estimated_cost >= 0 AND estimated_cost <= 1000000),
    planned_date DATE,
    urgent BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_repair_requests_customer FOREIGN KEY (customer_id) REFERENCES customers(id),
    CONSTRAINT chk_repair_status CHECK (status IN ('NEW', 'DIAGNOSING', 'REPAIRING', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT chk_repair_priority CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'URGENT')),
    CONSTRAINT chk_urgent_priority CHECK (urgent = FALSE OR priority IN ('HIGH', 'URGENT'))
);

CREATE INDEX IF NOT EXISTS idx_repair_requests_customer ON repair_requests(customer_id);
CREATE INDEX IF NOT EXISTS idx_repair_requests_status ON repair_requests(status);
CREATE INDEX IF NOT EXISTS idx_repair_requests_priority ON repair_requests(priority);
