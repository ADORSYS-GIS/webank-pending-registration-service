-- Create enum types
CREATE TYPE personal_info_status AS ENUM ('PENDING', 'APPROVED', 'REJECTED');
CREATE TYPE otp_status AS ENUM ('PENDING', 'COMPLETE', 'INCOMPLETE');
CREATE TYPE user_documents_status AS ENUM ('PENDING', 'APPROVED', 'REJECTED');

-- Create personal_information_table
CREATE TABLE personal_information_table (
    account_id VARCHAR(255) PRIMARY KEY,
    document_id VARCHAR(20),
    document_expiration_date VARCHAR(20),
    otp_expires_at TIMESTAMP,
    location VARCHAR(255),
    email VARCHAR(30),
    email_otp_hash VARCHAR(255),
    email_otp_code VARCHAR(255),
    status personal_info_status,
    rejection_reason VARCHAR(255)
);

-- Create otp_requests table
CREATE TABLE otp_requests (
    id UUID PRIMARY KEY,
    phone_number VARCHAR(20) NOT NULL,
    public_key_hash VARCHAR(255) NOT NULL UNIQUE,
    otp_hash VARCHAR(255) NOT NULL,
    otp_code VARCHAR(20) NOT NULL,
    status otp_status NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

-- Create index on public_key_hash
CREATE INDEX idx_public_key_hash ON otp_requests(public_key_hash);

-- Create user_documents table
CREATE TABLE user_documents (
    account_id VARCHAR(255) PRIMARY KEY,
    front_id TEXT,
    back_id TEXT,
    selfie_id TEXT,
    tax_id TEXT,
    status user_documents_status
); 