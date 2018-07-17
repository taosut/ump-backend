package vn.ssdc.vnpt.selfCare.services;

import io.searchbox.client.JestClient;
import io.searchbox.client.JestResult;
import io.searchbox.core.*;
import io.searchbox.core.search.sort.Sort;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import vn.ssdc.vnpt.qos.model.QosAlarmDeviceNew;
import vn.ssdc.vnpt.qos.model.QosDataELK;
import vn.ssdc.vnpt.selfCare.model.searchForm.SCAlarmQosSeachForm;
import vn.ssdc.vnpt.utils.StringUtils;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by tuanha2
 */
@Service
public class SelfCareServiceAlarmQos {
    private static final Logger logger = LoggerFactory.getLogger(SelfCareServiceAlarmQos.class);
    public static String INDEX_ALARM_DETAIL = "threshold_qos_index_elk";
    public static String TYPE_ALARM_DETAIL = "threshold_qos_type_elk";
    ////
    public static String INDEX_ALARM_LIST = "qos_alarm_list_index";
    public static String TYPE_ALARM_LIST = "qos_alarm_list_type";
    ////
    @Autowired
    JestClient elasticSearchClient;

    public QosAlarmDeviceNew getFromAlarmList(String strAlarmListId) {
        QosAlarmDeviceNew dataReturn = new QosAlarmDeviceNew();
        try {
            BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
            boolQueryBuilder.must(QueryBuilders.matchPhraseQuery("_id", strAlarmListId));

            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            searchSourceBuilder.query(boolQueryBuilder);

            List<QosAlarmDeviceNew> data = get(searchSourceBuilder);
            dataReturn = data.get(0);
        } catch (Exception ex) {
            logger.error("updateIndex , error:" + ex.getMessage());
        }
        return dataReturn;
    }

    public void deleteIndex(String strId) throws IOException {
        //1.Get Data Alarm List
        QosAlarmDeviceNew dataELK = getFromAlarmList(strId);
        long qosKpiId = dataELK.qosKpiId;
        long kpiSeverityNumber = dataELK.kpiSeverityNumber;
        String deviceId = dataELK.deviceId;
        String timeStamp = dataELK.timestamp;

        //2.Delete from qos_alarm_list_index
//        JestResult response = elasticSearchClient.execute(new Delete.Builder(strId).index(INDEX_ALARM_LIST).type(TYPE_ALARM_LIST).build());
        elasticSearchClient.execute(new Delete.Builder(strId).index(INDEX_ALARM_LIST).type(TYPE_ALARM_LIST).build());
        logger.info("Delete from qos_alarm_list_index success : " + strId);

        //3.Get from INDEX_ALARM_DETAIL
        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
        boolQueryBuilder.must(QueryBuilders.matchPhraseQuery("qosKpiId", qosKpiId));
        boolQueryBuilder.must(QueryBuilders.matchPhraseQuery("kpiSeverityNumber", kpiSeverityNumber));
        boolQueryBuilder.must(QueryBuilders.matchPhraseQuery("deviceId", deviceId));
        boolQueryBuilder.must(QueryBuilders.matchPhraseQuery("@timestamp", timeStamp));

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(boolQueryBuilder);
        try {
            List<QosDataELK> data = getFromThreshold(searchSourceBuilder);
            //4.Delete from threshold_qos_index_elk
            for (QosDataELK temp : data) {
                elasticSearchClient.execute(new Delete.Builder(temp._id).index(INDEX_ALARM_DETAIL).type(TYPE_ALARM_DETAIL).build());
                logger.info("Delete from threshold_qos_index_elk success : " + temp._id);
            }

        } catch (IOException ex) {
            logger.error("updateIndex , error:" + ex.getMessage());
        }

    }

    public void updateStatusComplete(String strId) {
        try {
            //1.Get Data Alarm List
            QosAlarmDeviceNew dataELK = getFromAlarmList(strId);
            long qosKpiId = dataELK.qosKpiId;
            long kpiSeverityNumber = dataELK.kpiSeverityNumber;
            String deviceId = dataELK.deviceId;
            String timeStamp = dataELK.timestamp;
            //2.Update from qos_alarm_list_index
            dataELK.status = "Complete";
            dataELK._id = null;
            JestResult result = elasticSearchClient.execute(
                    new Index.Builder(dataELK)
                            .index(INDEX_ALARM_LIST)
                            .type(TYPE_ALARM_LIST)
                            .id(strId)
                            .build());
            logger.info("update from qos_alarm_list_index success : " + strId);
            //3.Get Data Threshold
            BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
            boolQueryBuilder.must(QueryBuilders.matchPhraseQuery("qosKpiId", qosKpiId));
            boolQueryBuilder.must(QueryBuilders.matchPhraseQuery("kpiSeverityNumber", kpiSeverityNumber));
            boolQueryBuilder.must(QueryBuilders.matchPhraseQuery("deviceId", deviceId));
            boolQueryBuilder.must(QueryBuilders.matchPhraseQuery("@timestamp", timeStamp));

            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            searchSourceBuilder.query(boolQueryBuilder);
            List<QosDataELK> data = getFromThreshold(searchSourceBuilder);

            //4.Update status from threshold_qos_index_elk
            for (QosDataELK temp : data) {
                String tempId = temp._id;
                temp.status = "Complete";
                temp._id = null;
                JestResult result_2 = elasticSearchClient.execute(
                        new Index.Builder(temp)
                                .index(INDEX_ALARM_DETAIL)
                                .type(TYPE_ALARM_DETAIL)
                                .id(tempId)
                                .build());
                logger.info("update from threshold_qos_index_elk success : " + temp._id);
            }
        } catch (Exception ex) {
            logger.error("updateStatusComplete , error:" + ex.getMessage());
        }
    }


