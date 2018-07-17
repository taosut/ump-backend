--liquibase formatted sql
--changeset kiendt:1.104
alter table device_groups add devices LONGTEXT;


