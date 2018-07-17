--liquibase formatted sql
--changeset kiendt:1.103
create table qos_kpis (
  id bigint auto_increment,
  kpi_index VARCHAR(255),
  kpi_value text,
  kpi_type VARCHAR(255),
  kpi_measure VARCHAR(255),
  profile_id bigint,
  created bigint,
  updated bigint,
  primary key(id)
);

create table qos_graphs (
  id bigint auto_increment,
  graph_name VARCHAR(255),
  graph_by text,
  graph_type text,
  graph_index text,
  auto_refresh int,
  graph_position int,
  profile_id bigint,
  created bigint,
  updated bigint,
  primary key(id)
);

