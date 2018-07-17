--liquibase formatted sql
--changeset luongnv:1.101
INSERT INTO email_templates VALUES ('user.forgotPassword2', '<p>Please click <a href="http://10.15.12.134:8081/changeForgotPassword?userId=%d&token=%s&redirect=%s">here</a> to recover your password with code:  <strong>%s</strong></p>', null, null, null);