/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vn.ssdc.vnpt.notification.services;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.searchbox.client.JestClient;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandler;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;
import vn.ssdc.vnpt.common.services.EmailTemplateService;
import vn.ssdc.vnpt.common.services.MailService;
import vn.ssdc.vnpt.elk.BaseElkService;
import vn.ssdc.vnpt.notification.model.NotificationAlarmElk;
import vn.ssdc.vnpt.notification.model.NotificationSetting;
import vn.ssdc.vnpt.selfCare.model.SCNotificationAlarmElk;
import vn.ssdc.vnpt.selfCare.model.SCUser;
import vn.ssdc.vnpt.selfCare.model.searchForm.SCUserSearchForm;
import vn.ssdc.vnpt.selfCare.services.SelfCareServiceUser;
import vn.ssdc.vnpt.utils.CommonService;
import vn.ssdc.vnpt.utils.StringUtils;
import vn.ssdc.vnpt.websocket.MyStompSessionHandler;
import vn.vnpt.ssdc.core.SsdcCrudService;
import vn.vnpt.ssdc.jdbc.factories.RepositoryFactory;

/**
 *
 * @author kiendt
 */
@Service
public class NotificationSettingService extends SsdcCrudService<Long, NotificationSetting> {

    private static final Logger logger = LoggerFactory.getLogger(NotificationSetting.class);

    public static final int ALARM_TYPE_TOTAL = 0;
    public static final int ALARM_TYPE_CRITICAL = 1;
    public static final int ALARM_TYPE_MAJOR = 2;
    public static final int ALARM_TYPE_MINOR = 3;

    public static final int STATUS_SEEN = 1;
    public static final int STATUS_NEW = 0;

    @Autowired
    private CommonService baseService;

    @Autowired
    private Executor executor;

    @Autowired
    JestClient elasticSearchClient;

    @Autowired
    private SelfCareServiceUser selfCareServiceUser;

    @Value("${spring.elk.index.threshold_qos}")
    public String INDEX_THRESH_HOLD;

    @Value("${spring.elk.type.threshold_qos}")
    public String TYPE_THRESH_HOLD;

    @Value("${spring.elk.index.notification_alarm}")
    public String INDEX_NOTIFIACTION_ALARM;

    @Value("${spring.elk.type.notification_alarm}")
    public String TYPE_NOTIFIACTION_ALARM;

    @Autowired
    public EmailTemplateService emailTemplateService;

    @Autowired
    public MailService mailService;

    @Autowired
    public BaseElkService baseElkService;

    @Value("${websocketUrl}")
    public String websocketUrl;

    @Value("${spring.websocket.topic.notification-list}")
    public String notificationListTopic;

    @Value("${spring.websocket.topic.notification-popup}")
    public String notificationPopUpTopic;

    public StompSession webSocketStompClient() throws Exception {
        WebSocketClient simpleWebSocketClient = new StandardWebSocketClient();
        List<Transport> transports = new ArrayList<>(1);
        transports.add(new WebSocketTransport(simpleWebSocketClient));

        SockJsClient sockJsClient = new SockJsClient(transports);
        WebSocketStompClient stompClient = new WebSocketStompClient(sockJsClient);
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        StompSessionHandler sessionHandler = new MyStompSessionHandler();
        return stompClient.connect(websocketUrl, sessionHandler).get();
    }

    @Autowired
    public NotificationSettingService(RepositoryFactory repositoryFactory) {
        this.repository = repositoryFactory.create(NotificationSetting.class);
    }

    //<editor-fold defaultstate="collapsed" desc="Basic query to notification setting">
    public List<NotificationSetting> findByQuery(String query, Integer index, Integer limit) {
        return this.repository.search(query, new PageRequest(index, limit)).getContent();
    }

    public Page<NotificationSetting> getPage(int page, int limit) {
        return this.repository.findAll(new PageRequest(page, limit));
    }

    public List<NotificationSetting> findByQuery(String query) {
        return this.repository.search(query);
    }

