--liquibase formatted sql
--changeset kiendt:2.2
alter table device_groups add devices longvarchar;