CREATE DATABASE IF NOT EXISTS sannhaminh
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE sannhaminh;

CREATE TABLE IF NOT EXISTS accounts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_accounts_email UNIQUE (email)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS venues (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(120) NOT NULL,
    sport VARCHAR(40) NOT NULL,
    address VARCHAR(200) NOT NULL,
    price_per_hour BIGINT NOT NULL,
    image_url VARCHAR(500),
    rating DOUBLE NOT NULL,
    review_count INT NOT NULL,
    amenities VARCHAR(500),
    featured BOOLEAN NOT NULL,
    owner_id BIGINT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    KEY idx_venues_owner (owner_id),
    CONSTRAINT fk_venues_owner FOREIGN KEY (owner_id) REFERENCES accounts (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS bookings (
    id BIGINT NOT NULL AUTO_INCREMENT,
    venue_id BIGINT NOT NULL,
    customer_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    email VARCHAR(150) NOT NULL,
    booking_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    total_price BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    PRIMARY KEY (id),
    KEY idx_bookings_venue_date_status (venue_id, booking_date, status),
    KEY idx_bookings_email_date (email, booking_date),
    CONSTRAINT fk_bookings_venue FOREIGN KEY (venue_id) REFERENCES venues (id)
) ENGINE=InnoDB;
