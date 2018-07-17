--liquibase formatted sql
--changeset kiendt:1.105
alter table qos_graphs add graph_period varchar(5);


