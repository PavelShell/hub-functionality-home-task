CREATE TABLE simple_park_parking (
    id SERIAL PRIMARY KEY,
    internal_parking_id VARCHAR(255) UNIQUE NOT NULL,
    external_parking_id VARCHAR(255),
    license_plate VARCHAR(32) NOT NULL,
    area_code VARCHAR(32) NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP,
    status VARCHAR(8) NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);