    public List<QosAlarmDeviceNew> getPage(SCAlarmQosSeachForm scAlarmQosSeachForm) {
        List<QosAlarmDeviceNew> lstScAlarmQos = new LinkedList<QosAlarmDeviceNew>();
        try {
            SearchResult result = searchElk(scAlarmQosSeachForm);
            lstScAlarmQos = result.getSourceAsObjectList(QosAlarmDeviceNew.class);
        } catch (Exception e) {
            logger.error("getElkLoggingDevice", e);
        }
        return lstScAlarmQos;
    }

    public Integer count(SCAlarmQosSeachForm scAlarmQosSeachForm) {
        Integer count = null;
        try {
            BoolQueryBuilder boolQueryBuilder = createBoolQuery(scAlarmQosSeachForm);

            // Call elk to get data
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            searchSourceBuilder.query(boolQueryBuilder);
            Search search = new Search.Builder(searchSourceBuilder.toString())
                    .addIndex(INDEX_ALARM_LIST)
                    .addType(TYPE_ALARM_LIST)
                    .build();
            SearchResult result = elasticSearchClient.execute(search);

            count = result.getTotal();

        } catch (Exception e) {
            e.printStackTrace();
            count = Integer.valueOf(0);
        }
        return count;

    }

    private SearchResult searchElk(SCAlarmQosSeachForm scAlarmQosSeachForm) {
        try {

            BoolQueryBuilder boolQueryBuilder = createBoolQuery(scAlarmQosSeachForm);

            // Call elk to get data
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            searchSourceBuilder.query(boolQueryBuilder);
            searchSourceBuilder.query(boolQueryBuilder).from((scAlarmQosSeachForm.page - 1) * scAlarmQosSeachForm.limit).size(scAlarmQosSeachForm.limit);

            Search search = new Search.Builder(searchSourceBuilder.toString())
                    .addIndex(INDEX_ALARM_LIST)
                    .addType(TYPE_ALARM_LIST)
                    .addSort(new Sort("@timestamp", Sort.Sorting.DESC))
                    .build();

            return elasticSearchClient.execute(search);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    TimeZone tz = TimeZone.getTimeZone("GMT+0");

    public BoolQueryBuilder createBoolQuery(SCAlarmQosSeachForm scAlarmQosSeachForm) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

        sdf.setTimeZone(tz);

        String startTime = sdf.format(scAlarmQosSeachForm.raisedFrom);
        String endTime = sdf.format(scAlarmQosSeachForm.raisedTo);

        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        boolQueryBuilder.must(QueryBuilders
                .rangeQuery("@timestamp")
                .gte(startTime)
                .lt(endTime)
                .includeLower(true)
                .includeUpper(true));
        if (scAlarmQosSeachForm.device_id != null) {
            boolQueryBuilder.must(QueryBuilders.matchPhraseQuery("deviceId", String.valueOf(scAlarmQosSeachForm.device_id)));
        }
        if (scAlarmQosSeachForm.severity != null) {
            boolQueryBuilder.must(QueryBuilders.matchPhraseQuery("kpiSeverity", String.valueOf(scAlarmQosSeachForm.severity)));
        }
        if (scAlarmQosSeachForm.qosKpiId != null) {
            boolQueryBuilder.must(QueryBuilders.matchPhraseQuery("qosKpiId", String.valueOf(scAlarmQosSeachForm.qosKpiId)));
        }

        return boolQueryBuilder;
    }

    public List<QosAlarmDeviceNew> get(SearchSourceBuilder searchSourceBuilder) throws IOException {
        List<QosAlarmDeviceNew> listAlarm = new ArrayList<QosAlarmDeviceNew>();
        Search search = new Search.Builder(searchSourceBuilder.toString())
                .addIndex(INDEX_ALARM_LIST)
                .addType(TYPE_ALARM_LIST)
                .build();
        SearchResult result = elasticSearchClient.execute(search);
        listAlarm = result.getSourceAsObjectList(QosAlarmDeviceNew.class);
        return listAlarm;
    }

    public List<QosDataELK> getFromThreshold(SearchSourceBuilder searchSourceBuilder) throws IOException {
        List<QosDataELK> listAlarm = new ArrayList<QosDataELK>();
        Search search = new Search.Builder(searchSourceBuilder.toString())
                .addIndex(INDEX_ALARM_DETAIL)
                .addType(TYPE_ALARM_DETAIL)
                .build();
        SearchResult result = elasticSearchClient.execute(search);
        listAlarm = result.getSourceAsObjectList(QosDataELK.class);
        return listAlarm;
    }

}
