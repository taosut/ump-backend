--liquibase formatted sql
--changeset tuanha2:1.99
ALTER TABLE reports_datas
ADD count_online INT (20);

ALTER TABLE reports_datas
ADD serial_number varchar(255);

ALTER TABLE reports_datas
ADD ip_address varchar(255);

ALTER TABLE reports_datas
ADD manufacturer varchar(255);