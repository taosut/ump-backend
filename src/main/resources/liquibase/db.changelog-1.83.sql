--liquibase formatted sql
--changeset khanhmq:1.83
CREATE TABLE labels (
  id bigint(20) AUTO_INCREMENT,
  name varchar(256),
  description varchar(1024),
  parent_id varchar(128),
  parent_name varchar(256),
  ip_mapping varchar(256),
  created bigint,
	updated bigint,
	primary key(id)
);