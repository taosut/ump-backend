package vn.ssdc.vnpt.alarm.services;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.searchbox.client.JestClient;
import io.searchbox.client.JestClientFactory;
import io.searchbox.client.config.HttpClientConfig;
import io.searchbox.core.Index;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import vn.ssdc.vnpt.AcsClient;
import vn.ssdc.vnpt.alarm.model.*;
import vn.ssdc.vnpt.common.services.EmailTemplateService;
import vn.ssdc.vnpt.common.services.MailService;
import vn.ssdc.vnpt.devices.model.DeviceGroup;
import vn.ssdc.vnpt.logging.model.ElkLoggingDevice;
import vn.ssdc.vnpt.logging.model.LoggingDevice;
import vn.ssdc.vnpt.logging.services.LoggingDeviceService;
import vn.ssdc.vnpt.user.model.User;
import vn.ssdc.vnpt.user.services.UserService;
import vn.ssdc.vnpt.utils.StringUtils;
import vn.vnpt.ssdc.core.SsdcCrudService;
import vn.vnpt.ssdc.jdbc.factories.RepositoryFactory;

import javax.jws.soap.SOAPBinding;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.quartz.DateBuilder.futureDate;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import vn.ssdc.vnpt.performance.model.PerformanceELK;
import static vn.ssdc.vnpt.performance.sevices.PerformanceSettingService.INDEX_UMP_PERFORMANCE;
import static vn.ssdc.vnpt.performance.sevices.PerformanceSettingService.TYPE_UMP_PERFORMANCE;

/**
 * Created by Lamborgini on 5/24/2017.
 */
@Service
public class AlarmService extends SsdcCrudService<Long, Alarm> {

    private static final Logger logger = LoggerFactory.getLogger(AlarmService.class);

    public static String INDEX = "logging_cwmp";
    public static String TYPE = "logging_cwmp";

    public static String INDEX_REQUEST_FAIL = "logging_device";
    public static String TYPE_REQUEST_FAIL = "logging_device";

    public static String INDEX_PERFORMANCE = "ump_performance";
    public static String TYPE_PERFORMANCE = "ump_performance";

    private static final String SOFTWARE_VERSION_KEY = "summary.softwareVersion";
    private static final String DEVICEID_KEY = "_deviceId";

    @Autowired
    private AlarmTypeService alarmTypeService;

    @Autowired
    private AlarmDetailsService alarmDetailsService;

    @Autowired
    private AlarmDetailELKService alarmDetailELKService;

    @Autowired
    private AlarmELKService alarmELKService;

    @Value("${elasticSearchUrl}")
    public String elasticSearchUrl;

    @Autowired
    public MailService mailService;

    @Autowired
    public AcsClient acsClient;

    @Autowired
    public LoggingDeviceService loggingDeviceService;

    @Autowired
    public UserService userService;

    @Autowired
    public EmailTemplateService emailTemplateService;

    @Autowired
    public AlarmService(RepositoryFactory repositoryFactory) {
        this.repository = repositoryFactory.create(Alarm.class);
    }

    @Autowired
    private Scheduler scheduler;

    @Autowired
    JestClient elasticSearchClient;

    public void monitoringCWMPLog(String fromDate, String toDate) throws IOException {
        JestClient jestClient = elasticSearchClient();
        List<ElasticsearchLogObject> logList;

        BoolQueryBuilder boolQueryBuilder = QueryBuilders
                .boolQuery();

        try {
            boolQueryBuilder.must(QueryBuilders
                    .rangeQuery("@timestamp")
                    .gte(parseIsoDate(fromDate))
                    .lt(parseIsoDate(toDate))
                    .includeLower(true)
                    .includeUpper(true));
            boolQueryBuilder.must(QueryBuilders.matchPhrasePrefixQuery("message", "VALUE_CHANGE"));

        } catch (ParseException e) {
            e.printStackTrace();
        }

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(boolQueryBuilder).size(9999);
        Search.Builder builder = new Search.Builder(searchSourceBuilder.toString());
        builder.addIndex(INDEX);
        builder.addType(TYPE);

        SearchResult result = jestClient.execute(builder.build());
        logList = result.getSourceAsObjectList(ElasticsearchLogObject.class);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        TimeZone tz = TimeZone.getTimeZone("GMT+0");
        sdf.setTimeZone(tz);

        for (ElasticsearchLogObject elo : logList) {
            int beginString = elo.message.lastIndexOf("[VALUE_CHANGE]");
            String strMessageHandle = elo.message.substring(beginString + "[VALUE_CHANGE] ".length(), elo.message.length());
            int spilitLocation = strMessageHandle.indexOf(":");
            String strDeviceID = strMessageHandle.substring(0, spilitLocation);
            String strJsonValue = strMessageHandle.substring(spilitLocation + 1, strMessageHandle.length());
            ///
            beginString = elo.message.lastIndexOf("[32m[");
            String timeSystem = elo.message.substring(beginString + "[32m[".length(), elo.message.indexOf("]"));
            ///
            Gson gson = new Gson();
            JsonElement element = gson.fromJson(strJsonValue, JsonElement.class);
            JsonArray jsonArray = element.getAsJsonArray();

            for (int i = 0; i < jsonArray.size(); i++) {
                JsonElement entry = jsonArray.get(i);
                JsonArray entryArray = entry.getAsJsonArray();
                String strTags = getTagByDevice(strDeviceID);

                String source = jsonBuilder()
                        .startObject()
                        .field("deviceId", strDeviceID)
                        .field("parameterName", entryArray.get(0).getAsString())
                        .field("value", entryArray.get(1).getAsString())
                        .field("@timestamp", sdf.format(new Date()))
                        .field("tags", strTags)
                        .field("timeInLog", timeSystem)
                        .endObject().string();
                Index index = new Index.Builder(source).index(INDEX_PERFORMANCE).type(TYPE_PERFORMANCE).build();
                jestClient.execute(index);
            }
        }

    }

