package vn.ssdc.vnpt.logging.services;

import io.searchbox.client.JestClient;
import io.searchbox.core.Delete;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import io.searchbox.core.search.sort.Sort;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import vn.ssdc.vnpt.common.services.ConfigurationService;
import vn.ssdc.vnpt.logging.endpoints.LoggingDeviceEndpoint;
import vn.ssdc.vnpt.logging.model.ElkLoggingCwmp;
import vn.ssdc.vnpt.logging.model.LoggingDeviceActivity;
import vn.ssdc.vnpt.logging.model.LoggingPolicy;
import vn.ssdc.vnpt.policy.model.PolicyTask;
import vn.ssdc.vnpt.utils.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class LoggingPolicyService {

    private static final Logger logger = LoggerFactory.getLogger(LoggingDeviceEndpoint.class);

    @Autowired
    JestClient elasticSearchClient;

    @Value("${elasticSearchUrl}")
    public String elasticSearchUrl;

    @Value("${tmpDir}")
    private String tmpDir;

    @Autowired
    public ConfigurationService configurationService;

    public List<PolicyTask> getPage(int page, int limit, Long policyJobId) {
        List<PolicyTask> policyTasks = new LinkedList<PolicyTask>();
        try {

            // Create query
            BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
            boolQueryBuilder.must(QueryBuilders.queryStringQuery(
                    String.format("\"%s\"", ElkLoggingCwmp.START_TASK)
            ).field("message"));

            if (policyJobId > 0) {
                boolQueryBuilder.must(QueryBuilders.queryStringQuery(
                        String.format("\"POLICY_JOB_%s\"", policyJobId.toString())
                ).field("message"));
                boolQueryBuilder.mustNot(QueryBuilders.queryStringQuery("configurationPolicy").field("message"));
                boolQueryBuilder.mustNot(QueryBuilders.queryStringQuery("updateDiagnosticResult").field("message"));
            }

            // Call elk to get data
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            searchSourceBuilder.query(boolQueryBuilder);
            if (page != 0 && limit != 0) {
                searchSourceBuilder.query(boolQueryBuilder).from((page - 1) * limit).size(20);
            }
            Search search = new Search.Builder(searchSourceBuilder.toString())
                    .addIndex(ElkLoggingCwmp.INDEX_LOGGING_CWMP)
                    .addType(ElkLoggingCwmp.TYPE_LOGGING_CWMP)
                    .addSort(new Sort("@timestamp", Sort.Sorting.DESC))
                    .build();
            SearchResult result = elasticSearchClient.execute(search);
            List<ElkLoggingCwmp> elkLoggingCwmps = result.getSourceAsObjectList(ElkLoggingCwmp.class);

            for (ElkLoggingCwmp elkLoggingCwmp : elkLoggingCwmps) {
                LoggingPolicy loggingPolicy = elkLoggingCwmp.toLoggingPolicy();

                // Search completed task or fault task
                if (loggingPolicy.taskId != null) {
                    ElkLoggingCwmp elkLoggingCwmpCompleted = getLogByPolicyId(policyJobId, loggingPolicy.taskId, ElkLoggingCwmp.COMPLETED_TASK);
                    if (elkLoggingCwmpCompleted != null) {
                        elkLoggingCwmp = elkLoggingCwmpCompleted;
                        loggingPolicy.completed = elkLoggingCwmp.getCreated();
                        loggingPolicy.status = 1;
                    } else {
                        ElkLoggingCwmp elkLoggingCwmpFault = getLogByPolicyId(policyJobId, loggingPolicy.taskId, ElkLoggingCwmp.FAULT_TASK);
                        elkLoggingCwmp = elkLoggingCwmpFault != null ? elkLoggingCwmpFault : elkLoggingCwmp;
                        loggingPolicy.status = 2;
                        loggingPolicy.errorCode = elkLoggingCwmp.getErrorCode();
                        loggingPolicy.errorText = elkLoggingCwmp.getErrorText();
                    }
                }

                policyTasks.add(loggingPolicy.toPolicyTask());
            }

        } catch (Exception e) {
            logger.error("getPageLoggingPolicy", e);
        }

        return policyTasks;
    }

    public PolicyTask getLastCompleteTask(Long policyJobId) {
        PolicyTask policyTasks = new PolicyTask();
        try {

            // Create query
            BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
            boolQueryBuilder.must(QueryBuilders.queryStringQuery(
                    String.format("\"%s\"", ElkLoggingCwmp.COMPLETED_TASK)
            ).field("message"));

            if (policyJobId > 0) {
                boolQueryBuilder.must(QueryBuilders.queryStringQuery(
                        String.format("\"POLICY_JOB_%s\"", policyJobId.toString())
                ).field("message"));
                boolQueryBuilder.mustNot(QueryBuilders.queryStringQuery("configurationPolicy").field("message"));
                boolQueryBuilder.mustNot(QueryBuilders.queryStringQuery("updateDiagnosticResult").field("message"));
            }

            // Call elk to get data
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            searchSourceBuilder.query(boolQueryBuilder);
//            searchSourceBuilder.query(boolQueryBuilder).from((page - 1) * limit).size(20);
            Search search = new Search.Builder(searchSourceBuilder.toString())
                    .addIndex(ElkLoggingCwmp.INDEX_LOGGING_CWMP)
                    .addType(ElkLoggingCwmp.TYPE_LOGGING_CWMP)
                    .addSort(new Sort("@timestamp", Sort.Sorting.DESC))
                    .build();
            SearchResult result = elasticSearchClient.execute(search);
            List<ElkLoggingCwmp> elkLoggingCwmps = result.getSourceAsObjectList(ElkLoggingCwmp.class);
            if (!elkLoggingCwmps.isEmpty()) {
                ElkLoggingCwmp log = elkLoggingCwmps.get(0);
                policyTasks.deviceId = log.getDeviceId();
                policyTasks.policyJobId = log.getPolicyJobId();
                policyTasks.taskId = log.getTaskId();
                policyTasks.completed = log.getCreated();
                policyTasks.errorCode = log.getErrorCode();
                policyTasks.errorText = log.getErrorText();
                return policyTasks;
            }

        } catch (Exception e) {
            logger.error("getPageLoggingPolicy", e);
        }

        return null;
    }

    public List<String> getCompletedTaskIds(String deviceId, String fromDateTime, String toDateTime) throws IOException {
        List<String> listCompletedTaskIds = new ArrayList<>();

        // Create query
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        boolQueryBuilder.mustNot(QueryBuilders.queryStringQuery("configurationPolicy").field("message"));
        boolQueryBuilder.mustNot(QueryBuilders.queryStringQuery("updateDiagnosticResult").field("message"));
        boolQueryBuilder.must(QueryBuilders.queryStringQuery(
                String.format("\"%s\" AND \"%s\"", ElkLoggingCwmp.COMPLETED_TASK, deviceId)
        ).field("message"));

        // Get time expire to set from date time
        String timeExpire = configurationService.get("deviceActivityTimeSetting").value;
        if (!("").equals(fromDateTime) && timeExpire != null) {
            if (parseIsoDate(fromDateTime).compareTo(parseIsoDate(convertTimeExpire(timeExpire))) < 0) {
                fromDateTime = convertTimeExpire(timeExpire);
            }
        } else {
            fromDateTime = convertTimeExpire(timeExpire);
        }
        // Timestamp
        if (!("").equals(fromDateTime) && !("").equals(toDateTime)) {
            boolQueryBuilder.must(QueryBuilders.rangeQuery("@timestamp")
                    .gte(parseIsoDate(fromDateTime)).lt(parseIsoDate(toDateTime)));
        }
        if (!("").equals(fromDateTime) && ("").equals(toDateTime)) {
            boolQueryBuilder.must(QueryBuilders.rangeQuery("@timestamp")
                    .gte(parseIsoDate(fromDateTime)));
        }
        if (("").equals(fromDateTime) && !("").equals(toDateTime)) {
            boolQueryBuilder.must(QueryBuilders.rangeQuery("@timestamp")
                    .lt(parseIsoDate(toDateTime)));
        }
        // Call elk to get data
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(boolQueryBuilder);
        Search search = new Search.Builder(searchSourceBuilder.toString())
                .addIndex(ElkLoggingCwmp.INDEX_LOGGING_CWMP)
                .addType(ElkLoggingCwmp.TYPE_LOGGING_CWMP)
                .addSort(new Sort("@timestamp", Sort.Sorting.DESC))
                .build();
        SearchResult result = elasticSearchClient.execute(search);
        List<ElkLoggingCwmp> elkLoggingCwmps = result.getSourceAsObjectList(ElkLoggingCwmp.class);

        // Get list logging device activity
        for (ElkLoggingCwmp elkLoggingCwmp : elkLoggingCwmps) {
            listCompletedTaskIds.add(elkLoggingCwmp.getTaskId());
        }
        return listCompletedTaskIds;
    }

    public List<String> getFaultTaskIds(String deviceId, String errorCode, String errorText) throws IOException {
        List<String> listFaultTaskIds = new ArrayList<>();

        // Create query
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        boolQueryBuilder.mustNot(QueryBuilders.queryStringQuery("configurationPolicy").field("message"));
        boolQueryBuilder.mustNot(QueryBuilders.queryStringQuery("updateDiagnosticResult").field("message"));
        boolQueryBuilder.must(QueryBuilders.queryStringQuery(
                String.format("\"%s\" AND \"%s\"", ElkLoggingCwmp.FAULT_TASK, deviceId)
        ).field("message"));

        if (errorCode != null) {
            boolQueryBuilder.must(QueryBuilders.queryStringQuery(String.format("*%s*", errorCode)).field("message"));
        }

        if (errorText != null) {
            boolQueryBuilder.must(QueryBuilders.queryStringQuery(String.format("*%s*", errorText)).field("message"));
        }

        // Get time expire to set from date time
        String timeExpire = configurationService.get("deviceActivityTimeSetting").value;
        String fromDateTime = convertTimeExpire(timeExpire);
        boolQueryBuilder.must(QueryBuilders.rangeQuery("@timestamp")
                .gte(parseIsoDate(fromDateTime)));

        // Call elk to get data
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(boolQueryBuilder);
        Search search = new Search.Builder(searchSourceBuilder.toString())
                .addIndex(ElkLoggingCwmp.INDEX_LOGGING_CWMP)
                .addType(ElkLoggingCwmp.TYPE_LOGGING_CWMP)
                .addSort(new Sort("@timestamp", Sort.Sorting.DESC))
                .build();

        SearchResult result = elasticSearchClient.execute(search);
        List<ElkLoggingCwmp> elkLoggingCwmps = result.getSourceAsObjectList(ElkLoggingCwmp.class);

        // Get list logging device activity
        for (ElkLoggingCwmp elkLoggingCwmp : elkLoggingCwmps) {
            listFaultTaskIds.add(elkLoggingCwmp.getTaskId());
        }
        return listFaultTaskIds;
    }

    public List<LoggingDeviceActivity> getPageDeviceActivity(int page, int limit, String deviceId, String fromDateTime, String toDateTime, String taskName, String parameter, List<String> listTaskIds) {
        List<LoggingDeviceActivity> loggingDeviceActivities = new LinkedList<LoggingDeviceActivity>();
        try {
            // Create query
            BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
            boolQueryBuilder.must(QueryBuilders.queryStringQuery(
                    String.format("\"%s\" AND \"%s\"", ElkLoggingCwmp.START_TASK, deviceId)
            ).field("message"));
            if (taskName != null) {
                boolQueryBuilder.must(QueryBuilders.queryStringQuery(taskName).field("message"));
            }
            if (parameter != null) {
                boolQueryBuilder.must(QueryBuilders.queryStringQuery(parameter).field("message"));
            }
            if (listTaskIds != null) {
                if (listTaskIds.size() == 0) {
                    return new LinkedList<LoggingDeviceActivity>();
                } else {
                    List<String> listTaskQueries = new ArrayList<>();
                    for (String taskId : listTaskIds) {
                        listTaskQueries.add("\"TASK_ID_" + taskId + "\"");
                    }
                    boolQueryBuilder.must(QueryBuilders.queryStringQuery(String.join(" OR ", listTaskQueries)).field("message"));
                }
            }
            boolQueryBuilder.mustNot(QueryBuilders.queryStringQuery("configurationPolicy").field("message"));
            boolQueryBuilder.mustNot(QueryBuilders.queryStringQuery("updateDiagnosticResult").field("message"));

            // Get time expire to set from date time
            String timeExpire = configurationService.get("deviceActivityTimeSetting").value;
            if (!("").equals(fromDateTime) && timeExpire != null) {
                if (parseIsoDate(fromDateTime).compareTo(parseIsoDate(convertTimeExpire(timeExpire))) < 0) {
                    fromDateTime = convertTimeExpire(timeExpire);
                }
            } else {
                fromDateTime = convertTimeExpire(timeExpire);
            }

            // Timestamp
            if (!("").equals(fromDateTime) && !("").equals(toDateTime)) {
                boolQueryBuilder.must(QueryBuilders.rangeQuery("@timestamp")
                        .gte(parseIsoDate(fromDateTime)).lt(parseIsoDate(toDateTime)));
            }
            if (!("").equals(fromDateTime) && ("").equals(toDateTime)) {
                boolQueryBuilder.must(QueryBuilders.rangeQuery("@timestamp")
                        .gte(parseIsoDate(fromDateTime)));
            }
            if (("").equals(fromDateTime) && !("").equals(toDateTime)) {
                boolQueryBuilder.must(QueryBuilders.rangeQuery("@timestamp")
                        .lt(parseIsoDate(toDateTime)));
            }

            // Call elk to get data
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            searchSourceBuilder.query(boolQueryBuilder);
            searchSourceBuilder.query(boolQueryBuilder).from((page - 1) * limit).size(limit);
            Search search = new Search.Builder(searchSourceBuilder.toString())
                    .addIndex(ElkLoggingCwmp.INDEX_LOGGING_CWMP)
                    .addType(ElkLoggingCwmp.TYPE_LOGGING_CWMP)
                    .addSort(new Sort("@timestamp", Sort.Sorting.DESC))
                    .build();
            SearchResult result = elasticSearchClient.execute(search);
            List<ElkLoggingCwmp> elkLoggingCwmps = result.getSourceAsObjectList(ElkLoggingCwmp.class);

            // Get list logging device activity
            for (ElkLoggingCwmp elkLoggingCwmp : elkLoggingCwmps) {
                LoggingDeviceActivity loggingDeviceActivity = elkLoggingCwmp.toDeviceActivity();

                // Search completed task and fault task
                if (deviceId != null && !"".equals(deviceId)) {
                    ElkLoggingCwmp elkLoggingCwmpCompleted = getLogByDeviceId(deviceId, loggingDeviceActivity.taskId, ElkLoggingCwmp.COMPLETED_TASK);
                    if (elkLoggingCwmpCompleted != null) {
                        loggingDeviceActivity.completedTime = elkLoggingCwmpCompleted.getDateTime();
                    }

                    ElkLoggingCwmp elkLoggingCwmpFault = getLogByDeviceId(deviceId, loggingDeviceActivity.taskId, ElkLoggingCwmp.FAULT_TASK);
                    if (elkLoggingCwmpFault != null) {
                        loggingDeviceActivity.errorCode = elkLoggingCwmpFault.getErrorCode();
                        loggingDeviceActivity.errorText = elkLoggingCwmpFault.getErrorText();
                    }
                }

                loggingDeviceActivities.add(loggingDeviceActivity);
            }

        } catch (Exception e) {
            logger.error("getPageLoggingDeviceActivity", e);
        }

        return loggingDeviceActivities;
    }

    public List<LoggingDeviceActivity> getPageDeviceActivity(int page, int limit, String deviceId, String fromDateTime, String toDateTime) {
        return getPageDeviceActivity(page, limit, deviceId, fromDateTime, toDateTime, null, null, null);
    }

    private ElkLoggingCwmp getLogByPolicyId(Long policyJobId, String taskId, String taskType) {
        ElkLoggingCwmp elkLoggingCwmp = null;

        try {
            BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
            boolQueryBuilder.must(QueryBuilders.queryStringQuery(
                    String.format("\"%s\" AND \"TASK_ID_%s\" AND \"POLICY_JOB_%s\"", taskType, taskId, policyJobId)
            ).field("message"));
            boolQueryBuilder.mustNot(QueryBuilders.queryStringQuery("configurationPolicy").field("message"));
            boolQueryBuilder.mustNot(QueryBuilders.queryStringQuery("updateDiagnosticResult").field("message"));
            // Call elk to get data
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            searchSourceBuilder.query(boolQueryBuilder);

            Search search = new Search.Builder(searchSourceBuilder.toString())
                    .addIndex(ElkLoggingCwmp.INDEX_LOGGING_CWMP)
                    .addType(ElkLoggingCwmp.TYPE_LOGGING_CWMP)
                    .addSort(new Sort("@timestamp", Sort.Sorting.DESC))
                    .build();
            SearchResult result = elasticSearchClient.execute(search);
            List<ElkLoggingCwmp> elkLoggingCwmps = result.getSourceAsObjectList(ElkLoggingCwmp.class);
            if (elkLoggingCwmps.size() > 0) {
                elkLoggingCwmp = elkLoggingCwmps.get(0);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return elkLoggingCwmp;
    }

    private ElkLoggingCwmp getLogByDeviceId(String deviceId, String taskId, String taskType) {
        ElkLoggingCwmp elkLoggingCwmp = null;

        try {
            BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
            boolQueryBuilder.must(QueryBuilders.queryStringQuery(
                    String.format("\"%s\" AND \"TASK_ID_%s\" AND \"%s\"", taskType, taskId, deviceId)
            ).field("message"));
            boolQueryBuilder.mustNot(QueryBuilders.queryStringQuery("configurationPolicy").field("message"));
            boolQueryBuilder.mustNot(QueryBuilders.queryStringQuery("updateDiagnosticResult").field("message"));
            // Call elk to get data
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            searchSourceBuilder.query(boolQueryBuilder);

            Search search = new Search.Builder(searchSourceBuilder.toString())
                    .addIndex(ElkLoggingCwmp.INDEX_LOGGING_CWMP)
                    .addType(ElkLoggingCwmp.TYPE_LOGGING_CWMP)
                    .build();
            SearchResult result = elasticSearchClient.execute(search);
            List<ElkLoggingCwmp> elkLoggingCwmps = result.getSourceAsObjectList(ElkLoggingCwmp.class);
            if (elkLoggingCwmps.size() > 0) {
                elkLoggingCwmp = elkLoggingCwmps.get(0);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return elkLoggingCwmp;
    }

    public int countExistedTask(String deviceId, Long policyJobId, String time) {
        try {
            BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
            boolQueryBuilder.must(QueryBuilders.queryStringQuery(
                    String.format("\"%s\" AND \"POLICY_JOB_%s\" AND \"%s\"", ElkLoggingCwmp.START_TASK, policyJobId, deviceId)
            ).field("message"));
            boolQueryBuilder.mustNot(QueryBuilders.queryStringQuery("configurationPolicy").field("message"));
            boolQueryBuilder.mustNot(QueryBuilders.queryStringQuery("updateDiagnosticResult").field("message"));
            boolQueryBuilder.must(QueryBuilders.rangeQuery("@timestamp").lt(parseIsoDate(time)));

            // Call elk to get data
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            searchSourceBuilder.query(boolQueryBuilder);

            Search search = new Search.Builder(searchSourceBuilder.toString())
                    .addIndex(ElkLoggingCwmp.INDEX_LOGGING_CWMP)
                    .addType(ElkLoggingCwmp.TYPE_LOGGING_CWMP)
                    .build();
            SearchResult searchResult = elasticSearchClient.execute(search);
            return searchResult.getTotal();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public Boolean existedTaskWithParams(String deviceId, Long policyJobId) {
        Boolean isExisted = false;

        try {
            BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
            boolQueryBuilder.must(QueryBuilders.queryStringQuery(
                    String.format("\"%s\" AND \"POLICY_JOB_%s\" AND \"%s\"", ElkLoggingCwmp.START_TASK, policyJobId, deviceId)
            ).field("message"));
            boolQueryBuilder.mustNot(QueryBuilders.queryStringQuery("configurationPolicy").field("message"));
            boolQueryBuilder.mustNot(QueryBuilders.queryStringQuery("updateDiagnosticResult").field("message"));
            // Call elk to get data
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            searchSourceBuilder.query(boolQueryBuilder);

            Search search = new Search.Builder(searchSourceBuilder.toString())
                    .addIndex(ElkLoggingCwmp.INDEX_LOGGING_CWMP)
                    .addType(ElkLoggingCwmp.TYPE_LOGGING_CWMP)
                    .build();
            SearchResult result = elasticSearchClient.execute(search);
            List<ElkLoggingCwmp> elkLoggingCwmps = result.getSourceAsObjectList(ElkLoggingCwmp.class);
            if (elkLoggingCwmps.size() > 0) {
                isExisted = true;
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return isExisted;
    }

    public Integer countStartTaskWithParams(String deviceId, Long policyJobId) {
        Integer result = null;

        try {
            BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
            boolQueryBuilder.must(QueryBuilders.queryStringQuery(
                    String.format("\"%s\" AND \"POLICY_JOB_%s\" AND \"%s\"", ElkLoggingCwmp.START_TASK, policyJobId, deviceId)
            ).field("message"));
            boolQueryBuilder.mustNot(QueryBuilders.queryStringQuery("configurationPolicy").field("message"));
            boolQueryBuilder.mustNot(QueryBuilders.queryStringQuery("updateDiagnosticResult").field("message"));
            // Call elk to get data
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            searchSourceBuilder.query(boolQueryBuilder);

            Search search = new Search.Builder(searchSourceBuilder.toString())
                    .addIndex(ElkLoggingCwmp.INDEX_LOGGING_CWMP)
                    .addType(ElkLoggingCwmp.TYPE_LOGGING_CWMP)
                    .build();
            SearchResult searchResult = elasticSearchClient.execute(search);
            result = searchResult.getTotal();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }

    public Boolean removeAllElk() {
        Boolean result = false;
        try {
            URL url = new URL(elasticSearchUrl + "/" + ElkLoggingCwmp.INDEX_LOGGING_CWMP);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("DELETE");
            conn.setRequestProperty("Accept", "application/json");

            if (conn.getResponseCode() != 200) {
                throw new RuntimeException("Failed : HTTP error code : "
                        + conn.getResponseCode());
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(
                    (conn.getInputStream())));
            while (br.readLine() != null) {
                result = true;
            }
            conn.disconnect();
        } catch (IOException e) {
            logger.error("removeAllLog", e);
        }

        return result;
    }

    public Map<String, Long> getSummary(Long policyJobId) {

        Map<String, Long> result = new LinkedHashMap<>();

        Long totalElements = getTotalElement(policyJobId);
        Long totalCompleted = getTotalCompleted(policyJobId);
        Long totalError = getTotalFault(policyJobId);
        Long totalInprocess = totalElements - totalCompleted - totalError;

        result.put("totalElements", totalElements);
        result.put("totalInprocess", totalInprocess);
        result.put("totalCompleted", totalCompleted);
        result.put("totalError", totalError);

        return result;
    }

    public Map<String, Long> getSummaryDeviceActivity(String deviceId, String fromDateTime, String toDateTime) {

        // Get time expire to set from date time
        String timeExpire = configurationService.get("deviceActivityTimeSetting").value;
        if (!("").equals(fromDateTime) && timeExpire != null) {
            if (parseIsoDate(fromDateTime).compareTo(parseIsoDate(convertTimeExpire(timeExpire))) < 0) {
                fromDateTime = convertTimeExpire(timeExpire);
            }
        } else {
            fromDateTime = convertTimeExpire(timeExpire);
        }

        Map<String, Long> result = new LinkedHashMap<>();
        Long totalElements = getTotalElementDeviceActivity(deviceId, fromDateTime, toDateTime);
        result.put("totalElements", totalElements);

        return result;
    }

    public Map<String, Long> getSummaryDeviceActivity(String deviceId, String fromDateTime, String toDateTime, String taskName, String parameter, List<String> listTaskIds) {

        // Get time expire to set from date time
        String timeExpire = configurationService.get("deviceActivityTimeSetting").value;
        if (!("").equals(fromDateTime) && timeExpire != null) {
            if (parseIsoDate(fromDateTime).compareTo(parseIsoDate(convertTimeExpire(timeExpire))) < 0) {
                fromDateTime = convertTimeExpire(timeExpire);
            }
        } else {
            fromDateTime = convertTimeExpire(timeExpire);
        }

        Map<String, Long> result = new LinkedHashMap<>();
        Long totalElements = getTotalElementDeviceActivity(deviceId, fromDateTime, toDateTime, taskName, parameter, listTaskIds);
        result.put("totalElements", totalElements);

        return result;

    }

    private Long getTotalElementDeviceActivity(String deviceId, String fromDateTime, String toDateTime, String taskName, String parameter, List<String> listTaskIds) {
        Long result = null;
        try {
            // Create query
            BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
            boolQueryBuilder.must(QueryBuilders.queryStringQuery(
                    String.format("\"%s\" AND \"%s\"", ElkLoggingCwmp.START_TASK, deviceId)
            ).field("message"));
            boolQueryBuilder.mustNot(QueryBuilders.queryStringQuery("configurationPolicy").field("message"));
            boolQueryBuilder.mustNot(QueryBuilders.queryStringQuery("updateDiagnosticResult").field("message"));

            if (taskName != null) {
                boolQueryBuilder.must(QueryBuilders.queryStringQuery(taskName).field("message"));
            }
            if (parameter != null) {
//                boolQueryBuilder.must(QueryBuilders.queryStringQuery(parameter).field("message"));
                boolQueryBuilder.must(QueryBuilders.matchPhrasePrefixQuery("message", parameter));
                
            }
            if (listTaskIds != null) {
                if (listTaskIds.size() == 0) {
                    return 0L;
                } else {
                    List<String> listTaskQueries = new ArrayList<>();
                    for (String taskId : listTaskIds) {
                        listTaskQueries.add("\"TASK_ID_" + taskId + "\"");
                    }
                    boolQueryBuilder.must(QueryBuilders.queryStringQuery(String.join(" OR ", listTaskQueries)).field("message"));
                }
            }

            // Timestamp
            if (!("").equals(fromDateTime) && !("").equals(toDateTime)) {
                boolQueryBuilder.must(QueryBuilders.rangeQuery("@timestamp")
                        .gte(parseIsoDate(fromDateTime)).lt(parseIsoDate(toDateTime)));
            }
            if (!("").equals(fromDateTime) && ("").equals(toDateTime)) {
                boolQueryBuilder.must(QueryBuilders.rangeQuery("@timestamp")
                        .gte(parseIsoDate(fromDateTime)));
            }
            if (("").equals(fromDateTime) && !("").equals(toDateTime)) {
                boolQueryBuilder.must(QueryBuilders.rangeQuery("@timestamp")
                        .lt(parseIsoDate(toDateTime)));
            }

            // Call elk to get data
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            searchSourceBuilder.query(boolQueryBuilder);

            Search search = new Search.Builder(searchSourceBuilder.toString())
                    .addIndex(ElkLoggingCwmp.INDEX_LOGGING_CWMP)
                    .addType(ElkLoggingCwmp.TYPE_LOGGING_CWMP)
                    .build();
            SearchResult resultElk = elasticSearchClient.execute(search);
            result = Long.valueOf(resultElk.getTotal());

        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }

    private Long getTotalElementDeviceActivity(String deviceId, String fromDateTime, String toDateTime) {
        return getTotalElementDeviceActivity(deviceId, fromDateTime, toDateTime, null, null, null);
    }

    private Long getTotalByType(Long policyJobId, String taskType) {
        Long result = null;
        try {
            // Create query
            BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
            boolQueryBuilder.must(QueryBuilders.queryStringQuery(
                    String.format("\"%s\" AND \"POLICY_JOB_%s\"", taskType, policyJobId)
            ).field("message"));

            boolQueryBuilder.mustNot(QueryBuilders.queryStringQuery("configurationPolicy").field("message"));
            boolQueryBuilder.mustNot(QueryBuilders.queryStringQuery("updateDiagnosticResult").field("message"));

            // Call elk to get data
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            searchSourceBuilder.query(boolQueryBuilder);

            Search search = new Search.Builder(searchSourceBuilder.toString())
                    .addIndex(ElkLoggingCwmp.INDEX_LOGGING_CWMP)
                    .addType(ElkLoggingCwmp.TYPE_LOGGING_CWMP)
                    .build();
            SearchResult resultElk = elasticSearchClient.execute(search);
            result = Long.valueOf(resultElk.getTotal());

        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }

    public Long getTotalElement(Long policyJobId) {
        return getTotalByType(policyJobId, "START_TASK");
    }

    private Long getTotalCompleted(Long policyJobId) {
        return getTotalByType(policyJobId, "COMPLETED_TASK");
    }

    private Long getTotalFault(Long policyJobId) {
        return getTotalByType(policyJobId, "FAULT_TASK");
    }

    private String parseIsoDate(String date) {
        return StringUtils.convertDateToElk(date, ElkLoggingCwmp.FORMAT_DATETIME_TO_VIEW, ElkLoggingCwmp.FORMAT_TIMESTAMP_STORAGE);
    }

    public Boolean removeById(String id) {
        Boolean result = true;
        try {
            elasticSearchClient.execute(new Delete.Builder(id)
                    .index(ElkLoggingCwmp.INDEX_LOGGING_CWMP)
                    .type(ElkLoggingCwmp.TYPE_LOGGING_CWMP)
                    .build());

        } catch (Exception e) {
            result = false;
            e.printStackTrace();
        }

        return result;
    }

    public String convertTimeExpire(String timeExpire) {
        int days = 0;
        String time = "";
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        String[] timeArr = timeExpire.split("\\s+");
        for (String t : timeArr) {
            if (t.endsWith("d")) {
                days += Integer.valueOf(t.substring(0, t.length() - 1));
            }
            if (t.endsWith("w")) {
                days += Integer.valueOf(t.substring(0, t.length() - 1)) * 7;
            }
            if (t.endsWith("m")) {
                days += Integer.valueOf(t.substring(0, t.length() - 1)) * 30;
            }
            if (t.endsWith("y")) {
                days += Integer.valueOf(t.substring(0, t.length() - 1)) * 365;
            }
        }
        if (days >= 0) {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DATE, -days);
            Date date = new Date(cal.getTimeInMillis());
            time = sdf.format(date);
        }
        return time;
    }
}
