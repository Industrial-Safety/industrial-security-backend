-- Usuario de replicación
CREATE USER replicator WITH REPLICATION ENCRYPTED PASSWORD 'replicator123';

-- Extensión UUID
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Tabla cameras
CREATE TABLE IF NOT EXISTS cameras (
    id_camera   UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(100) NOT NULL,
    ip          VARCHAR(45),
    camera_key  VARCHAR(50)  NOT NULL UNIQUE,
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

INSERT INTO cameras (name, ip, camera_key)
VALUES ('Webcam local prueba', NULL, 'cam-0')
ON CONFLICT (camera_key) DO NOTHING;

-- Tabla incidents
CREATE TABLE IF NOT EXISTS incidents (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    camera_key      VARCHAR(50) NOT NULL,
    violation_types TEXT        NOT NULL,
    evidence_url    TEXT        NOT NULL,
    confidence      DECIMAL(5,4),
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                    CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    detected_at     TIMESTAMPTZ NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    reviewed_by     UUID,
    reviewed_at     TIMESTAMPTZ,
    review_notes    TEXT
);

-- Índices
CREATE INDEX IF NOT EXISTS idx_incidents_status   ON incidents(status);
CREATE INDEX IF NOT EXISTS idx_incidents_camera   ON incidents(camera_key);
CREATE INDEX IF NOT EXISTS idx_incidents_detected ON incidents(detected_at DESC);