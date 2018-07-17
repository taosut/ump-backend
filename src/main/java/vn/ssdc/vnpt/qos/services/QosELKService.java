/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vn.ssdc.vnpt.qos.services;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.searchbox.client.JestClient;
import io.searchbox.core.Index;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import org.apache.commons.lang3.time.DateUtils;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import vn.ssdc.vnpt.devices.model.DeviceTypeVersion;
import vn.ssdc.vnpt.devices.model.Tag;
import vn.ssdc.vnpt.devices.services.DeviceGroupService;
import vn.ssdc.vnpt.devices.services.DeviceTypeVersionService;
import vn.ssdc.vnpt.devices.services.TagService;
import vn.ssdc.vnpt.kafka.services.KafkaService;
import vn.ssdc.vnpt.logging.services.ElkService;
import vn.ssdc.vnpt.qos.model.*;
import vn.ssdc.vnpt.qos.model.searchForm.QosGraphDataSearchForm;
import vn.ssdc.vnpt.qos.services.kpifunctions.BaseHandlerKpi;
import vn.ssdc.vnpt.qos.services.kpifunctions.HandleIfFunction;
import vn.ssdc.vnpt.qos.services.kpifunctions.HandleValueFunction;
import vn.ssdc.vnpt.qos.services.kpifunctions.KpiInput;
import vn.ssdc.vnpt.selfCare.model.SCDevice;
import vn.ssdc.vnpt.selfCare.model.SCDeviceGroup;
import vn.ssdc.vnpt.selfCare.model.searchForm.SCAlarmQosDashboardSeachForm;
import vn.ssdc.vnpt.selfCare.model.searchForm.SCAlarmQosDeviceGroupSeachForm;
import vn.ssdc.vnpt.selfCare.model.searchForm.SCAlarmQosSingleDeviceSeachForm;
import vn.ssdc.vnpt.selfCare.model.searchForm.SCDeviceGroupSearchForm;
import vn.ssdc.vnpt.selfCare.services.SelfCareServiceDevice;
import vn.ssdc.vnpt.selfCare.services.SelfCareServiceDeviceGroup;
import vn.ssdc.vnpt.utils.StringUtils;
import vn.vnpt.ssdc.event.Event;

/**
 * @author kiendt
 */
@Service
@Produces(APPLICATION_JSON)
@Consumes({APPLICATION_JSON, TEXT_PLAIN})
public class QosELKService extends ElkService {

    @Autowired
    DeviceGroupService deviceGroupService;

    @Autowired
    JestClient elasticSearchClient;

    @Autowired
    DeviceTypeVersionService deviceTypeVersionService;

    @Autowired
    SelfCareServiceDeviceGroup selfCareServiceDeviceGroup;

    @Autowired
    TagService tagService;

    @Autowired
    QosGraphService qosGraphService;

    @Autowired
    QosKpiService qosKpiService;

    @Autowired
    QosELKService qosELKService;

    @Autowired
    SelfCareServiceDevice selfCareServiceDevice;

    @Autowired
    KafkaService kafkaService;

    List<BaseHandlerKpi> handleKpiService = new ArrayList<>();

    @Value("${spring.elk.index.qos}")
    public String INDEX_QOS = "qos_index_elk";

    @Value("${spring.elk.type.qos}")
    public String TYPE_QOS = "qos_type_elk";

    @Value("${spring.elk.index.threshold_qos}")
    public String INDEX_ALARM_QOS = "threshold_qos_index_elk";

    @Value("${spring.elk.type.threshold_qos}")
    public String TYPE_ALARM_QOS = "threshold_qos_type_elk";

    @Value("${spring.elk.index.alarm_qos}")
    public String INDEX_ALARM_LIST = "qos_alarm_list_index";
    
    @Value("${spring.elk.type.alarm_qos}")
    public String TYPE_ALARM_LIST = "qos_alarm_list_type";

    private static final Logger logger = LoggerFactory.getLogger(QosELKService.class);

    public void create(QosKpiDataELK qosData) throws IOException {
        String source = null;
        XContentBuilder content = jsonBuilder()
                .startObject()
                .field("deviceId", qosData.deviceId)
                .field("deviceGroupId", qosData.deviceGroupId)
                .field("graphId", qosData.graphId)
                .field("kpiId", qosData.kpiId)
                .field("kpiIndex", qosData.kpiIndex)
                .field("@timestamp", StringUtils.convertDateToElk(qosData.timestamp == null ? StringUtils.convertDateToString(0) : qosData.timestamp, "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"))
                .field("manufacture", qosData.manufacture)
                .field("modelName", qosData.modelName)
                .field("firmwareVersion", qosData.firmwareVersion);
        if (qosData.value instanceof Long) {
            source = content.field("value", (Long) qosData.value).endObject().string();
        } else if (qosData.value instanceof Integer) {
            source = content.field("value", (Integer) qosData.value).endObject().string();
        } else {
            source = content.field("textValue", qosData.value).endObject().string();
        }
        try {
            Index index = new Index.Builder(source).index(INDEX_QOS).type(TYPE_QOS).build();
            elasticSearchClient.execute(index);
        } catch (IOException ex) {
            ex.printStackTrace();
            logger.error("createStatiticsELK", ex.toString());
        }
    }

