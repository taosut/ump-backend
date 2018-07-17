--liquibase formatted sql
--changeset kiendt:1.108
create table policy_templates (
  id bigint auto_increment,
  name VARCHAR(255),
  type VARCHAR(255),
  connection_direction VARCHAR(255),
  url text,
  description text,
  created bigint,
  updated bigint,
  primary key(id)
);