--liquibase formatted sql
--changeset thangnc2:1.78
ALTER TABLE performance_settings ADD COLUMN device_group_role text;