--liquibase formatted sql
--changeset khanhmq:1.85
CREATE TABLE ip_mappings (
  id bigint(20) AUTO_INCREMENT,
  ip_mappings varchar(256),
  label VARCHAR(255),
  created bigint,
	updated bigint,
	primary key(id)
);