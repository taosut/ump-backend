/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vn.ssdc.vnpt.kafka.services;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 *
 * @author kiendt
 */
@Service
public class KafkaService {

    private static final Logger LOG = LoggerFactory.getLogger(KafkaService.class);

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Value("${spring.kafka.topic.threshold}")
    private String thresholdTopic;

    @Value("${spring.kafka.topic.qosKpi}")
    private String qosKpi;

    @Value("${spring.kafka.topic.alarmList}")
    private String alarmList;

    @Value("${spring.kafka.topic.notification}")
    private String notificationTopic;

    public void sendToThresholdTopic(String message) {
        try {
            LOG.info("Sending message {} to threshold topic", "[" + message + "]");
            kafkaTemplate.send(thresholdTopic, "[" + message + "]");
        } catch (Exception e) {
            LOG.error("Error when sending message {} to threshold topic, error : {}", message, e.getMessage());
        }

    }

    public void sendToQosKpiTopic(String message) {
        try {
            LOG.info("Sending message {} to QosKpi topic", message);
            kafkaTemplate.send(qosKpi, message);
        } catch (Exception e) {
            LOG.error("Error when sending message {} to QosKpi topic, error : {}", message, e);
        }
    }

    public void sendToAlarmListTopic(String message) {
        try {
            LOG.info("Sending message {} to AlarmList topic", "[" + message + "]");
            kafkaTemplate.send(alarmList, "[" + message + "]");
        } catch (Exception e) {
            LOG.error("Error when sending message {} to AlarmList Topic, error : {}", message, e);
        }
    }

    public void sendToNotificationTopic(String message) {
        try {
            LOG.info("Sending message {} to Notification topic", "[" + message + "]");
            kafkaTemplate.send(notificationTopic, message);

        } catch (Exception e) {
            LOG.error("Error when sending message {} to notificationTopic , error : {}", message, e);
        }
    }

}
