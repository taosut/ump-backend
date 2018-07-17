/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vn.ssdc.vnpt.selfCare.services;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import vn.ssdc.vnpt.AcsClient;
import vn.ssdc.vnpt.common.services.ConfigurationService;
import vn.ssdc.vnpt.devices.model.DeviceGroup;
import vn.ssdc.vnpt.selfCare.model.SCTask;
import vn.ssdc.vnpt.selfCare.model.searchForm.SCTaskSearchForm;
import static vn.ssdc.vnpt.selfCare.services.SelfCareServiceDevice.CONNECTION_REQUEST_URL;
import static vn.ssdc.vnpt.selfCare.services.SelfCareServiceDevice.CREATED;
import static vn.ssdc.vnpt.selfCare.services.SelfCareServiceDevice.FIRMWARE_VERSION;
import static vn.ssdc.vnpt.selfCare.services.SelfCareServiceDevice.MODEL_NAME;
import static vn.ssdc.vnpt.selfCare.services.SelfCareServiceDevice.TAGS;

/**
 *
 * @author Admin
 */
@Service
public class SelfCareServiceTask {

    @Autowired
    private AcsClient acsClient;

    @Autowired
    private ConfigurationService configurationService;

    public static final String TASK_NAME = "name";
    public static final String DEVICE_ID = "device";
    public static final String TIMESTAMP = "timestamp";
    public static final String FAULT = "fault.detail.Fault.FaultString";
    public static final String RETRIES = "retries";
    SimpleDateFormat dt1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    public List<SCTask> search(SCTaskSearchForm sCTaskSearchForm) {
        ResponseEntity<String> responseEntity = acsClient.search("tasks", generateMapCondition(sCTaskSearchForm));
        return convertResponseToSCTask(responseEntity);
    }

    public long count(SCTaskSearchForm sCTaskSearchForm) {
        ResponseEntity<String> responseEntity = acsClient.search("tasks", generateMapCondition(sCTaskSearchForm));
        return Integer.valueOf(responseEntity.getHeaders().get("totalAll").get(0));
    }

    private Map<String, String> generateMapCondition(SCTaskSearchForm sCTaskSearchForm) {
        dt1.setTimeZone(TimeZone.getTimeZone("GMT+0"));
        Map<String, Object> mapParam = new HashMap<String, Object>();

        if(!Strings.isNullOrEmpty(sCTaskSearchForm.deviceId)) {
            mapParam.put("device",sCTaskSearchForm.deviceId);
        }

        if (!Strings.isNullOrEmpty(sCTaskSearchForm.taskName)) {
            mapParam.put(TASK_NAME, String.format("/%s/", sCTaskSearchForm.taskName));
        }

        if (!Strings.isNullOrEmpty(sCTaskSearchForm.fault)) {
            mapParam.put(FAULT, String.format("/%s/", sCTaskSearchForm.fault));
        }

        if (sCTaskSearchForm.retries != null) {
            mapParam.put(RETRIES, sCTaskSearchForm.retries);
        }

        if (sCTaskSearchForm.fromCreateDate != null) {
            JsonObject obj = new JsonObject();
            obj.addProperty("$gte", dt1.format(sCTaskSearchForm.fromCreateDate));
            mapParam.put(TIMESTAMP, obj);
        }

        if (sCTaskSearchForm.toCreateDate != null) {
            JsonObject obj = new JsonObject();
            obj.addProperty("$lte", dt1.format(sCTaskSearchForm.toCreateDate));
            mapParam.put(TIMESTAMP, obj);
        }

        Map<String, String> mCondition = new HashMap<>();
        if (!mapParam.isEmpty()) {
            String obj = new Gson().toJson(mapParam);
            mCondition.put("query", obj);
        }
//        if (sCTaskSearchForm.page == null) {
//            sCTaskSearchForm.page = Integer.valueOf(configurationService.get("page_default").value);
//        }

        if (sCTaskSearchForm.limit != null) {
            mCondition.put("limit", String.valueOf(sCTaskSearchForm.limit));
            mCondition.put("skip", String.valueOf((sCTaskSearchForm.page - 1) * sCTaskSearchForm.limit));
        }
        return mCondition;
    }

    public List<SCTask> convertResponseToSCTask(ResponseEntity<String> response) {
        List<SCTask> tasks = new ArrayList<>();
        if (!Strings.isNullOrEmpty(response.getBody())) {
            JsonArray arrTasks = new Gson().fromJson(response.getBody(), JsonArray.class);
            for (int i = 0; i < arrTasks.size(); i++) {
                JsonObject taskObjectFromArray = arrTasks.get(i).getAsJsonObject();
                SCTask taskEntity = new SCTask();
                taskEntity.deviceId = taskObjectFromArray.get("device") == null ? "" : taskObjectFromArray.get("device").getAsString();
                taskEntity.name = taskObjectFromArray.get("name") == null ? "" : taskObjectFromArray.get("name").getAsString();
                taskEntity.taskId = taskObjectFromArray.get("_id") == null ? "" : taskObjectFromArray.get("_id").getAsString();
                taskEntity.fault = taskObjectFromArray.get("fault") == null ? "" : taskObjectFromArray.get("fault").getAsJsonObject().toString();
                taskEntity.retries = taskObjectFromArray.get("retries") == null ? null : taskObjectFromArray.get("retries").getAsInt();
                try {
                    taskEntity.date = taskObjectFromArray.get("timestamp") == null ? null : SelfCareServiceDevice.parseDate(taskObjectFromArray.get("timestamp").getAsString());
                } catch (Exception e) {
                    taskEntity.date = null;
                }

                tasks.add(taskEntity);
            }
        }
        return tasks;
    }

}
