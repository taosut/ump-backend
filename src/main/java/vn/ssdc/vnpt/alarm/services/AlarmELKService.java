/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vn.ssdc.vnpt.alarm.services;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestResult;
import io.searchbox.core.DeleteByQuery;
import io.searchbox.core.Index;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import io.searchbox.core.search.sort.Sort;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.search.suggest.Suggest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import vn.ssdc.vnpt.alarm.model.Alarm;
import vn.ssdc.vnpt.alarm.model.AlarmELK;
import vn.ssdc.vnpt.alarm.model.AlarmGraphs;
import vn.ssdc.vnpt.devices.model.DeviceGroup;
import vn.ssdc.vnpt.devices.services.DeviceGroupService;
import vn.ssdc.vnpt.logging.model.DeleteByQuery5;
import vn.ssdc.vnpt.logging.services.ElkService;
import vn.ssdc.vnpt.utils.StringUtils;

/**
 *
 * @author Admin
 */
@Service
public class AlarmELKService extends ElkService {

    public static String INDEX_ALARM = "alarms_index_elk";
    public static String TYPE_ALARM = "alarms_type_elk";
    private static final Logger logger = LoggerFactory.getLogger(AlarmELKService.class);

    public static final String PAGE_SERIALNUMBER = "device_id";
    public static final String PAGE_SEVERITY = "severity";
    public static final String PAGE_ALARM_TYPE_NAME = "alarm_type_name";
    public static final String PAGE_ALARM_NAME = "alarmName";
    public static final String PAGE_STATUS = "status";
    public static final String PAGE_GROUP_FILTER = "device_groups";
    public static final String PAGE_RAISED_FROM = "raised_from";
    public static final String PAGE_RAISED_TO = "raised_to";
    public static final String PAGE_SEARCH_ALL = "search_all";
    public static final String PAGE_DESCRIPTION = "description";
    public static final String PAGE_ROLE_GROUP = "deviceGroupIds";
    public static final String PAGE_INDEX = "indexPage";
    public static final String PAGE_LIMIT = "limit";

    @Autowired
    JestClient elasticSearchClient;

    @Autowired
    AlarmTypeService alarmTypeService;

    @Autowired
    DeviceGroupService deviceGroupService;

    @Autowired
    AlarmGraphService alarmGraphService;

    public void create(AlarmELK alarm) throws IOException {
        String source = jsonBuilder()
                .startObject()
                .field("device_id", alarm.device_id)
                .field("alarm_type_id", String.valueOf(alarm.alarm_type_id))
                .field("alarm_type_name", alarm.alarm_type_name)
                .field("created", StringUtils.convertDateToString(0))
                .field("device_groups", alarm.device_groups)
                .field("raised", alarm.raised)
                .field("status", alarm.status)
                .field("severity", alarm.severity)
                .field("alarmName", alarm.alarmName)
                .field("@timestamp", StringUtils.convertDateToElk(StringUtils.convertDateToString(0), "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"))
                .endObject().string();
        try {
            Index index = new Index.Builder(source).index(INDEX_ALARM).type(TYPE_ALARM).build();
            elasticSearchClient.execute(index);
        } catch (IOException ex) {
            logger.error("createStatiticsELK", ex.toString());
        }
    }

