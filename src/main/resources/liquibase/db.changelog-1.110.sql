--liquibase formatted sql
--changeset kiendt:1.110
insert into notification_settings(
user_id,
alarm_total,
critical_total,
major_total,
minor_total,
device_groups,
is_send_email,
is_send_notify_list,
is_send_notify,
is_send_sms,
active,
created,
updated,
time_count_total,
time_count_critical,
time_count_major,
time_count_minor
) select id,
NULL,
NULL,
NULL,
NULL,
NULL,
0,0,0,0,0,
NULL,NULL,NULL,NULL,NULL
,null from users a where a.id not in (select b.user_id from notification_settings b);