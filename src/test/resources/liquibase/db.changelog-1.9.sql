--liquibase formatted sql
--changeset khanhmq:1.9
CREATE TABLE labels (
  id bigint identity primary key,
  name longvarchar,
  description longvarchar,
  parent_id longvarchar,
  parent_name longvarchar,
  ip_mapping longvarchar,
  created bigint,
	updated bigint
);



