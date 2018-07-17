--liquibase formatted sql
--changeset khanhmq:1.8
ALTER TABLE device_groups ADD COLUMN label_id longvarchar;


