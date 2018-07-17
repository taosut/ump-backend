--liquibase formatted sql
--changeset thangnc2:1.79
ALTER TABLE alarm_types ADD COLUMN device_group_role text;