    public void create1(QosKpiDataELK qosData) throws IOException {
        String source = null;
        XContentBuilder content = jsonBuilder()
                .startObject()
                .field("deviceId", qosData.deviceId)
                .field("deviceGroupId", qosData.deviceGroupId)
                .field("graphId", qosData.graphId)
                .field("kpiId", qosData.kpiId)
                .field("kpiIndex", qosData.kpiIndex)
                .field("@timestamp", qosData.timestamp)
                .field("manufacture", qosData.manufacture)
                .field("modelName", qosData.modelName)
                .field("firmwareVersion", qosData.firmwareVersion);
        if (qosData.value instanceof Long) {
            source = content.field("value", (Long) qosData.value).endObject().string();
        } else if (qosData.value instanceof Integer) {
            source = content.field("value", (Integer) qosData.value).endObject().string();
        } else {
            source = content.field("textValue", (String) qosData.value).endObject().string();
        }
        try {
            Index index = new Index.Builder(source).index(INDEX_QOS).type(TYPE_QOS).build();
            elasticSearchClient.execute(index);
        } catch (IOException ex) {
            ex.printStackTrace();
            logger.error("createStatiticsELK", ex.getStackTrace());
        }
    }

    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    TimeZone tz = TimeZone.getTimeZone("GMT+0");

    ///////////////
    public JsonArray getAlarmSeverity(SCAlarmQosDashboardSeachForm searchForm) throws IOException {
        sdf.setTimeZone(tz);
        //
        JsonArray lstReturn = new JsonArray();
        //GET DATE TIME
        String startTime = sdf.format(searchForm.raisedFrom);
        String endTime = sdf.format(searchForm.raisedTo);
        //
        String query = "{\n"
                + "  \"size\" : 9999,\n"
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
                + "      }]\n"
                + "    }\n"
                + "  },\n"
                + "    \"aggs\": {\n"
                + "\t    \"kpiSeverityNumber\": {\n"
                + "\t      \"terms\": {\n"
                + "\t        \"field\": \"kpiSeverityNumber\",\n"
                + "\t        \"size\": 9999\n"
                + "\t      }\n"
                + "\t    }\n"
                + "\t}\n"
                + "}";

        Search search = new Search.Builder(query)
                .addIndex(INDEX_ALARM_QOS)
                .addType(TYPE_ALARM_QOS)
                .build();
        SearchResult result = elasticSearchClient.execute(search);
        try {
            JsonArray resultJson = result.getJsonObject().get("aggregations").getAsJsonObject()
                    .get("kpiSeverityNumber").getAsJsonObject().get("buckets").getAsJsonArray();

            //Always Return 3
            for (int i = 0; i < resultJson.size(); i++) {
                JsonObject jsonData = resultJson.get(i).getAsJsonObject();
                Long lKey = jsonData.get("key").getAsLong();
                Long lCount = jsonData.get("doc_count").getAsLong();
                //1 :minor
                //2 :major
                //3 :critical
                String strSeverity = "";
                if (1L == lKey) {
                    strSeverity = "minor";
                } else if (2L == lKey) {
                    strSeverity = "major";
                } else if (3L == lKey) {
                    strSeverity = "critical";
                }
                JsonObject jsonTemp = new JsonObject();
                jsonTemp.addProperty(strSeverity, lCount);
                lstReturn.add(jsonTemp);
            }
        } catch (Exception ex) {
            logger.info("Can't get data from input : *" + result.toString() + "*");
        }

        return lstReturn;
    }

    ///////////////
    ///////////////
    public JsonArray getTop5AlarmByKpi(SCAlarmQosDashboardSeachForm searchForm) throws IOException {
        //
        sdf.setTimeZone(tz);
        //
        JsonArray lstReturn = new JsonArray();
        //GET DATE TIME
        String startTime = sdf.format(searchForm.raisedFrom);
        String endTime = sdf.format(searchForm.raisedTo);
        //
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
                + "      }]\n"
                + "    }\n"
                + "  },\n"
                + "    \"aggs\": {\n"
                + "\t    \"qosKpiId\": {\n"
                + "\t      \"terms\": {\n"
                + "\t        \"field\": \"qosKpiId\",\n"
                + "\t        \"size\": 9999\n"
                + "\t      }\n"
                + "\t    }\n"
                + "\t}\n"
                + "}";

        Search search = new Search.Builder(query)
                .addIndex(INDEX_ALARM_QOS)
                .addType(TYPE_ALARM_QOS)
                .build();
        SearchResult result = elasticSearchClient.execute(search);
        try {
            JsonArray resultJson = result.getJsonObject().get("aggregations").getAsJsonObject()
                    .get("qosKpiId").getAsJsonObject().get("buckets").getAsJsonArray();

            //if larger than 5 return 5
            Integer intDataGet = 0;
            if (resultJson.size() >= 5) {
                intDataGet = 5;
            } //else return all
            else {
                intDataGet = resultJson.size();
            }

            for (int i = 0; i < intDataGet; i++) {
                JsonObject jsonData = resultJson.get(i).getAsJsonObject();
                Long lKey = jsonData.get("key").getAsLong();
                Long lCount = jsonData.get("doc_count").getAsLong();

                JsonObject jsonTemp = new JsonObject();
                try{
                    jsonTemp.addProperty(qosKpiService.get(lKey).kpiIndex, lCount);
                    lstReturn.add(jsonTemp);
                }catch (Exception ex){
                    logger.error("Can't get data from id : *" + lKey + "*");
                }
            }

        } catch (Exception ex) {
            logger.error("Can't get data from input : *" + result.toString() + "*");
        }
        return lstReturn;
    }

