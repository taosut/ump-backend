--liquibase formatted sql
--changeset kiendt:1.106
alter table qos_kpis add kpi_threshold LONGTEXT;
alter table qos_kpis add kpi_formula varchar(200);


