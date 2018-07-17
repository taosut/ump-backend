--liquibase formatted sql
--changeset kiendt:2.2
ALTER TABLE labels ADD COLUMN device_group_id bigint;