    public boolean delete(SearchSourceBuilder searchSourceBuilder) {
        Boolean result = false;
        try {
            JestResult jestResult;
            if (getVersionElk() >= 5) {
                DeleteByQuery5 deleteByQuery = new DeleteByQuery5.Builder(searchSourceBuilder.toString())
                        .addIndex(INDEX_ALARM)
                        .addType(TYPE_ALARM)
                        .build();
                jestResult = elasticSearchClient.execute(deleteByQuery);
            } else {
                DeleteByQuery deleteByQuery = new DeleteByQuery.Builder(searchSourceBuilder.toString())
                        .addIndex(INDEX_ALARM)
                        .addType(TYPE_ALARM)
                        .build();
                jestResult = elasticSearchClient.execute(deleteByQuery);
            }

            result = jestResult.isSucceeded();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    public int count(SearchSourceBuilder searchSourceBuilder) throws IOException {
        Search search = new Search.Builder(searchSourceBuilder.toString())
                .addIndex(INDEX_ALARM)
                .addType(TYPE_ALARM)
                .build();
        SearchResult result = elasticSearchClient.execute(search);
        return result.getTotal() == null ? 0 : result.getTotal().intValue();
    }

    public List<AlarmELK> get(SearchSourceBuilder searchSourceBuilder) throws IOException {
        List<AlarmELK> listAlarm = new ArrayList<AlarmELK>();
        Search search = new Search.Builder(searchSourceBuilder.toString())
                .addIndex(INDEX_ALARM)
                .addType(TYPE_ALARM)
                .build();
        SearchResult result = elasticSearchClient.execute(search);
        listAlarm = result.getSourceAsObjectList(AlarmELK.class);
        return listAlarm;
    }

    public SearchSourceBuilder buildQuery(Map<String, String> requestParams, boolean isPaging) throws IOException, ParseException {
        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
        List<AlarmELK> alarmList = new ArrayList<AlarmELK>();
        if (requestParams.get(PAGE_RAISED_FROM) != null
                && !requestParams.get(PAGE_RAISED_FROM).isEmpty() && requestParams.get(PAGE_RAISED_TO) == null) {

            RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery("raised");
            rangeQuery.from(requestParams.get(PAGE_RAISED_FROM));
            boolQueryBuilder.must(rangeQuery);
        }

        if (requestParams.get(PAGE_RAISED_TO) != null
                && !requestParams.get(PAGE_RAISED_TO).isEmpty() && requestParams.get(PAGE_RAISED_FROM) == null) {
            RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery("raised");
            rangeQuery.to(requestParams.get(PAGE_RAISED_TO));
            boolQueryBuilder.must(rangeQuery);
        }

        if (requestParams.get(PAGE_RAISED_TO) != null
                && !requestParams.get(PAGE_RAISED_TO).isEmpty()
                && requestParams.get(PAGE_RAISED_FROM) != null
                && !requestParams.get(PAGE_RAISED_FROM).isEmpty()) {
            RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery("raised");
            rangeQuery.to(requestParams.get(PAGE_RAISED_TO));
            rangeQuery.from(requestParams.get(PAGE_RAISED_FROM));
            boolQueryBuilder.must(rangeQuery);
        }

        if (requestParams.get(PAGE_SERIALNUMBER) != null
                && !requestParams.get(PAGE_SERIALNUMBER).isEmpty()) {
            boolQueryBuilder.must(QueryBuilders.matchPhrasePrefixQuery(PAGE_SERIALNUMBER, requestParams.get(PAGE_SERIALNUMBER)));
        }
        if (requestParams.get(PAGE_SEVERITY) != null
                && !requestParams.get(PAGE_SEVERITY).isEmpty()) {
            boolQueryBuilder.must(QueryBuilders.matchPhrasePrefixQuery(PAGE_SEVERITY, requestParams.get(PAGE_SEVERITY)));
        }
        if (requestParams.get(PAGE_ALARM_TYPE_NAME) != null
                && !requestParams.get(PAGE_ALARM_TYPE_NAME).isEmpty()) {
            String alarmTypeParam = requestParams.get(PAGE_ALARM_TYPE_NAME);
            if (alarmTypeParam.equals("PARAMETER_VALUE") || alarmTypeParam.equals("Alarm threshold")) {
                boolQueryBuilder.mustNot(QueryBuilders.matchPhraseQuery(PAGE_ALARM_TYPE_NAME, "REQUEST_FAIL"));
                boolQueryBuilder.mustNot(QueryBuilders.matchPhraseQuery(PAGE_ALARM_TYPE_NAME, "CONFIGURATION_FAIL"));
                boolQueryBuilder.mustNot(QueryBuilders.matchPhraseQuery(PAGE_ALARM_TYPE_NAME, "UPDATE_FIRMWARE_FAIL"));
                boolQueryBuilder.mustNot(QueryBuilders.matchPhraseQuery(PAGE_ALARM_TYPE_NAME, "REBOOT_FAIL"));
                boolQueryBuilder.mustNot(QueryBuilders.matchPhraseQuery(PAGE_ALARM_TYPE_NAME, "FACTORY_RESET_FAIL"));
            } else {
                boolQueryBuilder.must(QueryBuilders.matchPhrasePrefixQuery(PAGE_ALARM_TYPE_NAME, alarmTypeParam));
            }
        }

        if (requestParams.get(PAGE_ALARM_NAME) != null
                && !requestParams.get(PAGE_ALARM_NAME).isEmpty()) {
            boolQueryBuilder.must(QueryBuilders.matchPhrasePrefixQuery(PAGE_ALARM_NAME, requestParams.get(PAGE_ALARM_NAME)));
        }
        if (requestParams.get(PAGE_STATUS) != null
                && !requestParams.get(PAGE_STATUS).isEmpty()) {
            boolQueryBuilder.must(QueryBuilders.matchPhrasePrefixQuery(PAGE_STATUS, requestParams.get(PAGE_STATUS)));
        }
        if (requestParams.get(PAGE_GROUP_FILTER) != null
                && !requestParams.get(PAGE_GROUP_FILTER).isEmpty()) {
            if (requestParams.get(PAGE_GROUP_FILTER).contains(",")) {
                BoolQueryBuilder tmpQr = new BoolQueryBuilder();
                String[] groups = requestParams.get(PAGE_GROUP_FILTER).split(",");
                for (int i = 0; i < groups.length; i++) {
                    tmpQr.should(QueryBuilders.matchPhrasePrefixQuery(PAGE_GROUP_FILTER, "\"id\":" + groups[i].replaceAll("\\s+", "")));
                }
                boolQueryBuilder.must(tmpQr);
            } else {
                boolQueryBuilder.must(QueryBuilders.matchPhrasePrefixQuery(PAGE_GROUP_FILTER, "\"id\":" + requestParams.get(PAGE_GROUP_FILTER)));
            }

        }

        if (requestParams.get(PAGE_SEARCH_ALL) != null
                && !requestParams.get(PAGE_SEARCH_ALL).isEmpty()) {
            String param = requestParams.get(PAGE_SEARCH_ALL);
            BoolQueryBuilder tmpQr = new BoolQueryBuilder();
            boolQueryBuilder.must(tmpQr);
            tmpQr.should(QueryBuilders.matchPhrasePrefixQuery(PAGE_SERIALNUMBER, param));
            tmpQr.should(QueryBuilders.matchPhrasePrefixQuery(PAGE_ALARM_TYPE_NAME, param));
            tmpQr.should(QueryBuilders.matchPhrasePrefixQuery(PAGE_SEVERITY, param));
            tmpQr.should(QueryBuilders.matchPhrasePrefixQuery(PAGE_GROUP_FILTER, param));
            tmpQr.should(QueryBuilders.matchPhrasePrefixQuery(PAGE_STATUS, param));
            tmpQr.should(QueryBuilders.matchPhrasePrefixQuery(PAGE_ALARM_NAME, param));
        }
        if (requestParams.get(PAGE_ROLE_GROUP) != null
                && !requestParams.get(PAGE_ROLE_GROUP).isEmpty()) {
            if (requestParams.get(PAGE_ROLE_GROUP).contains(",")) {
                BoolQueryBuilder tmpQr = new BoolQueryBuilder();
                String[] groups = requestParams.get(PAGE_ROLE_GROUP).split(",");
                for (int i = 0; i < groups.length; i++) {
                    tmpQr.should(QueryBuilders.matchPhrasePrefixQuery(PAGE_GROUP_FILTER, "\"id\":" + groups[i].replaceAll("\\s+", "")));
                }
                boolQueryBuilder.must(tmpQr);
            } else {
                boolQueryBuilder.must(QueryBuilders.matchPhrasePrefixQuery(PAGE_GROUP_FILTER, "\"id\":" + requestParams.get(PAGE_ROLE_GROUP)));
            }
        }
        
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(boolQueryBuilder);
        searchSourceBuilder.sort(new FieldSortBuilder("raised").order(SortOrder.DESC));
        if (isPaging) {
            searchSourceBuilder.size(Integer.valueOf(requestParams.get(PAGE_LIMIT)));
            searchSourceBuilder.from(Integer.valueOf(requestParams.get(PAGE_INDEX)) * Integer.valueOf(requestParams.get(PAGE_LIMIT)));
        } else {
            searchSourceBuilder.size(9999);
        }
        return searchSourceBuilder;
    }

    public int countAlarm(Map<String, String> requestParams, boolean isPaging) throws IOException, ParseException {
        SearchSourceBuilder searchSourceBuilder = buildQuery(requestParams, isPaging);
        return count(searchSourceBuilder);
    }

    public List<Alarm> searchAlarm(Map<String, String> requestParams, boolean isPaging) throws IOException, ParseException {
        SearchSourceBuilder searchSourceBuilder = buildQuery(requestParams, isPaging);
        List<AlarmELK> data = new ArrayList<>();
        List<Alarm> dataAlarm = new ArrayList<>();
        try {
            data = get(searchSourceBuilder);
//            Collections.sort(data, new Comparator<AlarmELK>() {
//                public int compare(AlarmELK o1, AlarmELK o2) {
//                    int i1 = (int) o1.raised;
//                    int i2 = (int) o2.raised;
//                    return i2 - i1;
//                }
//            });
            for (AlarmELK tmp : data) {
                Alarm alarm = new Alarm();
                alarm.alarmName = tmp.alarmName;
                alarm.alarmTypeId = tmp.alarm_type_id;
                alarm.alarmTypeName = tmp.alarm_type_name;
                alarm.description = tmp.description;
                alarm.deviceId = tmp.device_id;
                alarm.deviceGroups = convertToDeviceGroups(tmp.device_groups);
                alarm.raised = tmp.raised;
                alarm.severity = tmp.severity;
                alarm.status = tmp.status;
                alarm._uid = tmp._id;
                dataAlarm.add(alarm);
            }
        } catch (IOException ex) {
            logger.error("searchAlarm , error:" + ex.getMessage());
        } catch (Exception ex) {
            logger.error("searchAlarm , error:" + ex.getMessage());
        }
        return dataAlarm;
    }

    public Alarm clearAlarm(String id) {
        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
        boolQueryBuilder.must(QueryBuilders.matchPhraseQuery("_id", id));

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(boolQueryBuilder);
        List<AlarmELK> data = new ArrayList<>();
        try {
            data = get(searchSourceBuilder);
            AlarmELK alarm = data.get(0);
            alarm.status = "Cleared";
            alarm._id = null;
            JestResult result = elasticSearchClient.execute(
                    new Index.Builder(alarm)
                            .index(INDEX_ALARM)
                            .type(TYPE_ALARM)
                            .id(id)
                            .build());
        } catch (IOException ex) {
            logger.error("checkAlarmExits , error:" + ex.getMessage());
        }
        return null;
    }

    public Alarm removeAlarm(String id) {
        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
        boolQueryBuilder.must(QueryBuilders.matchPhraseQuery("_id", id));

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(boolQueryBuilder);
        List<AlarmELK> data = new ArrayList<>();
        try {
            DeleteByQuery5 deleteAllUserJohn = new DeleteByQuery5.Builder(searchSourceBuilder.toString())
                    .addIndex(INDEX_ALARM)
                    .addType(TYPE_ALARM)
                    .build();
            elasticSearchClient.execute(deleteAllUserJohn);
        } catch (IOException ex) {
            logger.error("checkAlarmExits , error:" + ex.getMessage());
        }
        return null;
    }

    public List<AlarmELK> checkAlarmExits(long alarmTypeId, String deviceId, long raised) {
        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
        boolQueryBuilder.must(QueryBuilders.matchPhraseQuery("alarm_type_id", alarmTypeId));
        boolQueryBuilder.must(QueryBuilders.matchPhraseQuery("device_id", alarmTypeId));
        boolQueryBuilder.must(QueryBuilders.matchPhraseQuery("raised", alarmTypeId));

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(boolQueryBuilder);
        searchSourceBuilder.size(9999);
        List<AlarmELK> data = new ArrayList<>();
        try {
            data = get(searchSourceBuilder);
        } catch (IOException ex) {
            logger.error("checkAlarmExits , error:" + ex.getMessage());
        }
        return data;
    }

    public List<AlarmELK> getAlarmActiveById(Long alarmTypeId) {
        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
        boolQueryBuilder.must(QueryBuilders.matchPhraseQuery("alarm_type_id", alarmTypeId));
        boolQueryBuilder.must(QueryBuilders.matchPhraseQuery("status", "Active"));

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(boolQueryBuilder);
        searchSourceBuilder.size(9999);
        List<AlarmELK> data = new ArrayList<>();
        try {
            data = get(searchSourceBuilder);
        } catch (IOException ex) {
            logger.error("checkAlarmExits , error:" + ex.getMessage());
        }
        return data;
    }

    private Set<DeviceGroup> convertToDeviceGroups(String json) throws Exception {
        json = json.replaceAll("\\\\", "");
        json = json.replace("\"{", "{");
        json = json.replace("}\"", "}");
        Set<DeviceGroup> s = new HashSet<>();
        JsonArray array = new Gson().fromJson(json, JsonArray.class);
        for (int i = 0; i < array.size(); i++) {
            JsonObject object = array.get(i).getAsJsonObject();
            JsonObject tmp = object.getAsJsonObject("query");
            object.remove("query");
            DeviceGroup deviceGroup = new Gson().fromJson(object, DeviceGroup.class);
            deviceGroup.query = tmp.toString();
            s.add(deviceGroup);
        }
        return s;
    }

    public List<AlarmGraphs> viewGraphSeverityAlarmElk(Map<String, String> requestParams) throws ParseException {

        return alarmGraphService.getAllAlarmsBySeverity(requestParams);
    }

    public List<AlarmGraphs> viewGraphNumberOfAlarmTypeElk(Map<String, String> requestParams) throws ParseException {

        return alarmGraphService.getAllByAlarmType(requestParams);
    }

    public void consumeAlarmData(String fromDate, String endDate) throws ParseException, IOException, Exception {
        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
        RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery("raised");
        rangeQuery.to(String.valueOf(StringUtils.convertDateToLong(endDate, "yyyy-MM-dd HH:mm:ss")));
        rangeQuery.from(String.valueOf(StringUtils.convertDateToLong(fromDate, "yyyy-MM-dd HH:mm:ss")));
        boolQueryBuilder.must(rangeQuery);

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(boolQueryBuilder);
        searchSourceBuilder.size(2000);
        List<AlarmELK> data = new ArrayList<>();
        data = get(searchSourceBuilder);
        // consume data for severity alarm
        List<AlarmELK> listAlarmCritical = new ArrayList<>();
        List<AlarmELK> listAlarmMajor = new ArrayList<>();
        List<AlarmELK> listAlarmMinor = new ArrayList<>();
        List<AlarmELK> listAlarmWarning = new ArrayList<>();
        List<AlarmELK> listAlarmInfo = new ArrayList<>();

        List<AlarmELK> list1 = new ArrayList<>();
        List<AlarmELK> list2 = new ArrayList<>();
        List<AlarmELK> list3 = new ArrayList<>();
        List<AlarmELK> list4 = new ArrayList<>();
        List<AlarmELK> list5 = new ArrayList<>();
        List<AlarmELK> list6 = new ArrayList<>();

        List<DeviceGroup> listGroup = deviceGroupService.getAll();

        for (DeviceGroup group : listGroup) {
            AlarmGraphs alamrGraphs = new AlarmGraphs();
            alamrGraphs.deviceGroups = new Gson().toJson(group);
            alamrGraphs.startDate = StringUtils.convertDateToLong(fromDate, "yyyy-MM-dd HH:mm:ss");
            alamrGraphs.endDate = StringUtils.convertDateToLong(endDate, "yyyy-MM-dd HH:mm:ss");
            for (AlarmELK tmp : data) {
                Set<DeviceGroup> set = convertToDeviceGroups(tmp.device_groups);
                for (DeviceGroup s : set) {
                    if (s.id.equals(group.id)) {
                        if (tmp.severity.toUpperCase().equals("CRITICAL")) {
                            listAlarmCritical.add(tmp);
                        } else if (tmp.severity.toUpperCase().endsWith("INFO")) {
                            listAlarmInfo.add(tmp);
                        } else if (tmp.severity.toUpperCase().endsWith("MAJOR")) {
                            listAlarmMajor.add(tmp);
                        } else if (tmp.severity.toUpperCase().endsWith("WARNING")) {
                            listAlarmWarning.add(tmp);
                        } else if (tmp.severity.toUpperCase().endsWith("MINOR")) {
                            listAlarmMinor.add(tmp);
                        }

                        if (tmp.alarm_type_name.equals("Request failed")) {
                            list1.add(tmp);
                        } else if (tmp.alarm_type_name.equals("Configuration device failed")) {
                            list2.add(tmp);
                        } else if (tmp.alarm_type_name.equals("Update firmware failed")) {
                            list3.add(tmp);
                        } else if (tmp.alarm_type_name.equals("Reboot failed")) {
                            list4.add(tmp);
                        } else if (tmp.alarm_type_name.equals("Factory reset failed")) {
                            list5.add(tmp);
                        } else {
                            list6.add(tmp);
                        }
                        break;
                    }
                }

            }

            if (!listAlarmCritical.isEmpty()) {
                alamrGraphs.id = null;
                alamrGraphs.total = listAlarmCritical.size();
                alamrGraphs.severity = "Critical";
                alarmGraphService.create(alamrGraphs);
            }
            if (!listAlarmInfo.isEmpty()) {
                alamrGraphs.id = null;
                alamrGraphs.total = listAlarmInfo.size();
                alamrGraphs.severity = "Info";
                alarmGraphService.create(alamrGraphs);
            }
            if (!listAlarmMajor.isEmpty()) {
                alamrGraphs.id = null;
                alamrGraphs.total = listAlarmMajor.size();
                alamrGraphs.severity = "Major";
                alarmGraphService.create(alamrGraphs);
            }
            if (!listAlarmWarning.isEmpty()) {
                alamrGraphs.id = null;
                alamrGraphs.total = listAlarmWarning.size();
                alamrGraphs.severity = "Warning";
                alarmGraphService.create(alamrGraphs);
            }
            if (!listAlarmMinor.isEmpty()) {
                alamrGraphs.id = null;
                alamrGraphs.total = listAlarmMinor.size();
                alamrGraphs.severity = "Minor";
                alarmGraphService.create(alamrGraphs);
            }

            if (!list1.isEmpty()) {
                alamrGraphs.id = null;
                alamrGraphs.total = list1.size();
                alamrGraphs.alarmTypeName = "Request failed";
                alamrGraphs.severity = null;
                alarmGraphService.create(alamrGraphs);
            }
            if (!list2.isEmpty()) {
                alamrGraphs.id = null;
                alamrGraphs.total = list2.size();
                alamrGraphs.alarmTypeName = "Configuration device failed";
                alamrGraphs.severity = null;
                alarmGraphService.create(alamrGraphs);
            }
            if (!list3.isEmpty()) {
                alamrGraphs.id = null;
                alamrGraphs.total = list3.size();
                alamrGraphs.alarmTypeName = "Update firmware failed";
                alamrGraphs.severity = null;
                alarmGraphService.create(alamrGraphs);
            }
            if (!list4.isEmpty()) {
                alamrGraphs.id = null;
                alamrGraphs.total = list4.size();
                alamrGraphs.alarmTypeName = "Reboot failed";
                alamrGraphs.severity = null;
                alarmGraphService.create(alamrGraphs);
            }
            if (!list5.isEmpty()) {
                alamrGraphs.id = null;
                alamrGraphs.total = list5.size();
                alamrGraphs.alarmTypeName = "Factory reset failed";
                alamrGraphs.severity = null;
                alarmGraphService.create(alamrGraphs);
            }
            if (!list6.isEmpty()) {
                alamrGraphs.id = null;
                alamrGraphs.total = list6.size();
                alamrGraphs.alarmTypeName = "Parameter values";
                alamrGraphs.severity = null;
                alarmGraphService.create(alamrGraphs);
            }

        }

    }

}
