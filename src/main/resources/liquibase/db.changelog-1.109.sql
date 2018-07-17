--liquibase formatted sql
--changeset kiendt:1.109
create table notification_settings (
  id bigint auto_increment,
  user_id bigint,  
  alarm_total INT (5),
  critical_total INT (5),
  major_total INT (5),
  minor_total INT (5),
  device_groups varchar(255),
  is_send_email BIT(1),
  is_send_notify_list BIT(1),
  is_send_notify BIT(1),
  is_send_sms BIT(1),
  active BIT(1),
  time_count_total varchar(255),
  time_count_critical varchar(255),
  time_count_major varchar(255),
  time_count_minor varchar(255),
  created bigint,
  updated bigint,
  primary key(id)
);