--liquibase formatted sql
--changeset kiendt:1.94
ALTER TABLE tags ADD COLUMN sub_profile_setting LONGTEXT;
ALTER TABLE tags ADD COLUMN profile_setting LONGTEXT;