--liquibase formatted sql
--changeset khanhmq:1.86
ALTER TABLE ip_mappings ADD COLUMN label_id text;