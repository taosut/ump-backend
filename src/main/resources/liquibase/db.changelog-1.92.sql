--liquibase formatted sql
--changeset khanhmq:1.92
ALTER TABLE `ip_mappings` ADD COLUMN start_ip VARCHAR(256);
ALTER TABLE `ip_mappings` ADD COLUMN end_ip VARCHAR(256);
