/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vn.ssdc.vnpt.alarm.services;

import io.searchbox.client.JestClient;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import io.searchbox.core.search.sort.Sort;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import vn.ssdc.vnpt.alarm.model.Alarm;
import vn.ssdc.vnpt.alarm.model.AlarmELK;
import vn.ssdc.vnpt.alarm.model.AlarmGraphs;
import static vn.ssdc.vnpt.alarm.services.AlarmELKService.INDEX_ALARM;
import static vn.ssdc.vnpt.alarm.services.AlarmELKService.PAGE_GROUP_FILTER;
import static vn.ssdc.vnpt.alarm.services.AlarmELKService.PAGE_RAISED_FROM;
import static vn.ssdc.vnpt.alarm.services.AlarmELKService.PAGE_RAISED_TO;
import static vn.ssdc.vnpt.alarm.services.AlarmELKService.PAGE_ROLE_GROUP;
import static vn.ssdc.vnpt.alarm.services.AlarmELKService.TYPE_ALARM;
import static vn.ssdc.vnpt.alarm.services.AlarmELKService.PAGE_SERIALNUMBER;
import vn.ssdc.vnpt.utils.StringUtils;
import vn.vnpt.ssdc.core.SsdcCrudService;
import vn.vnpt.ssdc.jdbc.factories.RepositoryFactory;

/**
 *
 * @author Admin
 */
@Service
public class AlarmGraphService extends SsdcCrudService<Long, AlarmGraphs> {

    private Logger logger = LoggerFactory.getLogger(AlarmGraphService.class);

    @Autowired
    public AlarmGraphService(RepositoryFactory repositoryFactory) {
        this.repository = repositoryFactory.create(AlarmGraphs.class);
    }

    @Autowired
    JestClient elasticSearchClient;

    public List<AlarmGraphs> getAllAlarmsBySeverity(Map<String, String> requestParams) {
        List<AlarmGraphs> listAlarmGrap = new ArrayList<>();
        try {
            List<AlarmELK> data = getAlamrsElk(requestParams);
            AlarmGraphs a1 = new AlarmGraphs();
            a1.severity = "Critical";
            a1.total = 0;
            AlarmGraphs a2 = new AlarmGraphs();
            a2.severity = "Info";
            a2.total = 0;
            AlarmGraphs a3 = new AlarmGraphs();
            a3.severity = "Major";
            a3.total = 0;
            AlarmGraphs a4 = new AlarmGraphs();
            a4.severity = "Warning";
            a4.total = 0;
            AlarmGraphs a5 = new AlarmGraphs();
            a5.severity = "Minor";
            a5.total = 0;
            for (AlarmELK tmp : data) {
                if (tmp.severity.toUpperCase().equals("CRITICAL")) {
                    a1.total++;
                } else if (tmp.severity.toUpperCase().endsWith("INFO")) {
                    a2.total++;
                } else if (tmp.severity.toUpperCase().endsWith("MAJOR")) {
                    a3.total++;
                } else if (tmp.severity.toUpperCase().endsWith("WARNING")) {
                    a4.total++;
                } else if (tmp.severity.toUpperCase().endsWith("MINOR")) {
                    a5.total++;
                }
            }
            listAlarmGrap.add(a1);
            listAlarmGrap.add(a2);
            listAlarmGrap.add(a3);
            listAlarmGrap.add(a4);
            listAlarmGrap.add(a5);
        } catch (Exception e) {
            logger.error("getAllAlarmsBySeverity ", e);
        }

        return listAlarmGrap;
    }

