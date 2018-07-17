--liquibase formatted sql
--changeset kiendt:1.100
ALTER TABLE labels ADD COLUMN device_group_id bigint(20);