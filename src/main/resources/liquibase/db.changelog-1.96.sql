--liquibase formatted sql
--changeset thanhlx:1.96
ALTER TABLE policy_jobs ADD COLUMN ended bigint(20);