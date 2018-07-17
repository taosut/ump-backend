/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vn.ssdc.vnpt.alarm.services;

import com.google.gson.Gson;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestResult;
import io.searchbox.core.DeleteByQuery;
import io.searchbox.core.Index;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import io.searchbox.core.search.sort.Sort;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import vn.ssdc.vnpt.alarm.model.AlarmDetailELK;
import vn.ssdc.vnpt.alarm.model.AlarmELK;
import vn.ssdc.vnpt.logging.model.DeleteByQuery5;
import vn.ssdc.vnpt.logging.services.ElkService;
import vn.ssdc.vnpt.utils.StringUtils;

/**
 *
 * @author Admin
 */
@Service
public class AlarmDetailELKService extends ElkService {

    public static String INDEX_ALARM_DETAIL = "alarms_detail_index_elk";
    public static String TYPE_ALARM_DETAIL = "alarms_detail_type_elk";
    private static final Logger logger = LoggerFactory.getLogger(AlarmDetailELKService.class);

    @Autowired
    JestClient elasticSearchClient;

    @Autowired
    AlarmELKService alarmELKService;

    public void create(AlarmDetailELK alarmDetail) throws IOException {
        String source = jsonBuilder()
                .startObject()
                .field("device_id", alarmDetail.device_id)
                .field("alarm_type_id", String.valueOf(alarmDetail.alarm_type_id))
                .field("alarm_type", String.valueOf(alarmDetail.alarm_type))
                .field("alarm_type_name", alarmDetail.alarm_type_name)
                .field("device_groups", alarmDetail.device_groups)
                .field("raised", alarmDetail.raised)
                .field("@timestamp", alarmDetail.timestamp)
                .endObject().string();
        try {
            Index index = new Index.Builder(source).index(INDEX_ALARM_DETAIL).type(TYPE_ALARM_DETAIL).build();
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
                        .addIndex(INDEX_ALARM_DETAIL)
                        .addType(TYPE_ALARM_DETAIL)
                        .build();
                jestResult = elasticSearchClient.execute(deleteByQuery);
            } else {
                DeleteByQuery deleteByQuery = new DeleteByQuery.Builder(searchSourceBuilder.toString())
                        .addIndex(INDEX_ALARM_DETAIL)
                        .addType(TYPE_ALARM_DETAIL)
                        .build();
                jestResult = elasticSearchClient.execute(deleteByQuery);
            }

            result = jestResult.isSucceeded();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    public List<AlarmDetailELK> get(SearchSourceBuilder searchSourceBuilder) throws IOException {
        List<AlarmDetailELK> listAlarm = new ArrayList<AlarmDetailELK>();
        Search search = new Search.Builder(searchSourceBuilder.toString())
                .addIndex(INDEX_ALARM_DETAIL)
                .addType(TYPE_ALARM_DETAIL)
                .build();
        SearchResult result = elasticSearchClient.execute(search);
        listAlarm = result.getSourceAsObjectList(AlarmDetailELK.class);
        return listAlarm;
    }

    public List<AlarmDetailELK> findByMonitoring(Long alarmTypeId, String startTime, String endTime) {

        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        boolQueryBuilder.must(QueryBuilders
                .rangeQuery("@timestamp")
                .gte(StringUtils.convertDateToElk(startTime, "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"))
                .lt(StringUtils.convertDateToElk(endTime, "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"))
                .includeLower(true)
                .includeUpper(true));
        boolQueryBuilder.must(QueryBuilders.matchPhraseQuery("alarm_type_id", String.valueOf(alarmTypeId)));

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(boolQueryBuilder);
        searchSourceBuilder.size(9999);
        List<AlarmDetailELK> data = new ArrayList<>();
        try {
            data = get(searchSourceBuilder);
        } catch (IOException ex) {
            logger.error("findByMonitoring , error:" + ex.getMessage());
        }
        return data;
    }

    public List<AlarmDetailELK> checkAlarmDetailELKExits(long alarmTypeId, String deviceId, long raised) {
        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
        boolQueryBuilder.must(QueryBuilders.matchPhraseQuery("alarm_type_id", alarmTypeId));
        boolQueryBuilder.must(QueryBuilders.matchPhraseQuery("device_id", alarmTypeId));
        boolQueryBuilder.must(QueryBuilders.matchPhraseQuery("raised", alarmTypeId));

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(boolQueryBuilder);
        searchSourceBuilder.size(9999);
        List<AlarmDetailELK> data = new ArrayList<>();
        try {
            data = get(searchSourceBuilder);
        } catch (IOException ex) {
            logger.error("findByMonitoring , error:" + ex.getMessage());
        }
        return data;
    }

}