    public List<AlarmGraphs> getAllByAlarmType(Map<String, String> requestParams) {
        List<AlarmGraphs> listAlarmGrap = new ArrayList<>();
        try {
            List<AlarmELK> data = getAlamrsElk(requestParams);
            AlarmGraphs a1 = new AlarmGraphs();
            a1.alarmTypeName = "Request failed";
            a1.total = 0;
            AlarmGraphs a2 = new AlarmGraphs();
            a2.alarmTypeName = "Configuration device failed";
            a2.total = 0;
            AlarmGraphs a3 = new AlarmGraphs();
            a3.alarmTypeName = "Update firmware failed";
            a3.total = 0;
            AlarmGraphs a4 = new AlarmGraphs();
            a4.alarmTypeName = "Reboot failed";
            a4.total = 0;
            AlarmGraphs a5 = new AlarmGraphs();
            a5.alarmTypeName = "Factory reset failed";
            a5.total = 0;
            AlarmGraphs a6 = new AlarmGraphs();
            a6.alarmTypeName = "Alarm ThresHold";
            a6.total = 0;
            for (AlarmELK tmp : data) {
                if (tmp.alarm_type_name.equals("Request failed") || tmp.alarm_type_name.equals("REQUEST_FAIL")) {
                    a1.total++;
                } else if (tmp.alarm_type_name.equals("Configuration device failed") || tmp.alarm_type_name.equals("CONFIGURATION_FAIL")) {
                    a2.total++;
                } else if (tmp.alarm_type_name.equals("Update firmware failed") || tmp.alarm_type_name.equals("UPDATE_FIRMWARE_FAIL")) {
                    a3.total++;
                } else if (tmp.alarm_type_name.equals("Reboot failed") || tmp.alarm_type_name.equals("REBOOT_FAIL")) {
                    a4.total++;
                } else if (tmp.alarm_type_name.equals("Factory reset failed") || tmp.alarm_type_name.equals("FACTORY_RESET_FAIL")) {
                    a5.total++;
                } else {
                    a6.total++;
                }
            }
            listAlarmGrap.add(a1);
            listAlarmGrap.add(a2);
            listAlarmGrap.add(a3);
            listAlarmGrap.add(a4);
            listAlarmGrap.add(a5);
            listAlarmGrap.add(a6);
            return listAlarmGrap;
        } catch (IOException | ParseException e) {
            logger.error("getAllAlarmsBySeverity ", e);
        }
        return listAlarmGrap;
    }

    public List<AlarmELK> getAlamrsElk(Map<String, String> requestParams) throws ParseException, IOException {

        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
        List<AlarmELK> alarmList = new ArrayList<AlarmELK>();
        if (requestParams.get(PAGE_RAISED_FROM) != null
                && !requestParams.get(PAGE_RAISED_FROM).isEmpty() && requestParams.get(PAGE_RAISED_TO) == null) {

            RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery("raised");
            rangeQuery.from(String.valueOf(requestParams.get(PAGE_RAISED_FROM)));
            boolQueryBuilder.must(rangeQuery);
        }

        if (requestParams.get(PAGE_RAISED_TO) != null
                && !requestParams.get(PAGE_RAISED_TO).isEmpty() && requestParams.get(PAGE_RAISED_FROM) == null) {
            RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery("raised");
            rangeQuery.to(String.valueOf(requestParams.get(PAGE_RAISED_TO)));
            boolQueryBuilder.must(rangeQuery);
        }

        if (requestParams.get(PAGE_RAISED_TO) != null
                && !requestParams.get(PAGE_RAISED_TO).isEmpty()
                && requestParams.get(PAGE_RAISED_FROM) != null
                && !requestParams.get(PAGE_RAISED_FROM).isEmpty()) {
            RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery("raised");
            rangeQuery.to(String.valueOf(requestParams.get(PAGE_RAISED_TO)));
            rangeQuery.from(String.valueOf(requestParams.get(PAGE_RAISED_FROM)));
            boolQueryBuilder.must(rangeQuery);
        }

        if (requestParams.get(PAGE_SERIALNUMBER) != null
                && !requestParams.get(PAGE_SERIALNUMBER).isEmpty()) {
            boolQueryBuilder.must(QueryBuilders.matchPhraseQuery(PAGE_SERIALNUMBER, requestParams.get(PAGE_SERIALNUMBER)));
        }

        if (requestParams.get(PAGE_GROUP_FILTER) != null
                && !requestParams.get(PAGE_GROUP_FILTER).isEmpty()) {
            String groupFilter = "\"name\":\"" + requestParams.get(PAGE_GROUP_FILTER) + "\",";
            boolQueryBuilder.must(QueryBuilders.matchPhraseQuery(PAGE_GROUP_FILTER, groupFilter));
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
        searchSourceBuilder.size(9999);
        List<AlarmELK> data = new ArrayList<>();
        Search search = new Search.Builder(searchSourceBuilder.toString())
                .addIndex(INDEX_ALARM)
                .addType(TYPE_ALARM)
                .addSort(new Sort("_uid", Sort.Sorting.DESC))
                .build();
        SearchResult result = elasticSearchClient.execute(search);
        data = result.getSourceAsObjectList(AlarmELK.class);
        return data;
    }

}
