/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vn.ssdc.vnpt.selfCare.services;

import io.searchbox.client.JestClient;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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
import static vn.ssdc.vnpt.alarm.services.AlarmDetailELKService.INDEX_ALARM_DETAIL;
import static vn.ssdc.vnpt.alarm.services.AlarmDetailELKService.TYPE_ALARM_DETAIL;
import vn.ssdc.vnpt.elk.BaseElkService;
import vn.ssdc.vnpt.notification.model.NotificationAlarmElk;
import vn.ssdc.vnpt.selfCare.model.searchForm.SCNotificationElkSearchForm;

/**
 *
 * @author kiendt
 */
@Service
public class SelfCareServiceNotificationElk {

    private static final Logger logger = LoggerFactory.getLogger(SelfCareServiceNotificationElk.class);

    @Value("${spring.elk.index.notification_alarm}")
    private String INDEX_NOTIFICATION;

    @Value("${spring.elk.type.notification_alarm}")
    private String TYPE_NOTIFICATION;

    @Autowired
    BaseElkService baseElkService;

    @Autowired
    JestClient elasticSearchClient;

    public List<NotificationAlarmElk> search(SCNotificationElkSearchForm searchForm) {
        List<NotificationAlarmElk> notificationAlarmElks = new ArrayList<>();
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        if (searchForm.userId != null) {
            boolQueryBuilder.must(QueryBuilders.matchPhraseQuery("userId", searchForm.userId));
        }
        if (searchForm.deviceId != null) {
            boolQueryBuilder.must(QueryBuilders.matchPhraseQuery("deviceId", searchForm.userId));
        }
        if (searchForm.isSeen != null) {
            boolQueryBuilder.must(QueryBuilders.matchPhraseQuery("status", searchForm.isSeen ? 1 : 0));
        }
        if (searchForm.id != null) {
            boolQueryBuilder.must(QueryBuilders.matchPhraseQuery("_id", searchForm.id));
        }

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(boolQueryBuilder);
        searchSourceBuilder.size(9999);
        searchSourceBuilder.sort(new FieldSortBuilder("timestamp").order(SortOrder.DESC));
        if (searchForm.limit != null && searchForm.page != null) {
            searchSourceBuilder.query(boolQueryBuilder).from((searchForm.page - 1) * searchForm.limit).size(searchForm.limit);
        }
        try {
            notificationAlarmElks = get(searchSourceBuilder);
        } catch (IOException ex) {
            logger.error("findByMonitoring , error:" + ex.getMessage());
        }
        return notificationAlarmElks;
    }

    public List<NotificationAlarmElk> get(SearchSourceBuilder searchSourceBuilder) throws IOException {
        List<NotificationAlarmElk> listAlarm = new ArrayList<NotificationAlarmElk>();
        Search search = new Search.Builder(searchSourceBuilder.toString())
                .addIndex(INDEX_NOTIFICATION)
                .addType(TYPE_NOTIFICATION)
                .build();
        SearchResult result = elasticSearchClient.execute(search);
        listAlarm = result.getSourceAsObjectList(NotificationAlarmElk.class);
        return listAlarm;
    }

    public NotificationAlarmElk update(String id, NotificationAlarmElk elk) {
        return (NotificationAlarmElk) baseElkService.updateDocument(id, elk, INDEX_NOTIFICATION, TYPE_NOTIFICATION);
    }

    public NotificationAlarmElk updateStatus(String id) {
        SCNotificationElkSearchForm form = new SCNotificationElkSearchForm();
        form.id = id;
        List<NotificationAlarmElk> data = search(form);
        NotificationAlarmElk elk = data != null ? data.get(0) : null;
        if (elk != null) {
            elk.status = 1;
            elk = update(id, elk);
            return elk;
        }
        return null;
    }

}