    public void processAlarm(String fromDate, String toDate) throws ParseException {
        //STEP 1 : GET ALL ALARMTYPE NOTIFY = 1
        List<AlarmType> lstAlarmType = alarmTypeService.findByNotify();

        SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        TimeZone tz = TimeZone.getTimeZone("GMT+7");
        sdf1.setTimeZone(tz);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        sdf.setTimeZone(tz);
//        String endTime = String.valueOf(sdf1.format(toDate));
//        String startTime = String.valueOf(sdf1.format(fromDate));

        //STEP 2 : GET ALL ALARMDETAIL BY ALARM TYPE AND TIME
        for (int index = 0; index < lstAlarmType.size(); index++) {
            AlarmType alarmType = lstAlarmType.get(index);
            Long alarmTypeId = alarmType.id;
//            List<AlarmDetails> lstAlarmDetail = alarmDetailsService.findByMonitoring(alarmTypeId, startTime, endTime);
            List<AlarmDetailELK> listAlarmDetailElk = alarmDetailELKService.findByMonitoring(alarmTypeId, fromDate, toDate);

            //STEP 3 : IF SIZE > 0 INSERT TO ALARM
            if (listAlarmDetailElk.size() > 0) {
                Map<String, AlarmELK> mapNotDuplicate = new HashMap<>();
                for (int index_2 = 0; index_2 < listAlarmDetailElk.size(); index_2++) {
                    AlarmDetailELK alarmDetails = listAlarmDetailElk.get(index_2);
                    //Check Before Create
//                    List<Alarm> lstAlarmExits = checkAlarmExits(alarmDetails.alarm_type_id, alarmDetails.device_id, alarmDetails.raised);
                    List<AlarmELK> listAlarmElkExiste = alarmELKService.checkAlarmExits(alarmDetails.alarm_type_id, alarmDetails.device_id, alarmDetails.raised);
                    if (listAlarmElkExiste.isEmpty()) {
                        ////CREATE ALARM
//                        Alarm alarm = new Alarm();
//                        alarm.deviceId = alarmDetails.device_id;
//                        alarm.alarmTypeId = alarmDetails.alarm_type_id;
//                        alarm.alarmTypeName = alarmDetails.alarm_type_name;
//                        alarm.deviceGroups = alarmType.deviceGroups;
//                        alarm.raised = alarmDetails.raised;
//                        alarm.status = "Active";
//                        alarm.severity = alarmType.severity;
//                        alarm.alarmName = alarmType.name;
//                        this.create(alarm);

                        AlarmELK alarmElk = new AlarmELK();
                        alarmElk.device_id = alarmDetails.device_id;
                        alarmElk.alarm_type_id = alarmDetails.alarm_type_id;
                        alarmElk.alarm_type_name = alarmDetails.alarm_type;
                        alarmElk.device_groups = new Gson().toJson(alarmType.deviceGroups);
                        alarmElk.raised = alarmDetails.raised;
                        alarmElk.status = "Active";
                        alarmElk.severity = alarmType.severity;
                        alarmElk.alarmName = alarmType.name;
                        alarmElk.timestamp = sdf1.format(new Date());
                        String key = alarmElk.raised + alarmElk.alarm_type_id + alarmElk.device_id;
                        mapNotDuplicate.put(key, alarmElk);
//                            alarmELKService.create(alarmElk);
                    }
                }
                // remove duplicate
                for (Map.Entry<String, AlarmELK> entry : mapNotDuplicate.entrySet()) {
                    try {
                        logger.info(entry.getValue().toString());
                        alarmELKService.create(entry.getValue());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                if (alarmType.notifyAggregated.equals("EMAIL")) {
                    //CHECK SEND MAIL
                    List<AlarmELK> lstAlarmELKActive = alarmELKService.getAlarmActiveById(alarmType.id);
//                    List<Alarm> lstAlarmActive = getAlarmActiveById(alarmType.id);
                    // List<Alarm> lstAlarmActive = getAlarmActiveById(22l);
                    if (lstAlarmELKActive.size() >= alarmType.aggregatedVolume) {
                        //GET ALL USER ALARM
                        Set<DeviceGroup> deviceGroups = alarmType.deviceGroups;
                        Set<String> listMailSend = new HashSet<>();

                        for (DeviceGroup temp
                                : deviceGroups) {
                            String strSearchID = temp.id.toString();
                            List<User> listUserByDeviceGroupId = userService.getListUserByDeviceGroupId(strSearchID);
                            for (User tempUser : listUserByDeviceGroupId) {
                                listMailSend.add(tempUser.email);
                            }
                        }
                        //SEND MAIL ALL USER
                        String mailContent = String.format(emailTemplateService.get("alarm.notify_ver2").value,
                                alarmType.severity,
                                alarmType.type,
                                alarmType.name);
                        for (String strEMail : listMailSend) {
                            logger.info("SENDING MAIL TO : " + strEMail);
                            mailService.sendMail(strEMail, "ALARM DEVICE OVER AGGREGATED VOLUME", mailContent, null, null);
                        }
                    }
                }
            }
        }
    }

//    Format Date Input
//    String fromDate = "2017-06-05 00:00:00";
//    String toDate = "2017-06-05 23:59:59";
    public void processingAlarmDetail(String fromDate, String toDate) throws IOException, ParseException {
        //Get Alarm Type
        List<AlarmType> lAlarmType = alarmTypeService.findByMonitoring();
        for (int index = 0; index < lAlarmType.size(); index++) {
            AlarmType alarmType = lAlarmType.get(index);
            createQueryAlarmType(alarmType, fromDate, toDate);
        }
    }

    public void createQueryAlarmType(AlarmType alarmType, String fromDate, String toDate) throws IOException, ParseException {
        JestClient jestClient = elasticSearchClient();

        //STEP 1 : Check Alarm Type
        if (alarmType.type.equalsIgnoreCase("REQUEST_FAIL")) {
            logger.info("Handle Alarm REQUEST_FAIL");
            handleRequestFail(alarmType, fromDate, toDate, jestClient);
        } else if (alarmType.type.equalsIgnoreCase("CONFIGURATION_FAIL")) {
            logger.info("Handle Alarm CONFIGURATION_FAIL");
            handleConfigurationFail(alarmType, fromDate, toDate, jestClient);
        } else if (alarmType.type.equalsIgnoreCase("UPDATE_FIRMWARE_FAIL")) {
            logger.info("Handle Alarm UPDATE_FIRMWARE_FAIL");
            handleUpdateFirmWareFail(alarmType, fromDate, toDate, jestClient);
        } else if (alarmType.type.equalsIgnoreCase("REBOOT_FAIL")) {
            logger.info("Handle Alarm REBOOT_FAIL");
            handleRebootFail(alarmType, fromDate, toDate, jestClient);
        } else if (alarmType.type.equalsIgnoreCase("FACTORY_RESET_FAIL")) {
            logger.info("Handle Alarm FACTORY_RESET_FAIL");
            handleFactoryResetFail(alarmType, fromDate, toDate, jestClient);
        } else if (alarmType.type.equalsIgnoreCase("PARAMETER_VALUE")) {
            logger.info("Handle Alarm PARAMETER_VALUE");
            handleParameterValue(alarmType, fromDate, toDate, jestClient);
        } else {
            logger.info("UNKNOW ALARM TYPE : " + alarmType.type);
        }
    }

    public void handleParameterValue(AlarmType alarmType, String fromDate, String toDate, JestClient jestClient) throws IOException, ParseException {
        Map<String, String> mapCompare = alarmType.parameterValues;
        List<String> lstKey = new ArrayList<>();
        for (Map.Entry<String, String> entry : mapCompare.entrySet()) {
            lstKey.add(entry.getKey());
        }

        //STEP 1 : Get Data From ELK
        BoolQueryBuilder boolQueryBuilder = QueryBuilders
                .boolQuery();

        try {
            boolQueryBuilder.must(QueryBuilders
                    .rangeQuery("@timestamp")
                    .gte(parseIsoDate(fromDate))
                    .lt(parseIsoDate(toDate))
                    .includeLower(true)
                    .includeUpper(true));
            for (String strCompare : lstKey) {
                boolQueryBuilder.should(QueryBuilders.matchQuery("parameterName", strCompare)).minimumShouldMatch("1");
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(boolQueryBuilder).size(9999);
        Search.Builder builder = new Search.Builder(searchSourceBuilder.toString());
        builder.addIndex(INDEX_PERFORMANCE);
        builder.addType(TYPE_PERFORMANCE);

        SearchResult result = jestClient.execute(builder.build());
        List<UmpPerformance> lstUmpPerformance = result.getSourceAsObjectList(UmpPerformance.class);

        Map<String, String> setAlarmDetailInsert = new HashMap<>();
        for (Map.Entry<String, String> entry : mapCompare.entrySet()) {
            String strParam = entry.getKey();
            for (UmpPerformance umpPerformance : lstUmpPerformance) {
                if (strParam.equalsIgnoreCase(umpPerformance.parameterName)) {
                    String valueCompare = entry.getValue();
                    String valueDevice = umpPerformance.value;
                    if (checkCompare(valueCompare, valueDevice)) {
//                        createAlarmDetail(alarmType, umpPerformance.deviceId);
//                        count++;
                        setAlarmDetailInsert.put(umpPerformance.timeInLog, umpPerformance.deviceId);
                    }
                }
            }
        }
        Set<DeviceGroup> deviceGroups = alarmType.deviceGroups;
        //STEP 3 : If Device In Device Type

        for (Map.Entry<String, String> entry : setAlarmDetailInsert.entrySet()) {
            if (checkDeviceInAlarmType(entry.getValue(), deviceGroups)) {
                createAlarmDetail(alarmType, entry.getValue(), entry.getKey());
            }
        }
    }

    public boolean checkCompare(String valueCompare, String valueDevice) {
        boolean blReturn = false;

        try {
            if (valueCompare.equalsIgnoreCase("true") || valueCompare.equalsIgnoreCase("false")) {
                // handle with string
                if (valueCompare.equalsIgnoreCase(valueDevice)) {
                    blReturn = true;
                }
            } else {
                // handle with number
//                >;>=;<;<=
//                >50
//                50<=70
//                60
                if (!valueCompare.contains(",")) {
                    blReturn = processCompare(valueCompare, valueDevice);
                } else {
                    String[] agru = valueCompare.split(",");
                    for (String strAgru : agru) {
                        blReturn = processCompare(strAgru, valueDevice);
                        if (blReturn) {
                            break;
                        }
                    }
                }
            }
        } catch (Exception ex) {
            logger.error("Method checkCompare :" + ex.toString());
        }
        return blReturn;
    }

    public boolean processCompare(String valueCompare, String valueDevice) {
        boolean blReturn = false;
        try {
            //
            long lValueDevice = Long.parseLong(valueDevice);
            //
            int indexBegin = 0;
            int indexEnd = 0;

            String a = "";
            String b = "";
            String operator = "";

            //get index param
            if (valueCompare.contains(">") && !valueCompare.contains("=")) {
                indexBegin = valueCompare.indexOf(">");
                indexEnd = valueCompare.indexOf(">") + 1;
                operator = ">";
            }
            if (valueCompare.contains("<") && !valueCompare.contains("=")) {
                indexBegin = valueCompare.indexOf("<");
                indexEnd = valueCompare.indexOf("<") + 1;
                operator = "<";
            }
            if (valueCompare.contains(">=")) {
                indexBegin = valueCompare.indexOf(">=");
                indexEnd = valueCompare.indexOf(">=") + 2;
                operator = ">=";
            }
            if (valueCompare.contains("<=")) {
                indexBegin = valueCompare.indexOf("<=");
                indexEnd = valueCompare.indexOf("<=") + 2;
                operator = "<=";
            }
            //
            a = valueCompare.substring(0, indexBegin);
            b = valueCompare.substring(indexEnd, valueCompare.length());
            boolean haveBegin = false;

            if (a.equals("")) {
                long valueBegin = 0;
                long valueEnd = Long.parseLong(b);
                blReturn = compare(valueBegin, valueEnd, operator, lValueDevice, haveBegin);
            } else {
                haveBegin = true;
                long valueBegin = Long.parseLong(a);
                long valueEnd = Long.parseLong(b);
                blReturn = compare(valueBegin, valueEnd, operator, lValueDevice, haveBegin);
            }

        } catch (NumberFormatException nfe) {
            if (valueCompare.equals(valueDevice)) {
                return true;
            } else {
                return false;
            }
        }
        return blReturn;
    }

    public boolean compare(long valueBegin, long valueEnd, String operator, long deviceValue, boolean haveBegin) {
        boolean blReturn = false;
//        >;>=;<;<=
        if (haveBegin) {
            if (operator.equals(">")) {
                if (valueBegin > deviceValue && deviceValue > valueEnd) {
                    blReturn = true;
                }
            }
            if (operator.equals("<")) {
                if (valueBegin < deviceValue && deviceValue < valueEnd) {
                    blReturn = true;
                }
            }
            if (operator.equals(">=")) {
                if (valueBegin >= deviceValue && deviceValue >= valueEnd) {
                    blReturn = true;
                }
            }
            if (operator.equals("<=")) {
                if (valueBegin <= deviceValue && deviceValue <= valueEnd) {
                    blReturn = true;
                }
            }
        } else {
            if (operator.equals(">")) {
                if (deviceValue > valueEnd) {
                    blReturn = true;
                }
            }
            if (operator.equals("<")) {
                if (deviceValue < valueEnd) {
                    blReturn = true;
                }
            }
            if (operator.equals(">=")) {
                if (deviceValue >= valueEnd) {
                    blReturn = true;
                }
            }
            if (operator.equals("<=")) {
                if (deviceValue <= valueEnd) {
                    blReturn = true;
                }
            }
        }

        return blReturn;
    }

    public void handleRequestFail(AlarmType alarmType, String fromDate, String toDate, JestClient jestClient) throws IOException, ParseException {
        //STEP 1 : Get Data From ELK
        BoolQueryBuilder boolQueryBuilder = QueryBuilders
                .boolQuery();

        try {
            boolQueryBuilder.must(QueryBuilders
                    .rangeQuery("@timestamp")
                    .gte(parseIsoDate(fromDate))
                    .lt(parseIsoDate(toDate))
                    .includeLower(true)
                    .includeUpper(true));
            boolQueryBuilder.mustNot(QueryBuilders.matchPhrasePrefixQuery("message", "HTTP EMPTY POST"));

        } catch (ParseException e) {
            e.printStackTrace();
        }

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(boolQueryBuilder).size(9999);
        Search.Builder builder = new Search.Builder(searchSourceBuilder.toString());
        builder.addIndex(INDEX_REQUEST_FAIL);
        builder.addType(TYPE_REQUEST_FAIL);

        SearchResult result = jestClient.execute(builder.build());
        List<ElkLoggingDevice> elkLoggingDevices = result.getSourceAsObjectList(ElkLoggingDevice.class);

        // Convert data
        List<LoggingDevice> loggingDevices = new LinkedList<LoggingDevice>();
        loggingDevices = loggingDeviceService.convertToLoggingDevices(elkLoggingDevices);

        //For each device
        for (LoggingDevice loggingDevice : loggingDevices) {
            Set<DeviceGroup> deviceGroups = alarmType.deviceGroups;
            //STEP 3 : If Device In Device Type
            if (checkDeviceInAlarmType(loggingDevice.deviceId, deviceGroups)) {
                //STEP 4 : Create Alarm Detail
                createAlarmDetail(alarmType, loggingDevice.deviceId, loggingDevice.time);
            }
        }
    }

    public void handleUpdateFirmWareFail(AlarmType alarmType, String fromDate, String toDate, JestClient jestClient) throws IOException {
        //STEP 1 : Get Data From ELK
        List<ElasticsearchLogObject> logList;
        BoolQueryBuilder boolQueryBuilder = QueryBuilders
                .boolQuery();

        try {
            boolQueryBuilder.must(QueryBuilders
                    .rangeQuery("@timestamp")
                    .gte(parseIsoDate(fromDate))
                    .lt(parseIsoDate(toDate))
                    .includeLower(true)
                    .includeUpper(true));
            boolQueryBuilder.must(QueryBuilders.matchPhrasePrefixQuery("message", "download"));
            boolQueryBuilder.should(QueryBuilders.matchPhrasePrefixQuery("message", "[FAULT_TASK]"));
            boolQueryBuilder.should(QueryBuilders.matchPhrasePrefixQuery("message", "[COMPLETED_TASK]"));

        } catch (ParseException e) {
            e.printStackTrace();
        }

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(boolQueryBuilder).size(9999);
        Search.Builder builder = new Search.Builder(searchSourceBuilder.toString());
        builder.addIndex(INDEX);
        builder.addType(TYPE);

        SearchResult result = jestClient.execute(builder.build());
        logList = result.getSourceAsObjectList(ElasticsearchLogObject.class);

        for (int i = 0; i < logList.size(); i++) {
            ElasticsearchLogObject elo = logList.get(i);
            try {
                // Handle Raised Time
                String strHandleRaisedTime = elo.message;
                int beginRaisedTime = strHandleRaisedTime.lastIndexOf("[32m[");
                String strRaisedTime = strHandleRaisedTime.substring(beginRaisedTime + "[32m[".length(), strHandleRaisedTime.indexOf("]"));
                //
                if (elo.message.contains("[FAULT_TASK]")) {
                    int beginString = elo.message.lastIndexOf("[FAULT_TASK]");
                    String strMessageHandle = elo.message.substring(beginString + "[FAULT_TASK]".length(), elo.message.length());
                    String strDeviceID = removeLastChar(strMessageHandle.split(" ")[1]);
                    Set<DeviceGroup> deviceGroups = alarmType.deviceGroups;
                    //STEP 3 : If Device In Device Type
                    if (checkDeviceInAlarmType(strDeviceID, deviceGroups)) {
                        //STEP 4 : Create Alarm Detail
                        createAlarmDetail(alarmType, strDeviceID, strRaisedTime);
                    }
                } else if (elo.message.contains("[COMPLETED_TASK]")) {
                    int beginString = elo.message.lastIndexOf("[COMPLETED_TASK]");
                    String strMessageHandle = elo.message.substring(beginString + "[COMPLETED_TASK]".length(), elo.message.length());
                    String strDeviceID = removeLastChar(strMessageHandle.split(" ")[1]);
                    Set<DeviceGroup> deviceGroups = alarmType.deviceGroups;
                    //STEP 3 : If Device In Device Type
                    if (checkDeviceInAlarmType(strDeviceID, deviceGroups)) {
                        //Convert timestamp to fromdate
                        //Create new from date after get the COMPLETED_TASK
                        String strFromDateReboot = convertTimeStampToDate(elo.timestamp);
                        //
                        //Call Quartz Job
                        long unixTime = System.currentTimeMillis() / 1000L;
                        JobDetail job = JobBuilder.newJob(AlarmAsynchronousQuartzJob.class).withIdentity("HandleRebootFail_" + strDeviceID + "_" + unixTime).build();
                        //
                        job.getJobDataMap().put("strDeviceID", strDeviceID);
                        job.getJobDataMap().put("strFromDateReboot", strFromDateReboot);
                        job.getJobDataMap().put("strRaisedTime", strRaisedTime);
                        job.getJobDataMap().put("alarmType", alarmType);
                        //Start After 3 Min
                        Trigger trigger = TriggerBuilder.newTrigger().withIdentity("Trigger_HandleRebootFail_" + strDeviceID + "_" + unixTime)
                                .startAt(futureDate(3, DateBuilder.IntervalUnit.MINUTE))
                                .build();
                        scheduler.scheduleJob(job, trigger);
                        //
                    }
                }
            } catch (Exception ex) {
                logger.error("Unknow Exception " + ex);
            }
        }
    }

    public void handleConfigurationFail(AlarmType alarmType, String fromDate, String toDate, JestClient jestClient) throws IOException {
        //STEP 1 : Get Data From ELK
        List<ElasticsearchLogObject> logList;
        BoolQueryBuilder boolQueryBuilder = QueryBuilders
                .boolQuery();

        try {
            boolQueryBuilder.must(QueryBuilders
                    .rangeQuery("@timestamp")
                    .gte(parseIsoDate(fromDate))
                    .lt(parseIsoDate(toDate))
                    .includeLower(true)
                    .includeUpper(true));
            boolQueryBuilder.must(QueryBuilders.matchPhrasePrefixQuery("message", "[FAULT_TASK]"));
            boolQueryBuilder.should(QueryBuilders.matchPhrasePrefixQuery("message", "setParameterValues"));
            boolQueryBuilder.should(QueryBuilders.matchPhrasePrefixQuery("message", "addObject"));

        } catch (ParseException e) {
            e.printStackTrace();
        }

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(boolQueryBuilder).size(9999);
        Search.Builder builder = new Search.Builder(searchSourceBuilder.toString());
        builder.addIndex(INDEX);
        builder.addType(TYPE);

        SearchResult result = jestClient.execute(builder.build());
        logList = result.getSourceAsObjectList(ElasticsearchLogObject.class);
        //STEP 2 : For Each AlarmLogList Get DeviceID
        for (int i = 0; i < logList.size(); i++) {
            logger.info("Log Size Handle : " + logList.size());
            ElasticsearchLogObject elo = logList.get(i);
            try {
                // Handle Raised Time
                String strHandleRaisedTime = elo.message;
                int beginRaisedTime = strHandleRaisedTime.lastIndexOf("[32m[");
                String strRaisedTime = strHandleRaisedTime.substring(beginRaisedTime + "[32m[".length(), strHandleRaisedTime.indexOf("]"));
                //

                int beginString = elo.message.lastIndexOf("[FAULT_TASK]");
                String strMessageHandle = elo.message.substring(beginString + "[FAULT_TASK]".length(), elo.message.length());
                String strDeviceID = removeLastChar(strMessageHandle.split(" ")[1]);
                Set<DeviceGroup> deviceGroups = alarmType.deviceGroups;
                //STEP 3 : If Device In Device Type
                if (checkDeviceInAlarmType(strDeviceID, deviceGroups)) {
                    //STEP 4 : Create Alarm Detail
                    createAlarmDetail(alarmType, strDeviceID, strRaisedTime);
                }
            } catch (Exception ex) {
                logger.error("Unknow Exception " + ex);
            }
        }
    }

    public void handleFactoryResetFail(AlarmType alarmType, String fromDate, String toDate, JestClient jestClient) throws IOException {
        //STEP 1 : Get Data From ELK
        List<ElasticsearchLogObject> logList;
        BoolQueryBuilder boolQueryBuilder = QueryBuilders
                .boolQuery();

        try {
            boolQueryBuilder.must(QueryBuilders
                    .rangeQuery("@timestamp")
                    .gte(parseIsoDate(fromDate))
                    .lt(parseIsoDate(toDate))
                    .includeLower(true)
                    .includeUpper(true));
            boolQueryBuilder.should(QueryBuilders.matchPhrasePrefixQuery("message", "[FAULT_TASK]"));
            boolQueryBuilder.should(QueryBuilders.matchPhrasePrefixQuery("message", "[COMPLETED_TASK]"));
            boolQueryBuilder.must(QueryBuilders.matchPhrasePrefixQuery("message", "factoryReset"));

        } catch (ParseException e) {
            e.printStackTrace();
        }

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(boolQueryBuilder).size(9999);
        Search.Builder builder = new Search.Builder(searchSourceBuilder.toString());
        builder.addIndex(INDEX);
        builder.addType(TYPE);

        SearchResult result = jestClient.execute(builder.build());
        logList = result.getSourceAsObjectList(ElasticsearchLogObject.class);

        //STEP 2 : For Each AlarmLogList Get DeviceID
        for (int i = 0; i < logList.size(); i++) {
            ElasticsearchLogObject elo = logList.get(i);
            try {
                // Handle Reboot Fail
                // Case 1: [FAULT_TASK] & reboot
                // Handle Raised Time
                String strHandleRaisedTime = elo.message;
                int beginRaisedTime = strHandleRaisedTime.lastIndexOf("[32m[");
                String strRaisedTime = strHandleRaisedTime.substring(beginRaisedTime + "[32m[".length(), strHandleRaisedTime.indexOf("]"));
                //

                if (elo.message.contains("[FAULT_TASK]")) {

                    int beginString = elo.message.lastIndexOf("[FAULT_TASK]");
                    String strMessageHandle = elo.message.substring(beginString + "[FAULT_TASK]".length(), elo.message.length());
                    String strDeviceID = removeLastChar(strMessageHandle.split(" ")[1]);
                    Set<DeviceGroup> deviceGroups = alarmType.deviceGroups;
                    //STEP 3 : If Device In Device Type
                    if (checkDeviceInAlarmType(strDeviceID, deviceGroups)) {
                        //STEP 4 : Create Alarm Detail
                        createAlarmDetail(alarmType, strDeviceID, strRaisedTime);
                    }
                } // Case 2:
                //        o	[COMPLETED_TASK] & reboot
                //        o	Inform 1 BOOT
                else if (elo.message.contains("[COMPLETED_TASK]")) {
                    int beginString = elo.message.lastIndexOf("[COMPLETED_TASK]");
                    String strMessageHandle = elo.message.substring(beginString + "[COMPLETED_TASK]".length(), elo.message.length());
                    String strDeviceID = removeLastChar(strMessageHandle.split(" ")[1]);
                    //Convert timestamp to fromdate
                    //Create new from date after get the COMPLETED_TASK
                    Set<DeviceGroup> deviceGroups = alarmType.deviceGroups;
                    //STEP 3 : If Device In Device Type
                    if (checkDeviceInAlarmType(strDeviceID, deviceGroups)) {
                        String strFromDateReboot = convertTimeStampToDate(elo.timestamp);
                        //
                        //Call Quartz Job
                        long unixTime = System.currentTimeMillis() / 1000L;
                        JobDetail job = JobBuilder.newJob(AlarmAsynchronousQuartzJob.class).withIdentity("HandleRebootFail_" + strDeviceID + "_" + unixTime).build();
                        //
                        job.getJobDataMap().put("strDeviceID", strDeviceID);
                        job.getJobDataMap().put("strFromDateReboot", strFromDateReboot);
                        job.getJobDataMap().put("strRaisedTime", strRaisedTime);
                        job.getJobDataMap().put("alarmType", alarmType);
                        //Start After 3 Min
                        Trigger trigger = TriggerBuilder.newTrigger().withIdentity("Trigger_HandleRebootFail_" + strDeviceID + "_" + unixTime)
                                .startAt(futureDate(3, DateBuilder.IntervalUnit.MINUTE))
                                .build();
                        scheduler.scheduleJob(job, trigger);
                        //
                    }
                }
            } catch (Exception ex) {
                logger.error("Unknow Exception " + ex);
            }
        }
    }

    public void handleRebootFail(AlarmType alarmType, String fromDate, String toDate, JestClient jestClient) throws IOException {
        //STEP 1 : Get Data From ELK
        List<ElasticsearchLogObject> logList;
        BoolQueryBuilder boolQueryBuilder = QueryBuilders
                .boolQuery();

        try {
            boolQueryBuilder.must(QueryBuilders
                    .rangeQuery("@timestamp")
                    .gte(parseIsoDate(fromDate))
                    .lt(parseIsoDate(toDate))
                    .includeLower(true)
                    .includeUpper(true));
            boolQueryBuilder.should(QueryBuilders.matchPhrasePrefixQuery("message", "[FAULT_TASK]"));
            boolQueryBuilder.should(QueryBuilders.matchPhrasePrefixQuery("message", "[COMPLETED_TASK]"));
            boolQueryBuilder.must(QueryBuilders.matchPhrasePrefixQuery("message", "reboot"));

        } catch (ParseException e) {
            e.printStackTrace();
        }

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(boolQueryBuilder).size(9999);
        Search.Builder builder = new Search.Builder(searchSourceBuilder.toString());
        builder.addIndex(INDEX);
        builder.addType(TYPE);

        SearchResult result = jestClient.execute(builder.build());
        logList = result.getSourceAsObjectList(ElasticsearchLogObject.class);

        //STEP 2 : For Each AlarmLogList Get DeviceID
        for (int i = 0; i < logList.size(); i++) {
            ElasticsearchLogObject elo = logList.get(i);
            try {
                // Handle Reboot Fail
                // Case 1: [FAULT_TASK] & reboot

                // Handle Raised Time
                String strHandleRaisedTime = elo.message;
                int beginRaisedTime = strHandleRaisedTime.lastIndexOf("[32m[");
                String strRaisedTime = strHandleRaisedTime.substring(beginRaisedTime + "[32m[".length(), strHandleRaisedTime.indexOf("]"));
                //

                if (elo.message.contains("[FAULT_TASK]")) {
                    int beginString = elo.message.lastIndexOf("[FAULT_TASK]");
                    String strMessageHandle = elo.message.substring(beginString + "[FAULT_TASK]".length(), elo.message.length());
                    String strDeviceID = removeLastChar(strMessageHandle.split(" ")[1]);
                    Set<DeviceGroup> deviceGroups = alarmType.deviceGroups;
                    //STEP 3 : If Device In Device Type
                    if (checkDeviceInAlarmType(strDeviceID, deviceGroups)) {
                        //STEP 4 : Create Alarm Detail
                        createAlarmDetail(alarmType, strDeviceID, strRaisedTime);
                    }
                } // Case 2:
                //        o	[COMPLETED_TASK] & reboot
                //        o	Inform 1 BOOT
                else if (elo.message.contains("[COMPLETED_TASK]")) {

                    int beginString = elo.message.lastIndexOf("[COMPLETED_TASK]");
                    String strMessageHandle = elo.message.substring(beginString + "[COMPLETED_TASK]".length(), elo.message.length());
                    String strDeviceID = removeLastChar(strMessageHandle.split(" ")[1]);
                    //Convert timestamp to fromdate
                    //Create new from date after get the COMPLETED_TASK
                    Set<DeviceGroup> deviceGroups = alarmType.deviceGroups;
                    //STEP 3 : If Device In Device Type
                    if (checkDeviceInAlarmType(strDeviceID, deviceGroups)) {
                        String strFromDateReboot = convertTimeStampToDate(elo.timestamp);
                        //Call Quartz Job
                        long unixTime = System.currentTimeMillis() / 1000L;
                        JobDetail job = JobBuilder.newJob(AlarmAsynchronousQuartzJob.class).withIdentity("HandleRebootFail_" + strDeviceID + "_" + unixTime).build();
                        //
                        job.getJobDataMap().put("strDeviceID", strDeviceID);
                        job.getJobDataMap().put("strFromDateReboot", strFromDateReboot);
                        job.getJobDataMap().put("strRaisedTime", strRaisedTime);
                        job.getJobDataMap().put("alarmType", alarmType);
                        //Start After 3 Min
                        Trigger trigger = TriggerBuilder.newTrigger().withIdentity("Trigger_HandleRebootFail_" + strDeviceID + "_" + unixTime)
                                .startAt(futureDate(3, DateBuilder.IntervalUnit.MINUTE))
                                .build();
                        scheduler.scheduleJob(job, trigger);
                        //
                    }
                }
            } catch (Exception ex) {
                logger.error("Unknow Exception " + ex);
            }
        }
    }

    private String convertTimeStampToDate(String strTimeStamp) {
        strTimeStamp = strTimeStamp.replace("Z", " ");
        strTimeStamp = strTimeStamp.replace("T", " ");
        strTimeStamp = strTimeStamp.substring(0, strTimeStamp.indexOf("."));
        return strTimeStamp;
    }

    private String getTagByDevice(String deviceID) {
        String strReturn = "";
        String paramters = "_tags";
        ResponseEntity response = acsClient.getDevice(deviceID, paramters);
        String body = (String) response.getBody();
        JsonArray array = new Gson().fromJson(body, JsonArray.class);
        if (array.size() > 0) {
            JsonObject object = array.get(0).getAsJsonObject();
            if (object.get("_tags") != null) {
                strReturn = object.get("_tags").toString();
            }
        }
        return strReturn;
    }

    private Boolean checkDeviceInAlarmType(String deviceID, Set<DeviceGroup> deviceGroups) {
        Set<String> queries = new HashSet<>();
        for (DeviceGroup deviceGroup : deviceGroups) {
            queries.add(deviceGroup.query);
            if (deviceGroup.name.equals("All")) {
                return true;
            }
        }
        if (queries.size() > 0) {
            String groupQuery = "{\"$or\":[" + org.apache.commons.lang3.StringUtils.join(queries, ",") + "]}";
            String query = "{\"$and\":[{\"_id\":\"" + deviceID + "\"}," + groupQuery + "]}";
            Map<String, String> queryParams = new HashMap<String, String>();
            queryParams.put("query", query);
            ResponseEntity<String> responseEntity = acsClient.search("devices", queryParams);
            int total = Integer.parseInt(responseEntity.getHeaders().get("totalAll").get(0));
            if (total > 0) {
                return true;
            }
        }
        return false;
    }

    private static String removeLastChar(String str) {
        return str.substring(0, str.length() - 1);
    }

    public void createAlarmDetail(AlarmType alarmType, String strDeviceId, String strRaised) throws ParseException, IOException {
        //
        TimeZone tz = TimeZone.getTimeZone("GMT+0");
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
//        simpleDateFormat.setTimeZone(tz);
        Date dateRaised = simpleDateFormat.parse(strRaised);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        sdf.setTimeZone(tz);
        //
        //Check Before Insert
//        List<AlarmDetails> lstAlarmDetailsExits = alarmDetailsService.checkAlarmDetailExits(alarmType.id, strDeviceId, dateRaised.getTime());
        List<AlarmDetailELK> lstAlarmDetailELK = alarmDetailELKService.checkAlarmDetailELKExits(alarmType.id, strDeviceId, dateRaised.getTime());
        //
        if (lstAlarmDetailELK.isEmpty()) {
//            AlarmDetails alarmDetailsModel = new AlarmDetails();
//            alarmDetailsModel.alarm_type_id = alarmType.id;
//            alarmDetailsModel.alarm_type = alarmType.type;
//            alarmDetailsModel.alarm_type_name = alarmType.name;
//            alarmDetailsModel.device_id = strDeviceId;
//            alarmDetailsModel.deviceGroups = alarmType.deviceGroups;
//            alarmDetailsModel.raised = dateRaised.getTime();

            AlarmDetailELK alarmDetailElk = new AlarmDetailELK();
            alarmDetailElk.alarm_type = alarmType.type;
            alarmDetailElk.alarm_type_id = alarmType.id;
            alarmDetailElk.alarm_type_name = alarmType.name;
            alarmDetailElk.device_id = strDeviceId;
            alarmDetailElk.device_groups = new Gson().toJson(alarmType.deviceGroups);
            alarmDetailElk.raised = dateRaised.getTime();
            alarmDetailElk.timestamp = sdf.format(new Date());

//            alarmDetailsService.create(alarmDetailsModel);
            alarmDetailELKService.create(alarmDetailElk);
            logger.info("Insert AlarmDetails : " + alarmDetailElk);
        }
    }

    public List<Alarm> searchAlarm(String limit, String indexPage, String whereExp) {
        List<Alarm> alarmList = new ArrayList<Alarm>();
        if (!whereExp.isEmpty()) {
            alarmList = this.repository.search(whereExp, new PageRequest(Integer.parseInt(indexPage), Integer.parseInt(limit), new Sort(Sort.Direction.DESC, "id"))).getContent();
        } else {
            alarmList = this.repository.findAll(new PageRequest(Integer.parseInt(indexPage), Integer.parseInt(limit), new Sort(Sort.Direction.DESC, "id"))).getContent();
        }

        return alarmList;
    }

    public int countAlarm(String whereExp) {
        int count = 0;
        if (whereExp.isEmpty()) {
            count = (int) this.repository.count();
        } else {
            count = (int) this.repository.count(whereExp);
        }
        return count;
    }

    public List<Alarm> getAlarmNameByAlarmType(String alarmType) {
        List<Alarm> alarmList = new ArrayList<Alarm>();
        if (!alarmType.isEmpty()) {
            alarmList = this.repository.search("alarm_type_name like ?", "%" + alarmType + "%");
        }
        return alarmList;
    }

    public List<Alarm> getAlarmById(Long alarmTypeId) {
        List<Alarm> alarmList = new ArrayList<Alarm>();
        alarmList = this.repository.search("alarm_type_id = ?", alarmTypeId);
        return alarmList;
    }

    public List<Alarm> getAlarmActiveById(Long alarmTypeId) {
        List<Alarm> alarmList = new ArrayList<Alarm>();
        alarmList = this.repository.search("alarm_type_id =  ?  and status = 'Active' ", alarmTypeId);

        return alarmList;
    }

    public List<Alarm> viewGraphSeverityAlarm(String whereExp) {
        List<Alarm> alarmList = new ArrayList<Alarm>();
        if (!whereExp.isEmpty()) {
            alarmList = this.repository.searchWithGroupBy("severity", whereExp);
        } else {
            alarmList = this.repository.searchWithGroupBy("severity");
        }

        return alarmList;
    }

    public List<Alarm> viewGraphNumberOfAlarmType(String whereExp) {
        List<Alarm> alarmList = new ArrayList<Alarm>();
        if (!whereExp.isEmpty()) {
            alarmList = this.repository.searchWithGroupBy("alarm_name", whereExp);
        } else {
            alarmList = this.repository.searchWithGroupBy("alarm_name");
        }

        return alarmList;
    }

    public JestClient elasticSearchClient() {
        JestClientFactory jestClientFactory = new JestClientFactory();
        jestClientFactory.setHttpClientConfig(new HttpClientConfig.Builder(elasticSearchUrl)
                .multiThreaded(true)
                .build());

        return jestClientFactory.getObject();
    }

    public static String parseIsoDate(String date) throws ParseException {
        return StringUtils.convertDateToElk(date, "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    }

    public List<Alarm> checkAlarmExits(long alarmTypeId, String deviceId, long raised) {
        List<Alarm> alarmList = new ArrayList<Alarm>();
        alarmList = this.repository.search("alarm_type_id = ?"
                + " and device_id = ? and raised = ? ", alarmTypeId, deviceId, raised);
        return alarmList;
    }
}
