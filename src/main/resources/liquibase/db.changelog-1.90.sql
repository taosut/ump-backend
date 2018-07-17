--liquibase formatted sql
--changeset kiendt:1.90
DROP TABLE IF EXISTS `alarm_graphs`;
CREATE TABLE `alarm_graphs` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `device_groups` text,
  `start_date` bigint(20) DEFAULT NULL,
  `end_date` bigint(20) DEFAULT NULL,
  `severity` varchar(32) DEFAULT NULL,
  `alarm_type_name` varchar(32) DEFAULT NULL,
  `total` int(20) DEFAULT NULL,
  `created` bigint(20) DEFAULT NULL,
  `updated` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=682 DEFAULT CHARSET=utf8;