--liquibase formatted sql
--changeset thangnc2:1.93
ALTER TABLE performance_settings MODIFY stastics_interval bigint(20);