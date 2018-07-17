--liquibase formatted sql
--changeset kiendt:1.102
create table account_mappings (
  id bigint auto_increment,
  label VARCHAR(255),
  account_prefix VARCHAR(255),
  label_id bigint,
  created bigint,
  updated bigint,
  primary key(id)
);