    public NotificationSetting getByUserId(Long userId) {
        String query = String.format(" user_id = %s ", userId);
        List<NotificationSetting> data = findByQuery(query);
        if (data != null && !data.isEmpty()) {
            return data.get(0);
        }
        return null;
    }
//</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Handle Notify when receiver data from notificaiotn topic kafka">
    // TODO  moi listen gan voi mot process ump, neu trien khai tren nhieu not thi phai thay doi id
    @KafkaListener(topics = "${spring.kafka.topic.notification}", id = "notification_listener")
    public void listen(@Payload String message) throws IOException, InterruptedException {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                // handler notification message
                JsonObject object = new Gson().fromJson(message, JsonObject.class);
                Integer deviceGroupId = object.get("deviceGroups").getAsInt();
                String deviceId = object.get("deviceId").getAsString();
                // lay ra cac user va devicegroupid dinh kem
                Set<Integer> deviceGroupIds = new HashSet<>();
                deviceGroupIds.add(deviceGroupId);
                SCUserSearchForm form = new SCUserSearchForm();
                form.deviceGroupIds = deviceGroupIds;
                List<SCUser> scUsers = selfCareServiceUser.search(form);
                if (scUsers != null && !scUsers.isEmpty()) {
                    // Loi ra dong setting cua user va devicegourp
                    for (SCUser user : scUsers) {
                        NotificationSetting notificationSetting = getByUserId(user.userId);
                        if (notificationSetting.active == 1) {
                            try {
                                boolean needUpdate = false;
                                // so sanh total voi nguong xem co sinh notify khong
                                JsonObject obj1 = handleData(deviceGroupId, notificationSetting.timeCountTotal, StringUtils.convertDateToElk(StringUtils.convertDateToString(0), "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"));
                                Integer total = getCountAlarmByAlarmType(obj1, ALARM_TYPE_TOTAL);
                                logger.info("Total Alarm : {}, user: {}", total, user.fullName);
                                if (total != null && notificationSetting.alarmTotal != null && total >= notificationSetting.alarmTotal) {
                                    // generate content notify and insert to elk
                                    NotificationAlarmElk contentNotification = createContentNotification(ALARM_TYPE_TOTAL, total);
                                    contentNotification.deviceId = deviceId;
                                    contentNotification.userId = user.userId;
                                    contentNotification.type = 0;
                                    insertToElk(contentNotification);
                                    // send notify to user follow by notifiysetting
                                    createNotification(notificationSetting, contentNotification, ALARM_TYPE_TOTAL);
                                    // resset time count
                                    notificationSetting.timeCountTotal = StringUtils.convertDateToElk(StringUtils.convertDateToString(0), "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
                                    needUpdate = true;
                                }

                                // so sanh total voi nguong xem co sinh notify khong
                                JsonObject obj2 = handleData(deviceGroupId, notificationSetting.timeCountCritical, StringUtils.convertDateToElk(StringUtils.convertDateToString(0), "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"));
                                Integer numberCritical = getCountAlarmByAlarmType(obj2, ALARM_TYPE_CRITICAL);
                                logger.info("Total Critical : {}, user: {}", numberCritical, user.fullName);
                                if (numberCritical != null && notificationSetting.criticalTotal != null && numberCritical >= notificationSetting.criticalTotal) {
                                    // generate content notify and insert to elk
                                    NotificationAlarmElk contentNotification = createContentNotification(ALARM_TYPE_CRITICAL, numberCritical);
                                    contentNotification.deviceId = deviceId;
                                    contentNotification.userId = user.userId;
                                    contentNotification.type = 1;
                                    insertToElk(contentNotification);
                                    // send notify to user follow by notifiysetting
                                    createNotification(notificationSetting, contentNotification, ALARM_TYPE_CRITICAL);
                                    // resset time count
                                    notificationSetting.timeCountCritical = StringUtils.convertDateToElk(StringUtils.convertDateToString(0), "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
                                    needUpdate = true;
                                }
                                // so sanh total voi nguong xem co sinh notify khong
                                JsonObject obj3 = handleData(deviceGroupId, notificationSetting.timeCountMajor, StringUtils.convertDateToElk(StringUtils.convertDateToString(0), "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"));
                                Integer numberMajor = getCountAlarmByAlarmType(obj3, ALARM_TYPE_MAJOR);
                                logger.info("Total Major : {}, user: {}", numberMajor, user.fullName);
                                if (numberMajor != null && notificationSetting.majorTotal != null && numberMajor >= notificationSetting.majorTotal) {
                                    NotificationAlarmElk contentNotification = createContentNotification(ALARM_TYPE_MAJOR, numberMajor);
                                    contentNotification.deviceId = deviceId;
                                    contentNotification.userId = user.userId;
                                    contentNotification.type = 2;
                                    insertToElk(contentNotification);
                                    createNotification(notificationSetting, contentNotification, ALARM_TYPE_MAJOR);
                                    notificationSetting.timeCountMajor = StringUtils.convertDateToElk(StringUtils.convertDateToString(0), "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
                                    needUpdate = true;
                                }
                                // so sanh total voi nguong xem co sinh notify khong
                                JsonObject obj4 = handleData(deviceGroupId, notificationSetting.timeCountMinor, StringUtils.convertDateToElk(StringUtils.convertDateToString(0), "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"));
                                Integer numberMinor = getCountAlarmByAlarmType(obj4, ALARM_TYPE_MINOR);
                                logger.info("Total Minor : {}, user: {}", numberMinor, user.fullName);
                                if (numberMinor != null && notificationSetting.minorTotal != null && numberMinor >= notificationSetting.minorTotal) {
                                    NotificationAlarmElk contentNotification = createContentNotification(ALARM_TYPE_MINOR, numberMinor);
                                    contentNotification.deviceId = deviceId;
                                    contentNotification.userId = user.userId;
                                    contentNotification.type = 3;
                                    insertToElk(contentNotification);
                                    createNotification(notificationSetting, contentNotification, ALARM_TYPE_MINOR);
                                    notificationSetting.timeCountMinor = StringUtils.convertDateToElk(StringUtils.convertDateToString(0), "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
                                    needUpdate = true;
                                }
                                if (needUpdate) {
                                    resetCondition(notificationSetting);
                                }
                            } catch (IOException ex) {
                                ex.printStackTrace();
                            }
                        } else {
                            logger.info("Notification not active  to user {}", user.fullName);
                        }

                    }

                    //  truy van du lieuj tu elastich search
                } else {
                    logger.info("Device Group {} not assiged to any user!", deviceGroupId);
                }
            }
        });

    }

//</editor-fold>
    //<editor-fold defaultstate="collapsed" desc="Parse data from data in Elk query to count alarmtotal, cirtical alarm, major alarm and minor alamr">
    /**
     * Critical = 1; Major = 2 ;Minor = 3; Total = 0
     *
     * @param object
     * @param alarmType
     * @return
     */
    private Integer getCountAlarmByAlarmType(JsonObject object, int alarmType) {
        if (alarmType == 0) {
            Integer total = object.get("hits").getAsJsonObject().get("total").getAsInt();
            return total;
        }
        try {
            JsonArray arr = object.get("aggregations").getAsJsonObject().get("kpiSeverityNumber").getAsJsonObject().get("buckets").getAsJsonArray();
            for (int i = 0; i < arr.size(); i++) {
                JsonObject tmp = arr.get(i).getAsJsonObject();
                if (tmp.get("key") != null && tmp.get("doc_count") != null) {
                    if (tmp.get("key").getAsInt() == alarmType) {
                        return tmp.get("doc_count").getAsInt();
                    }
                } else {
                    return 0;
                }
            }
        } catch (Exception e) {
            logger.error("Error when get count alarm {} , {}", alarmType == 1 ? "critical" : alarmType == 2 ? "major" : "minor", e.getStackTrace());
        }
        return null;
    }

    /**
     * truy van du lieu trong elk de lay ra total_alarm, alarm theo tung
     * serverity critical - major - minor
     *
     * @param deviceGroupId
     * @param startTime
     * @param endTime
     * @return
     * @throws IOException
     */
    private JsonObject handleData(Integer deviceGroupId, String startTime, String endTime) throws IOException {
        return baseElkService.query(generateQuery(deviceGroupId, startTime, endTime), INDEX_THRESH_HOLD, TYPE_THRESH_HOLD);
    }

    private String generateQuery(Integer deviceGroupId, String startTime, String endTime) {
        String query = "{\n"
                + "  \"size\" : 0,\n"
                + "  \"query\" : {\n"
                + "    \"bool\" : {\n"
                + "      \"must\" : [ {\n"
                + "        \"range\" : {\n"
                + "          \"@timestamp\" : {\n"
                + "            \"from\" : \"" + startTime + "\",\n"
                + "            \"to\" : \"" + endTime + "\",\n"
                + "            \"include_lower\" : true,\n"
                + "            \"include_upper\" : true\n"
                + "          }\n"
                + "        }\n"
                + "      }, {\n"
                + "        \"match\" : {\n"
                + "          \"deviceGroups\" : {\n"
                + "            \"query\" : \"" + deviceGroupId + "\",\n"
                + "            \"type\" : \"phrase\"\n"
                + "          }\n"
                + "        }\n"
                + "      } ]\n"
                + "    }\n"
                + "  },\n"
                + "    \"aggs\": {\n"
                + "\t    \"kpiSeverityNumber\": {\n"
                + "\t      \"terms\": {\n"
                + "\t        \"field\": \"kpiSeverityNumber\",\n"
                + "\t        \"size\": 9999\n"
                + "\t      }"
                + "\t    }\n"
                + "\t}\n"
                + "}";
        return query;
    }
//</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Send notification base on notificaiton setting includes: send sms, send email, send notify to websocket">
    private void createNotification(NotificationSetting notificationSetting, NotificationAlarmElk contentNotification, int alarmType) {
        // neu co gui email
        if (contentNotification != null && !Strings.isNullOrEmpty(contentNotification.content)) {
            // insert data notifiy to elk

            // SEND EMAIL
            if (notificationSetting.isSendEmail == 1) {

                SCUserSearchForm searchForm = new SCUserSearchForm();
                searchForm.userId = notificationSetting.userId;
                SCUser user = selfCareServiceUser.search(searchForm) != null ? selfCareServiceUser.search(searchForm).get(0) : null;
                if (user != null) {
                    String email = user.email;
                    mailService.sendMail(email, "Alarm notification by UMP", contentNotification.content, null, null);
                } else {
                    logger.info("Cannot find user with userid = {}", notificationSetting.userId);
                }

            }
            // SEND NOTIFY TO WEB
            if (notificationSetting.isSendNotify == 1) {
                try {
                    webSocketStompClient().send(notificationListTopic + contentNotification.userId, new SCNotificationAlarmElk(contentNotification));
                } catch (Exception ex) {
                    logger.error("Cannot send notify to topic {} for {} , error : {} ", notificationListTopic, notificationSetting.userId, ex.getStackTrace());
                }
            }

            // SEND NOTIFY TO CAI CHUONG
            if (notificationSetting.isSendNotifyList == 1) {
                try {
                    webSocketStompClient().send(notificationPopUpTopic + contentNotification.userId, new SCNotificationAlarmElk(contentNotification));
                } catch (Exception ex) {
                    ex.printStackTrace();
                    logger.error("Cannot send notify to topic {} for {},, error : {}  ", notificationPopUpTopic, notificationSetting.userId, ex.getStackTrace());
                }
            }

            // SEND SMS
            if (notificationSetting.isSendSms == 1) {
                logger.info("NOT SUPPORT SEND SMS NOTIFICATION");
            }
        }

    }
//</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="reset time count in notifcation setting after send notify">
    private void resetCondition(NotificationSetting notificationSetting) {
        update(notificationSetting.id, notificationSetting);
    }
//</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Create nofitication base on alarmtype and value from notification topic">
    private NotificationAlarmElk createContentNotification(int alarmType, int value) {
        NotificationAlarmElk notificationAlarmElk = new NotificationAlarmElk();
        String contentNotify = "";
        switch (alarmType) {
            case ALARM_TYPE_CRITICAL:
                contentNotify = String.format(emailTemplateService.get("notification.max_critical").value, value);
                break;
            case ALARM_TYPE_MAJOR:
                contentNotify = String.format(emailTemplateService.get("notification.max_major").value, value);
                break;
            case ALARM_TYPE_MINOR:
                contentNotify = String.format(emailTemplateService.get("notification.max_minor").value, value);
                break;
            case ALARM_TYPE_TOTAL:
                contentNotify = String.format(emailTemplateService.get("notification.max_total").value, value);
                break;
            default:
                logger.info("Alarm type not allow {}", alarmType);
        }
        notificationAlarmElk.content = contentNotify;
        notificationAlarmElk.status = STATUS_NEW;
        notificationAlarmElk.timestamp = StringUtils.convertDateToElk(StringUtils.convertDateToString(0), "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        return notificationAlarmElk;
    }
//</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Insert Notification to ELK">
    private void insertToElk(NotificationAlarmElk alarm) {
        baseElkService.insertDocument(alarm, INDEX_NOTIFIACTION_ALARM, TYPE_NOTIFIACTION_ALARM);
    }

//</editor-fold>
}
