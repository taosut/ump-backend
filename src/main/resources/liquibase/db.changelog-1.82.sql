--liquibase formatted sql
--changeset luongnv:1.82
ALTER TABLE tags ADD COLUMN corresponding_module text;