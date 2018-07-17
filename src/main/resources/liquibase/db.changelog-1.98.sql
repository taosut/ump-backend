--liquibase formatted sql
--changeset tuanha2:1.98
DROP TABLE device_temp_birts ;

CREATE TABLE reports_datas (
id bigint(20) NOT NULL AUTO_INCREMENT,
label varchar(255),
model varchar(255),
firmware_version VARCHAR (255),
count_by_firmware INT (20),
count_by_label INT (20),
count_by_label_model INT (20),
specific_id bigint,
created bigint,
updated bigint,
PRIMARY KEY (id)
);