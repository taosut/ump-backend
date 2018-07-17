--liquibase formatted sql
--changeset tuanha:1.77
ALTER TABLE alarm_details ADD COLUMN raised bigint(20);