--liquibase formatted sql
--changeset khanhmq:1.88
ALTER TABLE device_groups ADD COLUMN label_id VARCHAR(256);