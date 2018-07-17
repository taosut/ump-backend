--liquibase formatted sql
--changeset tuanha2:1.97
CREATE TABLE device_temp_birts (
    id bigint(20) NOT NULL AUTO_INCREMENT,
    serial_number varchar(255),
    manufacturer varchar(255),
    product_class varchar(255),
    oui varchar(255),
    firmware_version varchar(255),
    model_name varchar(255),
    label text,
    created bigint,
    updated bigint,
    PRIMARY KEY (id)
);