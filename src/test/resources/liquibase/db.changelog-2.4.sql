--liquibase formatted sql
--changeset kiendt:2.4
create table notification_settings (
  id bigint identity primary key,
  user_id bigint,  
  alarm_total bigint,
  critical_total bigint,
  major_total bigint,
  minor_total bigint,
  device_groups longvarchar,
  is_send_email bigint,
  is_send_notify_list bigint,
  is_send_notify bigint,
  is_send_sms bigint,
  active bigint,
  time_count_total longvarchar,
  time_count_critical longvarchar,
  time_count_major longvarchar,
  time_count_minor longvarchar,
  created bigint,
  updated bigint
);