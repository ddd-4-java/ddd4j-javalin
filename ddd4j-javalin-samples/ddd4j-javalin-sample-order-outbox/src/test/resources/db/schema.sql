-- Order Outbox 示例 PostgreSQL 表结构（合并自 ddd4j-sample-order-jdbc 的
-- V1__sample_order_outbox.sql + V2__sample_order_outbox_delivery_lease.sql）

CREATE TABLE IF NOT EXISTS sample_orders (
    id VARCHAR(64) PRIMARY KEY,
    order_no VARCHAR(64) NOT NULL UNIQUE,
    buyer_id VARCHAR(64) NOT NULL,
    buyer_name VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL,
    total_amount NUMERIC(19, 2) NOT NULL
);

CREATE TABLE IF NOT EXISTS sample_order_lines (
    id VARCHAR(64) PRIMARY KEY,
    order_id VARCHAR(64) NOT NULL REFERENCES sample_orders (id) ON DELETE CASCADE,
    goods_id VARCHAR(64) NOT NULL,
    goods_name VARCHAR(255) NOT NULL,
    quantity INTEGER NOT NULL,
    unit_price NUMERIC(19, 2) NOT NULL,
    currency VARCHAR(8) NOT NULL
);

CREATE TABLE IF NOT EXISTS sample_order_read_models (
    id VARCHAR(64) PRIMARY KEY,
    order_no VARCHAR(64) NOT NULL UNIQUE,
    buyer_id VARCHAR(64) NOT NULL,
    buyer_name VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL,
    total_amount NUMERIC(19, 2) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_sample_order_read_models_query
    ON sample_order_read_models (buyer_id, status, order_no);

CREATE TABLE IF NOT EXISTS sample_order_outbox (
    id VARCHAR(64) PRIMARY KEY,
    aggregate_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    payload JSONB NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(16) NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    last_error TEXT,
    published_at TIMESTAMPTZ,
    available_at TIMESTAMPTZ NOT NULL,
    lease_owner VARCHAR(255),
    lease_until TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_sample_order_outbox_pending
    ON sample_order_outbox (status, occurred_at);

CREATE INDEX IF NOT EXISTS idx_sample_order_outbox_delivery
    ON sample_order_outbox (status, available_at, lease_until);
