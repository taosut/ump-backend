--liquibase formatted sql
--changeset khanhmq:1.87
ALTER TABLE `ip_mappings` CHANGE `label_id` `label_id` VARCHAR(1024);