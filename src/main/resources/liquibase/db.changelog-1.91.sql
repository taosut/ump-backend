--liquibase formatted sql
--changeset luongnv:1.91
ALTER TABLE tags MODIFY corresponding_module VARCHAR(1024);
ALTER TABLE tags DROP COLUMN synchronize;