--liquibase formatted sql
--changeset luongnv:1.95
ALTER TABLE policy_jobs ADD COLUMN schedule_time bigint(20);