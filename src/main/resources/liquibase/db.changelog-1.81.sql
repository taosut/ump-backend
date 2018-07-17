--liquibase formatted sql
--changeset thanhlx:1.81
ALTER TABLE policy_jobs ADD COLUMN current_number int(11);