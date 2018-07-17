--liquibase formatted sql
--changeset kiendt:2.1
ALTER TABLE tags ADD COLUMN sub_profile_setting longvarchar;
ALTER TABLE tags ADD COLUMN profile_setting longvarchar;