    ///////////////
    ///////////////
    public List<List<Object>> getAlarmTrends(SCAlarmQosDashboardSeachForm searchForm) throws IOException {
        //
        sdf.setTimeZone(tz);
        //
        List<List<Object>> lstReturn = new ArrayList<>();

        List<Object> lstHeader = new ArrayList<>();
        lstHeader.add("Date Time");
        lstHeader.add("Minor");
        lstHeader.add("Major");
        lstHeader.add("Critical");
        lstReturn.add(lstHeader);
        /////////
        int hourBetween = hoursDifference(searchForm.raisedFrom, searchForm.raisedTo);
        /////////
        for (int i = 0; i < hourBetween; i++) {
            Date startDate = DateUtils.addHours(searchForm.raisedFrom, i);
            Date endDate = DateUtils.addHours(searchForm.raisedFrom, i + 1);

            String startTime = sdf.format(startDate);
            String endTime = sdf.format(endDate);

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
                    + "      }]\n"
                    + "    }\n"
                    + "  },\n"
                    + "    \"aggs\": {\n"
                    + "\t    \"kpiSeverityNumber\": {\n"
                    + "\t      \"terms\": {\n"
                    + "\t        \"field\": \"kpiSeverityNumber\",\n"
                    + "\t        \"size\": 9999\n"
                    + "\t      }\n"
                    + "\t    }\n"
                    + "\t}\n"
                    + "}";

            Search search = new Search.Builder(query)
                    .addIndex(INDEX_ALARM_QOS)
                    .addType(TYPE_ALARM_QOS)
                    .build();
            SearchResult result = elasticSearchClient.execute(search);
            try {
                JsonArray resultJson = result.getJsonObject().get("aggregations").getAsJsonObject()
                        .get("kpiSeverityNumber").getAsJsonObject().get("buckets").getAsJsonArray();
                ///

                ///
                //Always Return 3
                if (resultJson.size() == 0) {
                    List<Object> lstData = new ArrayList<>();
                    lstData.add(endTime);
                    lstData.add(null);
                    lstData.add(null);
                    lstData.add(null);
                    lstReturn.add(lstData);
                } else {
                    List<Object> lstData = new ArrayList<>();
                    lstData.add(endTime);
                    lstData.add(null);
                    lstData.add(null);
                    lstData.add(null);

                    for (int x = 0; x < resultJson.size(); x++) {
                        JsonObject jsonData = resultJson.get(x).getAsJsonObject();
                        Long lKey = jsonData.get("key").getAsLong();
                        Long lCount = jsonData.get("doc_count").getAsLong();
                        //1 :minor
                        //2 :major
                        //3 :critical
                        String strSeverity = "";
                        if (1L == lKey) {
                            lstData.remove(1);
                            lstData.add(1, String.valueOf(lCount));
                        } else if (2L == lKey) {
                            lstData.remove(2);
                            lstData.add(2, String.valueOf(lCount));
                        } else if (3L == lKey) {
                            lstData.remove(3);
                            lstData.add(3, String.valueOf(lCount));
                        }
                    }

                    lstReturn.add(lstData);
                }
            } catch (Exception ex) {
                logger.info("Can't get data from input : *" + result.toString() + "*");
            }
        }
        return lstReturn;
    }

    ///////////////
    ///////////////
    public JsonArray getForEachQosKpi(SCAlarmQosDashboardSeachForm searchForm, QosKpi qosKpi) throws IOException {

        sdf.setTimeZone(tz);

        JsonArray lstReturn = new JsonArray();
        String startTime = sdf.format(searchForm.raisedFrom);
        String endTime = sdf.format(searchForm.raisedTo);
        Long qosKpiId = qosKpi.id;

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
                + "          \"qosKpiId\" : {\n"
                + "            \"query\" : \"" + qosKpiId + "\",\n"
                + "            \"type\" : \"phrase\"\n"
                + "          }\n"
                + "        }\n"
                + "      } ]\n"
                + "    }\n"
                + "  },\n"
                + "    \"aggs\": {\n"
                + "\t    \"deviceGroups\": {\n"
                + "\t      \"terms\": {\n"
                + "\t        \"field\": \"deviceGroups\",\n"
                + "\t        \"size\": 9999\n"
                + "\t      }\n"
                + "\t    }\n"
                + "\t}\n"
                + "}";
        Search search = new Search.Builder(query)
                .addIndex(INDEX_ALARM_QOS)
                .addType(TYPE_ALARM_QOS)
                .build();
        SearchResult result = elasticSearchClient.execute(search);

        try {
            JsonArray resultJson = result.getJsonObject().get("aggregations").getAsJsonObject()
                    .get("deviceGroups").getAsJsonObject().get("buckets").getAsJsonArray();
            //remove all - vietnam
            for(int a = 0 ; a < resultJson.size() ; a++){
                JsonObject jsonData = resultJson.get(a).getAsJsonObject();
                Long lKey = jsonData.get("key").getAsLong();
                if(lKey==2){
                    resultJson.remove(a);
                }
            }
            //
            //if larger than 5 return 5
            Integer intDataGet = 0;
            if (resultJson.size() >= 5) {
                intDataGet = 5;
            } //else return all
            else {
                intDataGet = resultJson.size();
            }

            for (int x = 0; x < intDataGet; x++) {
                JsonObject jsonData = resultJson.get(x).getAsJsonObject();

                Long lKey = jsonData.get("key").getAsLong();
                Long lCount = jsonData.get("doc_count").getAsLong();
                try{
                    String strGroupName = deviceGroupService.get(lKey).name;

                    JsonObject jsonObject = new JsonObject();
                    jsonObject.addProperty(strGroupName, lCount);
                    lstReturn.add(jsonObject);
                }catch(Exception ex){
                    logger.error("Can't get data from kpiID : *" + lKey + "*");
                }

            }
        } catch (Exception ex) {
            logger.error("Can't get data from input : *" + result.toString() + "*");
        }

        return lstReturn;
    }

