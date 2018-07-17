--liquibase formatted sql
--changeset thangnc2:1.80
ALTER TABLE alarm_types DROP device_group_role;