package vn.ssdc.vnpt.alarm.model;

import com.google.gson.Gson;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestClientFactory;
import io.searchbox.client.config.HttpClientConfig;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import vn.ssdc.vnpt.alarm.services.AlarmDetailsService;
import vn.ssdc.vnpt.utils.StringUtils;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.logging.Level;
import vn.ssdc.vnpt.alarm.services.AlarmDetailELKService;

import static vn.ssdc.vnpt.alarm.services.AlarmService.parseIsoDate;

/**
 * Created by Admin on 8/21/2017.
 */
public class AlarmAsynchronousQuartzJob implements Job {

    public static final Logger logger = LoggerFactory.getLogger(AlarmAsynchronousQuartzJob.class);

    public static String INDEX = "logging_cwmp";
    public static String TYPE = "logging_cwmp";
    @Value("${elasticSearchUrl}")
    public String elasticSearchUrl;
    @Autowired
    private AlarmDetailsService alarmDetailsService;

    @Autowired
    private AlarmDetailELKService alarmDetailELKService;

    public JestClient elasticSearchClient() {
        JestClientFactory jestClientFactory = new JestClientFactory();
        jestClientFactory.setHttpClientConfig(new HttpClientConfig.Builder(elasticSearchUrl)
                .multiThreaded(true)
                .build());
        return jestClientFactory.getObject();
    }

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        JobDataMap jdm = jobExecutionContext.getMergedJobDataMap();
        JestClient jestClient = elasticSearchClient();
        //
        String strDeviceID = jdm.getString("strDeviceID");
        String strFromDate = jdm.getString("strRaisedTime");
        AlarmType alarmType = (AlarmType) jdm.get("alarmType");
        //Create String To Date Add 3 Min
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
        Date toDate = null;
        try {
            toDate = addMinutesToDate(3, sdf.parse(strFromDate));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        String strToDate = sdf.format(toDate).toString();
        String strType_0 = "";
        String strType_1 = "";

        if (alarmType.type.equalsIgnoreCase("REBOOT_FAIL")) {
            strType_0 = "Inform";
            strType_1 = "1 BOOT";
        } else if (alarmType.type.equalsIgnoreCase("FACTORY_RESET_FAIL")) {
            strType_0 = "Inform";
            strType_1 = "0 BOOTSTRAP";
        } else if (alarmType.type.equalsIgnoreCase("UPDATE_FIRMWARE_FAIL")) {
            strType_0 = "1 BOOT";
            strType_1 = "7 TRANSFER COMPLETE";
        }

        boolQueryBuilder = new BoolQueryBuilder();
        boolQueryBuilder.must(QueryBuilders
                .rangeQuery("@timestamp")
                .gte(StringUtils.convertDateToElk(strFromDate, "yyyy-MM-dd HH:mm:ss.S", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"))
                .lt(StringUtils.convertDateToElk(strToDate, "yyyy-MM-dd HH:mm:ss.S", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"))
                .includeLower(true)
                .includeUpper(true));
        boolQueryBuilder.must(QueryBuilders.matchPhrasePrefixQuery("message", strDeviceID + ":"));
        boolQueryBuilder.must(QueryBuilders.matchPhrasePrefixQuery("message", strType_0));
        boolQueryBuilder.must(QueryBuilders.matchPhrasePrefixQuery("message", strType_1));

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(boolQueryBuilder).size(9999);
        Search.Builder builder = new Search.Builder(searchSourceBuilder.toString());
        builder.addIndex(INDEX);
        builder.addType(TYPE);

        SearchResult result = null;
        try {
            result = jestClient.execute(builder.build());
        } catch (IOException e) {
            e.printStackTrace();
        }

        List<ElasticsearchLogObject> lstResult = result.getSourceAsObjectList(ElasticsearchLogObject.class);
        logger.info("AlarmAsynchronousQuartzJob handle from " + strFromDate + " to " + strToDate + " Result: " + lstResult.size() + " - Device ID : " + strDeviceID + " - Alarm Type :" + alarmType.type);
        if (lstResult.size() == 0) {
            try {
                createAlarmDetail(alarmType, strDeviceID, strFromDate);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
    }

    public void createAlarmDetail(AlarmType alarmType, String strDeviceId, String strRaised) throws ParseException {
        //

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        TimeZone tz = TimeZone.getTimeZone("GMT+0");


        Date dateRaised = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S").parse(strRaised);
        //
        //Check Before Insert
//        List<AlarmDetails> lstAlarmDetailsExits = alarmDetailsService.checkAlarmDetailExits(alarmType.id, strDeviceId, dateRaised.getTime());
        List<AlarmDetailELK> lstAlarmDetailELK = alarmDetailELKService.checkAlarmDetailELKExits(alarmType.id, strDeviceId, dateRaised.getTime());
        //
        logger.info("createAlarmDetail in QUART");
        if (lstAlarmDetailELK.isEmpty()) {
//            AlarmDetails alarmDetailsModel = new AlarmDetails();
//            alarmDetailsModel.alarm_type_id = alarmType.id;
//            alarmDetailsModel.alarm_type = alarmType.type;
//            alarmDetailsModel.alarm_type_name = alarmType.name;
//            alarmDetailsModel.device_id = strDeviceId;
//            alarmDetailsModel.deviceGroups = alarmType.deviceGroups;
//            alarmDetailsModel.raised = dateRaised.getTime();

//            alarmDetailsService.create(alarmDetailsModel);
//             AlarmDetails alarmDetailsModel = new AlarmDetails();
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

            try {
                //            alarmDetailsService.create(alarmDetailsModel);
                alarmDetailELKService.create(alarmDetailElk);

                // insert alarmdetail to ELK
//            logger.info("Insert AlarmDetails : " + alarmDetailsModel.alarm_type_name);
            } catch (IOException ex) {
                logger.error("AlarmAsynchronousQuartzJob createAlarmDetail " + ex.getMessage());

            }
        }
    }

    private static Date addMinutesToDate(int minutes, Date beforeTime) {
        final long ONE_MINUTE_IN_MILLIS = 60000;//millisecs

        long curTimeInMs = beforeTime.getTime();
        Date afterAddingMins = new Date(curTimeInMs + (minutes * ONE_MINUTE_IN_MILLIS));
        return afterAddingMins;
    }
}