    ///////////////
    public List<List<Object>> getDataForSingleDevice(SCAlarmQosSingleDeviceSeachForm searchForm, QosKpi qosKpi) throws IOException {

        sdf.setTimeZone(tz);

        List<List<Object>> lstReturn = new ArrayList<>();

        List<Object> lstHeader = new ArrayList<>();
        lstHeader.add(qosKpi.id);

        Boolean blCheck = false;
        if (qosKpi.kpiValue.contains("=VALUE")) {
            //Nếu ko có mà là dạng số thì trả về 0
            lstHeader.add("value");
            blCheck = true;
        } else if (qosKpi.kpiValue.contains("=IF")) {
            //Nếu ko có mà là dạng chữ thì trả về null
            lstHeader.add("text");
            blCheck = false;
        }
        lstReturn.add(lstHeader);
        /////////
        searchForm.raisedFrom = DateUtils.addHours(searchForm.raisedFrom,-1);
        /////////

        int hourBetween = hoursDifference(searchForm.raisedFrom, searchForm.raisedTo);
        /////////
        for (int i = 0; i < hourBetween; i++) {
            Date startDate = DateUtils.addHours(searchForm.raisedFrom, i);
            Date endDate = DateUtils.addHours(searchForm.raisedFrom, i + 1);

            String startTime = sdf.format(startDate);
            String endTime = sdf.format(endDate);

            //
            List<Object> lstData = new ArrayList<>();
            //

            BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
            boolQueryBuilder.must(QueryBuilders
                    .rangeQuery("@timestamp")
                    .gte(startTime)
                    .lt(endTime)
                    .includeLower(true)
                    .includeUpper(true));
            boolQueryBuilder.must(QueryBuilders.matchPhraseQuery("deviceId", searchForm.deviceId));
            boolQueryBuilder.must(QueryBuilders.matchPhraseQuery("kpiId", qosKpi.id));

            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            searchSourceBuilder.query(boolQueryBuilder);
            searchSourceBuilder.size(9999);
            searchSourceBuilder.sort(new FieldSortBuilder("@timestamp").order(SortOrder.DESC));

            List<QosKpiDataELK> data = new ArrayList<>();
            try {
                data = get(searchSourceBuilder);
            } catch (IOException ex) {
                logger.error("findByMonitoring , error:" + ex.getMessage());
            }

            /////Luôn lấy bản ghi cuối cùng
            if (data.size() > 0) {
                try {
                    if (blCheck) {
                        lstData.add(endTime);
                        lstData.add(data.get(0).value.toString());
                    } else {
                        lstData.add(endTime);
                        lstData.add(data.get(0).textValue);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            else {
//                if (qosKpi.kpiValue.contains("=VALUE")) {
//                    //Nếu ko có mà là dạng số thì trả về 0
//                    lstData.add(null);
//                } else if (qosKpi.kpiValue.contains("=IF")) {
//                    //Nếu ko có mà là dạng chữ thì trả về null
//                    lstData.add(null);
//                }

            }
            lstReturn.add(lstData);
        }
        return lstReturn;
    }

    ///////////////
    public List<List<Object>> getDataForDeviceGroup(SCAlarmQosDeviceGroupSeachForm searchForm) throws IOException {
        //
        sdf.setTimeZone(tz);
        //
        List<List<Object>> lstReturn = new ArrayList<>();
        /////////
        int hourBetween = hoursDifference(searchForm.raisedFrom, searchForm.raisedTo);
        ///////// GET TYPE QOS
        QosKpi qosKpi = qosKpiService.get(searchForm.kpiId);
        if (qosKpi.kpiFormula != null) {
            ///Handle Header
            List<Object> lstHeader = new ArrayList<>();
            long qosDeviceGroupType = 0L;
            lstHeader.add("dateTime");
            for (String strFormula : qosKpi.kpiFormula) {
                if ("SUM".equalsIgnoreCase(strFormula)) {
                    qosDeviceGroupType = 1L;
                    lstHeader.add("sum");
                } else if ("AVG".equalsIgnoreCase(strFormula)) {
                    qosDeviceGroupType = 2L;
                    lstHeader.add("avg");
                } else if ("MAX".equalsIgnoreCase(strFormula)) {
                    qosDeviceGroupType = 3L;
                    lstHeader.add("max");
                } else if ("MIN".equalsIgnoreCase(strFormula)) {
                    qosDeviceGroupType = 4L;
                    lstHeader.add("min");
                }
            }

            lstReturn.add(lstHeader);
            ///Handle Data

            if (hourBetween < searchForm.timeInterval) {
                List<Object> lstData = new ArrayList<>();
                ///
                String raisedToTime = sdf.format(searchForm.raisedTo);
                lstData.add(raisedToTime);
                ///
                for (String strFormula : qosKpi.kpiFormula) {
                    if ("SUM".equalsIgnoreCase(strFormula)) {
                        qosDeviceGroupType = 1L;
                    } else if ("AVG".equalsIgnoreCase(strFormula)) {
                        qosDeviceGroupType = 2L;
                    } else if ("MAX".equalsIgnoreCase(strFormula)) {
                        qosDeviceGroupType = 3L;
                    } else if ("MIN".equalsIgnoreCase(strFormula)) {
                        qosDeviceGroupType = 4L;
                    }

                    BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
                    boolQueryBuilder.must(QueryBuilders
                            .rangeQuery("@timestamp")
                            .gt(searchForm.raisedFrom)
                            .lte(searchForm.raisedTo)
                            .includeLower(true)
                            .includeUpper(true));
                    boolQueryBuilder.must(QueryBuilders.matchPhraseQuery("qosKpiId", searchForm.kpiId));
                    boolQueryBuilder.must(QueryBuilders.matchPhraseQuery("deviceGroups", searchForm.deviceGroupId));
                    boolQueryBuilder.must(QueryBuilders.matchPhraseQuery("qosDeviceGroupType", qosDeviceGroupType));

                    SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
                    searchSourceBuilder.query(boolQueryBuilder);
                    searchSourceBuilder.size(9999);
                    searchSourceBuilder.sort(new FieldSortBuilder("@timestamp").order(SortOrder.DESC));

                    List<QosAlarmDetail> data = new ArrayList<>();
                    data = getQosAlarmDetails(searchSourceBuilder, data);
                    if (data.size() > 0) {
                        lstData.add(data.get(0).qosKpiValue.toString());
                    } else {
                        lstData.add("0");
                    }
                    ///
                    lstReturn.add(lstData);
                }
            } else {
                int timeRun = (hourBetween / searchForm.timeInterval);

                for (int i = 0; i < timeRun; i++) {
                    List<Object> lstData = new ArrayList<>();
                    Date startDate = DateUtils.addHours(searchForm.raisedFrom, i);
                    Date endDate = DateUtils.addHours(searchForm.raisedFrom, i + searchForm.timeInterval);
                    ///Add 1 milisecond to start date
                    startDate = DateUtils.addMilliseconds(startDate, 1);
                    ///
                    String startTime = sdf.format(startDate);
                    String endTime = sdf.format(endDate);
                    ///
                    lstData.add(endTime);

                    for (String strFormula : qosKpi.kpiFormula) {
                        if ("SUM".equalsIgnoreCase(strFormula)) {
                            qosDeviceGroupType = 1L;
                        } else if ("AVG".equalsIgnoreCase(strFormula)) {
                            qosDeviceGroupType = 2L;
                        } else if ("MAX".equalsIgnoreCase(strFormula)) {
                            qosDeviceGroupType = 3L;
                        } else if ("MIN".equalsIgnoreCase(strFormula)) {
                            qosDeviceGroupType = 4L;
                        }

                        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
                        boolQueryBuilder.must(QueryBuilders
                                .rangeQuery("@timestamp")
                                .gt(startTime)
                                .lte(endTime)
                                .includeLower(true)
                                .includeUpper(true));
                        boolQueryBuilder.must(QueryBuilders.matchPhraseQuery("qosKpiId", searchForm.kpiId));
                        boolQueryBuilder.must(QueryBuilders.matchPhraseQuery("deviceGroups", searchForm.deviceGroupId));
                        boolQueryBuilder.must(QueryBuilders.matchPhraseQuery("qosDeviceGroupType", qosDeviceGroupType));

                        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
                        searchSourceBuilder.query(boolQueryBuilder);
                        searchSourceBuilder.size(9999);
                        searchSourceBuilder.sort(new FieldSortBuilder("@timestamp").order(SortOrder.DESC));

                        List<QosAlarmDetail> data = new ArrayList<>();
                        data = getQosAlarmDetails(searchSourceBuilder, data);
                        if (data.size() > 0) {
                            lstData.add(data.get(0).qosKpiValue.toString());
                        } else {
                            lstData.add("0");
                        }
                    }
                    lstReturn.add(lstData);
                }
            }
        }
        /////////
        return lstReturn;
    }

    public static String QOS_INDEX_DEVICE_GROUP = "qos_index_device_group";
    public static String TYPE_INDEX_DEVICE_GROUP = "type_index_device_group";

    private List<QosAlarmDetail> getQosAlarmDetails(SearchSourceBuilder searchSourceBuilder, List<QosAlarmDetail> data) {
        try {
            Search search = new Search.Builder(searchSourceBuilder.toString())
                    .addIndex(QOS_INDEX_DEVICE_GROUP)
                    .addType(TYPE_INDEX_DEVICE_GROUP)
                    .build();
            SearchResult result = elasticSearchClient.execute(search);
            data = result.getSourceAsObjectList(QosAlarmDetail.class);
        } catch (IOException ex) {
            logger.error("findByMonitoring , error:" + ex.getMessage());
        }
        return data;
    }

    private int hoursDifference(Date date2, Date date1) {

        final int MILLI_TO_HOUR = 1000 * 60 * 60;
        return (int) (date1.getTime() - date2.getTime()) / MILLI_TO_HOUR;
    }

    public List<QosKpiDataELK> getDataELKLastRecordInTime(QosGraphDataSearchForm qosGraphDataSearchForm, Long qosKpiId) {
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        boolQueryBuilder.must(QueryBuilders
                .rangeQuery("@timestamp")
                .gte(qosGraphDataSearchForm.fromDate)
                .lt(qosGraphDataSearchForm.toDate)
                .includeLower(true)
                .includeUpper(true));
        boolQueryBuilder.must(QueryBuilders.matchPhraseQuery("graphId", qosGraphDataSearchForm.qosGraphId));
        boolQueryBuilder.must(QueryBuilders.matchPhraseQuery("kpiId", qosKpiId));

        if (null != qosGraphDataSearchForm.deviceId) {
            boolQueryBuilder.must(QueryBuilders.matchPhraseQuery("deviceId", qosGraphDataSearchForm.deviceId));
        }

        if (null != qosGraphDataSearchForm.deviceGroupId) {
            boolQueryBuilder.must(QueryBuilders.matchPhraseQuery("deviceGroupId", qosGraphDataSearchForm.deviceGroupId));
        }

        if (null != qosGraphDataSearchForm.manufacture) {
            boolQueryBuilder.must(QueryBuilders.matchPhraseQuery("manufacture", qosGraphDataSearchForm.manufacture));
        }

        if (null != qosGraphDataSearchForm.modelName) {
            boolQueryBuilder.must(QueryBuilders.matchPhraseQuery("modelName", qosGraphDataSearchForm.modelName));
        }

        if (null != qosGraphDataSearchForm.firmwareVersion) {
            boolQueryBuilder.must(QueryBuilders.matchPhraseQuery("firmwareVersion", qosGraphDataSearchForm.firmwareVersion));
        }

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(boolQueryBuilder);
        searchSourceBuilder.size(1);
        searchSourceBuilder.sort(new FieldSortBuilder("@timestamp").order(SortOrder.DESC));

        List<QosKpiDataELK> data = new ArrayList<>();
        try {
            data = get(searchSourceBuilder);
        } catch (IOException ex) {
            logger.error("findByMonitoring , error:" + ex.getMessage());
        }
        return data;
    }

    public List<QosKpiDataELK> get(SearchSourceBuilder searchSourceBuilder) throws IOException {
        List<QosKpiDataELK> listAlarm = new ArrayList<QosKpiDataELK>();
        Search search = new Search.Builder(searchSourceBuilder.toString())
                .addIndex(INDEX_QOS)
                .addType(TYPE_QOS)
                .build();
        SearchResult result = elasticSearchClient.execute(search);
        listAlarm = result.getSourceAsObjectList(QosKpiDataELK.class);
        return listAlarm;
    }

    public List<QosGraph> getQosGraphByDeviceId(String deviceId) throws ParseException {
        //step 1 get all profile of device
        SCDevice device = selfCareServiceDevice.getDevice(deviceId);
        DeviceTypeVersion deviceTypeVersion = deviceTypeVersionService.findByFirmwareVersion(device.firmwareVersion);
        List<Tag> tags = tagService.findSynchronizedByDeviceTypeVersion(deviceTypeVersion.id);
        // step 2, get all qosGraph theo profile cua device
        Set<Long> profileIds = new HashSet<>();
        for (Tag tag : tags) {
            profileIds.add(tag.id);
        }

        List<QosGraph> qosGraphs = qosGraphService.findByProfileIds(profileIds);
        return qosGraphs;
    }

    public List<QosKpi> getQosKpiByDeviceId(String deviceId) throws ParseException {
        //step 1 get all profile of device
        SCDevice device = selfCareServiceDevice.getDevice(deviceId);
        DeviceTypeVersion deviceTypeVersion = deviceTypeVersionService.findByFirmwareVersion(device.firmwareVersion);
        List<Tag> tags = tagService.findSynchronizedByDeviceTypeVersion(deviceTypeVersion.id);
        // step 2, get all qosGraph theo profile cua device
        Set<Long> profileIds = new HashSet<>();
        for (Tag tag : tags) {
            profileIds.add(tag.id);
        }

        List<QosKpi> qosKpis = qosKpiService.findByProfileIds(profileIds);
        return qosKpis;
    }

    public void processDataFromMessageQueue(SCDevice device, Event event) throws IOException, ParseException {
        List<QosKpiDataELK> dataElks = new ArrayList<>();
        // sttep 1 get List<QosKpi> from device
        List<QosKpi> qosKpis = getQosKpiByDeviceId(device.id);
        // step 2 tao map param- value tu event cua message
        Map<String, String> mapParameterValue = new HashMap<>();
        Set<List<Object>> parameterValues = new Gson().fromJson(event.message.get("parameterValues"), Set.class);
        for (List<Object> setTmp : parameterValues) {
            mapParameterValue.put((String) setTmp.get(0), String.valueOf(setTmp.get(1)));
        }
        // step 3 tao du lieu trong ELK
        for (QosKpi qosKpi : qosKpis) {
            qosKpi.setKpiChartType();
            dataElks.addAll(handleQosGraph(qosKpi, device, mapParameterValue));
        }
        // insert to dataabse
        String timeStamp = StringUtils.convertDateToElk(StringUtils.convertDateToString(0), "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        for (QosKpiDataELK tmp : dataElks) {
            tmp.timestamp = timeStamp;
            qosELKService.create(tmp);
        }
        // insert data to kafka
        Map<Long, QosKpiDataELK> mapNotDuplicate = new HashMap<>();

        for (QosKpiDataELK tmp : dataElks) {
            // set trung timestamp de ban vao threshold toppic
            tmp.timestamp = timeStamp;
            tmp.serialNumber = device.serialNumber;
            kafkaService.sendToThresholdTopic(tmp.toMessage());
            // dat timestamp bang null, dung set de loc trung cac qoskpi thuoc nhieu devicegroup
            tmp.deviceGroupId = null;
            mapNotDuplicate.put(tmp.kpiId, tmp);
        }

        for (Map.Entry<Long, QosKpiDataELK> tmp : mapNotDuplicate.entrySet()) {
            kafkaService.sendToAlarmListTopic(tmp.getValue().toMessage());
        }

    }

    public void processDataFromMessageQueue1(SCDevice device, Event event, String time) throws IOException, ParseException {
        List<QosKpiDataELK> dataElks = new ArrayList<>();
        // sttep 1 get List<QosKpi> from device
        List<QosKpi> qosKpis = getQosKpiByDeviceId(device.id);
        // step 2 tao map param- value tu event cua message
        Map<String, String> mapParameterValue = new HashMap<>();
        Set<List<Object>> parameterValues = new Gson().fromJson(event.message.get("parameterValues"), Set.class);
        for (List<Object> setTmp : parameterValues) {
            mapParameterValue.put((String) setTmp.get(0), String.valueOf(setTmp.get(1)));
        }
        // step 3 tao du lieu trong ELK
        for (QosKpi qosKpi : qosKpis) {
            qosKpi.setKpiChartType();
            dataElks.addAll(handleQosGraph(qosKpi, device, mapParameterValue));
        }
//        for (QosGraph qosGraph : qosGraphs) {
//            dataElks.addAll(handleQosGraph(qosGraph, device, mapParameterValue));
//        }
        // insert to dataabse
        String timeStamp = time;
        for (QosKpiDataELK tmp : dataElks) {
            tmp.timestamp = time;
            if (tmp.value != null) {
                qosELKService.create1(tmp);
            } else {
                logger.info("Create Qos Elk Data null value {}", tmp.toString());
            }

        }
        // insert data to kafka
        Map<Long, QosKpiDataELK> mapNotDuplicate = new HashMap<>();

        // convert to threshold topic
        for (QosKpiDataELK tmp : dataElks) {
            // set trung timestamp de ban vao threshold toppic
            tmp.timestamp = timeStamp;
            tmp.serialNumber = device.serialNumber;
            if (tmp.value != null) {
                kafkaService.sendToThresholdTopic(tmp.toMessage());
            } else {
                logger.info("Sending to ThreshHold topin null value{}", tmp.toString());
            }
            // dat timestamp bang null, dung set de loc trung cac qoskpi thuoc nhieu devicegroup
            tmp.deviceGroupId = null;
            mapNotDuplicate.put(tmp.kpiId, tmp);
        }

        for (Map.Entry<Long, QosKpiDataELK> tmp : mapNotDuplicate.entrySet()) {
            if (tmp.getValue().value != null) {
                kafkaService.sendToAlarmListTopic(tmp.getValue().toMessage());
            } else {
                logger.info("Sending to Alarm topin null value{}", tmp.getValue().toString());
            }
        }

    }

    private List<QosKpiDataELK> handleQosGraph(QosKpi qosKpi, SCDevice device, Map<String, String> data) {
        List<QosKpiDataELK> qosDataElks = new ArrayList<>();
        // get lisrt deviceGroup contain device
        String labels = String.join(",", device.labels);
        SCDeviceGroupSearchForm scDeviceGroupSearchForm = new SCDeviceGroupSearchForm();
        scDeviceGroupSearchForm.label = labels;
        List<SCDeviceGroup> deviceGroups = selfCareServiceDeviceGroup.findByDevice(device.id);
        for (SCDeviceGroup scDeviceGroup : deviceGroups) {
            QosKpiDataELK dataEKL = new QosKpiDataELK();
            dataEKL.deviceGroupId = scDeviceGroup.id;
            dataEKL.deviceId = device.id;
            dataEKL.firmwareVersion = device.firmwareVersion;
            dataEKL.graphId = -1l;
            dataEKL.kpiId = qosKpi.id;
            dataEKL.kpiIndex = qosKpi.kpiIndex;
            dataEKL.manufacture = device.manufacturer;
            dataEKL.modelName = device.modelName;
//            if (qosKpi.getKpiChartType().equals("table")) {
//                dataEKL.textValue = (String) handleQosKpi(device, qosKpi, data);
//            } else {
            dataEKL.value = handleQosKpi(device, qosKpi, data);
//            }
            qosDataElks.add(dataEKL);
        }
        return qosDataElks;
    }

    private Object handleQosKpi(SCDevice device, QosKpi qosKpi, Map<String, String> data) {
        // implement function for handle kpi qos
        handleKpiService.clear();
        handleKpiService.add(new HandleValueFunction(new KpiInput(device.id, data, qosKpi.kpiValue)));
        handleKpiService.add(new HandleIfFunction(new KpiInput(device.id, data, qosKpi.kpiValue)));
        for (BaseHandlerKpi handler : handleKpiService) {
            try {
                Object value = handler.process();
                return value;
            } catch (Exception e) {
            }
        }
        return null;
    }

    public void createAlarmDashBoard(QosAlarmDetail qosAlarmDetail) throws IOException {
        Long qosKpiSeverityNumber = 0L;
        if ("minor".equalsIgnoreCase(qosAlarmDetail.qosKpiSeverity)) {
            qosKpiSeverityNumber = 1L;
        } else if ("major".equalsIgnoreCase(qosAlarmDetail.qosKpiSeverity)) {
            qosKpiSeverityNumber = 2L;
        } else if ("critical".equalsIgnoreCase(qosAlarmDetail.qosKpiSeverity)) {
            qosKpiSeverityNumber = 3L;
        }
        String source = jsonBuilder()
                .startObject()
                .field("qosKpiId", qosAlarmDetail.qosKpiId)
                .field("qosKpiIndex", qosAlarmDetail.qosKpiIndex)
                .field("qosKpiType", qosAlarmDetail.qosKpiType)
                .field("deviceId", qosAlarmDetail.deviceId)
                .field("deviceGroups", qosAlarmDetail.deviceGroups)
                .field("kpiSeverity", qosAlarmDetail.qosKpiSeverity)
                .field("kpiSeverityNumber", qosKpiSeverityNumber)
                .field("qosKpiValue", qosAlarmDetail.qosKpiValue)
                .field("status", "Active")
                .field("@timestamp", qosAlarmDetail.timestamp)
                .field("serialNumber", qosAlarmDetail.serialNumber)
                .field("condition", qosAlarmDetail.qosKpiCondition)
                .field("qosKpiValueText", qosAlarmDetail.qosKpiValueText)
                .field("qosDeviceGroupType", qosAlarmDetail.qosDeviceGroupType)
                .endObject().string();
        try {
            Index index = new Index.Builder(source).index(QOS_INDEX_DEVICE_GROUP).type(TYPE_INDEX_DEVICE_GROUP).build();
            elasticSearchClient.execute(index);
        } catch (IOException ex) {
            logger.error("createAlarmQosForDevice", ex.toString());
        }
    }

    public void createAlarmDemo(QosAlarmDeviceNew qosAlarmDetail) throws IOException {

        Long qosKpiSeverityNumber = 0L;
        if ("minor".equalsIgnoreCase(qosAlarmDetail.kpiSeverity)) {
            qosKpiSeverityNumber = 1L;
        } else if ("major".equalsIgnoreCase(qosAlarmDetail.kpiSeverity)) {
            qosKpiSeverityNumber = 2L;
        } else if ("critical".equalsIgnoreCase(qosAlarmDetail.kpiSeverity)) {
            qosKpiSeverityNumber = 3L;
        }
        String source = jsonBuilder()
                .startObject()
                .field("qosKpiId", qosAlarmDetail.qosKpiId)
                .field("qosKpiIndex", qosAlarmDetail.qosKpiIndex)
                .field("qosKpiType", qosAlarmDetail.qosKpiType)
                .field("deviceId", qosAlarmDetail.deviceId)
                .field("deviceGroups", qosAlarmDetail.deviceGroups)
                .field("kpiSeverity", qosAlarmDetail.kpiSeverity)
                .field("kpiSeverityNumber", qosKpiSeverityNumber)
                .field("qosKpiValue", qosAlarmDetail.qosKpiValue)
                .field("status", "Active")
                .field("@timestamp", qosAlarmDetail.timestamp)
                .field("serialNumber", qosAlarmDetail.serialNumber)
                .field("condition", qosAlarmDetail.condition)
                .field("qosKpiValueText", qosAlarmDetail.qosKpiValueText)
                .field("qosDeviceGroupType", qosAlarmDetail.qosDeviceGroupType)
                .endObject().string();
        try {
            Index index_2 = new Index.Builder(source).index(INDEX_ALARM_LIST).type(TYPE_ALARM_LIST).build();
            elasticSearchClient.execute(index_2);
            logger.info("create new from threshold_qos_index_elk success : " + qosAlarmDetail.qosKpiId);

            if (qosAlarmDetail.deviceGroups != null) {
                for (Long strIdDeviceGroups : qosAlarmDetail.deviceGroups) {
                    String source_2 = jsonBuilder()
                            .startObject()
                            .field("qosKpiId", qosAlarmDetail.qosKpiId)
                            .field("qosKpiIndex", qosAlarmDetail.qosKpiIndex)
                            .field("qosKpiType", qosAlarmDetail.qosKpiType)
                            .field("deviceId", qosAlarmDetail.deviceId)
                            .field("deviceGroups", strIdDeviceGroups)
                            .field("kpiSeverity", qosAlarmDetail.kpiSeverity)
                            .field("qosKpiValue", qosAlarmDetail.qosKpiValue)
                            .field("status", "Active")
                            .field("@timestamp", qosAlarmDetail.timestamp)
                            .field("serialNumber", qosAlarmDetail.serialNumber)
                            .field("condition", qosAlarmDetail.condition)
                            .field("qosKpiValueText", qosAlarmDetail.qosKpiValueText)
                            .field("kpiSeverityNumber", qosKpiSeverityNumber)
                            .field("qosDeviceGroupType", qosAlarmDetail.qosDeviceGroupType)
                            .endObject().string();

                    Index index = new Index.Builder(source_2).index(INDEX_ALARM_QOS).type(TYPE_ALARM_QOS).build();
                    elasticSearchClient.execute(index);
                    logger.info("create new qos_alarm_list_index success : " + qosAlarmDetail.qosKpiId);
                }
            }

        } catch (IOException ex) {
            logger.error("createAlarmQosForDevice", ex.toString());
        }
    }

}
