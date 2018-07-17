--liquibase formatted sql
--changeset khanhmq:2.0
CREATE TABLE ip_mappings (
  id bigint identity primary key,
  ip_mappings longvarchar,
  label longvarchar,
  label_id longvarchar,
  start_ip longvarchar,
  end_ip longvarchar,
  created bigint,
	updated bigint
);


