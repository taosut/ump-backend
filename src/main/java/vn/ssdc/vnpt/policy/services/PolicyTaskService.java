package vn.ssdc.vnpt.policy.services;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.searchbox.client.JestClient;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import io.searchbox.core.search.sort.Sort;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import vn.ssdc.vnpt.policy.model.PolicyTask;
import vn.vnpt.ssdc.core.SsdcCrudService;
import vn.vnpt.ssdc.jdbc.factories.RepositoryFactory;

import java.util.List;
import java.util.Map;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import vn.ssdc.vnpt.AcsClient;
import vn.ssdc.vnpt.logging.endpoints.LoggingDeviceEndpoint;
import vn.ssdc.vnpt.logging.model.ElkLoggingCwmp;
import vn.ssdc.vnpt.selfCare.model.searchForm.SCPolicySearchForm;

/**
 * Created by Admin on 3/13/2017.
 */
@Service
public class PolicyTaskService extends SsdcCrudService<Long, PolicyTask> {

    @Autowired
    public PolicyTaskService(RepositoryFactory repositoryFactory) {
        this.repository = repositoryFactory.create(PolicyTask.class);
    }
    private static final Logger logger = LoggerFactory.getLogger(PolicyTaskService.class);

    @Autowired
    JestClient elasticSearchClient;

    @Autowired
    private AcsClient acsClient;

    public PolicyTask findByTaskId(String taskId) {
        String whereExp = "task_id = ?";
        List<PolicyTask> policyTasks = this.repository.search(whereExp, taskId);
        if (policyTasks.isEmpty()) {
            return null;
        } else {
            return policyTasks.get(0);
        }
    }

    public long count(String query) {
        return this.repository.count(query);

    }

    public List<PolicyTask> groupByStatus() {
        return this.repository.searchWithGroupBy("status");
    }

    public List<PolicyTask> getListPolicyTaskByPolicyId(int page, int limit, Long policyId) throws ParseException {
        Map<String, Object> mapParam = new HashMap<String, Object>();
        if (policyId != null) {
            mapParam.put("policyJobId", policyId);
        }
        String obj = new Gson().toJson(mapParam);
        Map<String, String> mCondition = new HashMap<>();
        mCondition.put("query", obj);
        mCondition.put("limit", String.valueOf(limit));
        mCondition.put("skip", String.valueOf((page - 1) * limit));
        ResponseEntity<String> responseEntity = acsClient.search("tasks", mCondition);
        return parseInfor(responseEntity.getBody());
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

    public List<PolicyTask> parseInfor(String body) throws ParseException {
        List<PolicyTask> listTask = new ArrayList<>();
        JsonArray arr = new Gson().fromJson(body, JsonArray.class);
        for (int i = 0; i < arr.size(); i++) {
            JsonObject obj = arr.get(i).getAsJsonObject();
            PolicyTask task = new PolicyTask();
            task.deviceId = obj.get("device") == null ? "" : obj.get("device").getAsString();
            task.policyJobId = obj.get("policyJobId") == null ? 0 : obj.get("policyJobId").getAsLong();
            task.taskId = obj.get("taskId") == null ? "" : obj.get("_id").getAsString();
            if (obj.get("fault").getAsJsonObject() != null) {
                task.status = 2;
            }
            SimpleDateFormat dt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            task.completed = dt.parse(obj.get("timestamp").getAsString()).getTime();
            listTask.add(task);
        }
        return listTask;
    }
}
