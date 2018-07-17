package vn.ssdc.vnpt.policy.services;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.internal.LinkedTreeMap;
import org.apache.commons.lang3.StringUtils;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import vn.ssdc.vnpt.AcsClient;
import vn.ssdc.vnpt.devices.model.*;
import vn.ssdc.vnpt.devices.services.*;
import vn.ssdc.vnpt.file.model.BackupFile;
import vn.ssdc.vnpt.logging.services.LoggingPolicyService;
import vn.ssdc.vnpt.policy.model.*;
import vn.ssdc.vnpt.selfCare.model.SCAddObject;
import vn.ssdc.vnpt.selfCare.model.SCObjectParameter;
import vn.ssdc.vnpt.selfCare.model.SCSetParameter;
import vn.ssdc.vnpt.selfCare.model.SCTask;
import vn.ssdc.vnpt.selfCare.services.SelfCareServiceDevice;
import vn.ssdc.vnpt.subscriber.model.Subscriber;
import vn.ssdc.vnpt.subscriber.services.SubscriberDeviceService;
import vn.vnpt.ssdc.core.SsdcCrudService;
import vn.vnpt.ssdc.jdbc.factories.RepositoryFactory;
import vn.vnpt.ssdc.utils.ObjectUtils;

import java.sql.Timestamp;
import java.text.ParseException;
import java.util.*;

import static org.quartz.SimpleScheduleBuilder.simpleSchedule;

/**
 * Created by Admin on 3/13/2017.
 */
@Service
public class PolicyJobService extends SsdcCrudService<Long, PolicyJob> {

    private static final Logger logger = LoggerFactory.getLogger(PolicyJobService.class);

    @Autowired
    AcsClient acsClient;

    @Autowired
    PolicyTaskService policyTaskService;

    @Autowired
    DeviceGroupService deviceGroupService;

    @Autowired
    public DeviceTypeService deviceTypeService;

    @Autowired
    public DeviceTypeVersionService deviceTypeVersionService;

    @Autowired
    public TagService tagService;

    @Autowired
    private Tr069ParameterService tr069ParameterService;

    @Autowired
    private Scheduler scheduler;

    @Autowired
    private LoggingPolicyService loggingPolicyService;

    @Autowired
    public SubscriberDeviceService subscriberDeviceService;

    @Autowired
    public ParameterDetailService parameterDetailService;

    @Autowired
    public PolicyJobService(RepositoryFactory repositoryFactory) {
        this.repository = repositoryFactory.create(PolicyJob.class);
    }

    //Create a new Quartz Job for Policy
    public void createQuartzJob(Long strStartDate, Long policyJobId, Integer intTimeInterval) throws ParseException, SchedulerException {
        Date dStartDate = new Date(strStartDate);
        JobDetail job = JobBuilder.newJob(PolicyQuartzJob.class).withIdentity("Job_" + policyJobId).build();
        job.getJobDataMap().put("policyJobId", policyJobId);
        Trigger trigger = null;
        if (intTimeInterval != null) {
            trigger = TriggerBuilder.newTrigger().withIdentity("Trigger_" + policyJobId)
                    .startAt(dStartDate)
                    .withSchedule(simpleSchedule().withIntervalInSeconds(60 * intTimeInterval).repeatForever())
                    .build();
        }
        else{
            trigger = TriggerBuilder.newTrigger().withIdentity("Trigger_" + policyJobId)
                    .startAt(dStartDate)
                    .build();
        }
        scheduler.scheduleJob(job, trigger);
    }

    //Create a new Quartz Job for Policy
    public void createQuartzJobWithEndTime(Long strStartDate, Long strEndDate, Long policyJobId, Integer intTimeInterval) throws ParseException, SchedulerException {
        Date dStartDate = new Date(strStartDate);
        Date dEndDate = new Date(strEndDate);
        JobDetail job = JobBuilder.newJob(PolicyQuartzJob.class).withIdentity("Job_" + policyJobId).build();
        job.getJobDataMap().put("policyJobId", policyJobId);
        Trigger trigger = null;
        if (intTimeInterval != null) {
            trigger = TriggerBuilder.newTrigger().withIdentity("Trigger_" + policyJobId)
                    .startAt(dStartDate)
                    .endAt(dEndDate)
                    .withSchedule(simpleSchedule().withIntervalInSeconds(60 * intTimeInterval).repeatForever())
                    .build();
        }
        else{
            trigger = TriggerBuilder.newTrigger().withIdentity("Trigger_" + policyJobId)
                    .startAt(dStartDate)
                    .endAt(dEndDate)
                    .build();
        }
        scheduler.scheduleJob(job, trigger);
    }

    //Create a new Delete Quartz Job for Policy
    public void createDeleteQuartzJob(Long strEndDate, Long policyJobId) throws ParseException, SchedulerException {
        Date dEndDate = new Date(strEndDate);
        JobDetail job = JobBuilder.newJob(DeletePolicyQuartzJob.class).withIdentity("Delete_Job_" + policyJobId).build();
        job.getJobDataMap().put("policyJobId", policyJobId);
        Trigger trigger = TriggerBuilder.newTrigger().withIdentity("Trigger_Delete_" + policyJobId)
                .startAt(dEndDate)
                .build();

        scheduler.scheduleJob(job, trigger);
    }

    //Unscheduling a Particular Trigger of Job
    public void deleteTriger(Long policyJobsId) throws SchedulerException {
        String strTrigger = "Trigger_".concat(Long.toString(policyJobsId));
        TriggerKey triggerKey = new TriggerKey(strTrigger);
        scheduler.unscheduleJob(triggerKey);

        String strTriggerDelete = "Trigger_Delete_".concat(Long.toString(policyJobsId));
        TriggerKey triggerKeyDelete = new TriggerKey(strTriggerDelete);
        scheduler.unscheduleJob(triggerKeyDelete);
    }

    //Deleting a Job and Unscheduling All of Its Triggers
    public void deleteQuartzJob(Long policyJobsId) throws SchedulerException {
        String strJob = "Job_".concat(Long.toString(policyJobsId));
        JobKey jobKey = new JobKey(strJob);
        scheduler.deleteJob(jobKey);

        String strDeleteJob = "Delete_Job_".concat(Long.toString(policyJobsId));
        JobKey jobDeleteKey = new JobKey(strDeleteJob);
        scheduler.deleteJob(jobDeleteKey);
    }

    public void processParametersPolicy(String deviceId, PolicyJob policyJob) {
        Map<String, String> acsQuery = new HashMap<String, String>();
        acsQuery.put("query", "{\"_id\":\"" + deviceId + "\"}");
        JsonArray arrayTmpObject = new Gson().fromJson(acsClient.search("devices", acsQuery).getBody(), JsonArray.class);
        if (arrayTmpObject.size() > 0) {
            JsonObject body = arrayTmpObject.get(0).getAsJsonObject();
            JsonObject inforObject = body.get("_deviceId").getAsJsonObject();
            String productClass = inforObject.get("_ProductClass") != null ? inforObject.get("_ProductClass").getAsString() : "";
            String oui = inforObject.get("_OUI").getAsString() != null ? inforObject.get("_OUI").getAsString() : "";
            String firmwareVersion = body.get("summary.softwareVersion") != null ? body.get("summary.softwareVersion").getAsJsonObject().get("_value").getAsString() : "";
            DeviceType currenDeviceType = deviceTypeService.findByPk(oui, productClass);
            if (currenDeviceType != null) {
                DeviceTypeVersion currentDeviceTypeVersion = deviceTypeVersionService.findByPk(currenDeviceType.id, firmwareVersion);
                if (currentDeviceTypeVersion != null) {
                    Map<String, Object> parameterValues = new HashMap<String, Object>();
                    Map<String, Map<String, String>> mapAddObject = new HashMap<String, Map<String, String>>();
                    List<String> listPath = new ArrayList<String>(policyJob.parameters.keySet());
                    String parameters = StringUtils.join(listPath, ",");
                    ResponseEntity<String> responseEntity = acsClient.getDevice(deviceId, parameters);
                    String responseEntityBody = responseEntity.getBody();
                    List<Device> devices = Device.fromJsonString(responseEntityBody, policyJob.parameters.keySet());
                    if (devices.size() > 0) {
                        Map<String, String> listParametersOfDevice = devices.get(0).parameters;
                        //List of change parameters
                        for (Map.Entry<String, Object> entry : policyJob.parameters.entrySet()) {
                            String path = entry.getKey();
                            String valueOfDevice = listParametersOfDevice.get(path);
                            LinkedTreeMap parameter = LinkedTreeMap.class.cast(entry.getValue());
                            if (parameter.get("value") != null) {
                                if (valueOfDevice != null) {
                                    parameterValues.put(path, parameter.get("value"));
                                } else {
                                    String tr069Path = tr069ParameterService.convertToTr069Param(path);
                                    if (tr069Path.lastIndexOf("{i}") > 0) {
                                        int lastIndex = tr069Path.lastIndexOf("{i}") + 4;
                                        String shortName = tr069Path.substring(lastIndex);
                                        String tmpObjectName = path.substring(0, path.length() - shortName.length() - 1);
                                        if (tmpObjectName.lastIndexOf(".") > 0) {
                                            String objectName = tmpObjectName.substring(0, tmpObjectName.lastIndexOf("."));
                                            Map<String, String> mapParameterValues = new HashMap<>();
                                            if (mapAddObject.containsKey(objectName)) {
                                                mapParameterValues = mapAddObject.get(objectName);
                                            }
                                            if (!mapParameterValues.containsKey(shortName)) {
                                                mapParameterValues.put(shortName, parameter.get("value").toString());
                                            }
                                            mapAddObject.put(objectName, mapParameterValues);
                                        }
                                    }
                                }
                            }
                        }
                    }
                    for (Map.Entry<String, Map<String, String>> entry : mapAddObject.entrySet()) {
                        acsClient.addObject(deviceId, entry.getKey(), entry.getValue(), true, policyJob.id);
                    }
                    if (parameterValues.size() > 0) {
                        acsClient.setParameterValues(deviceId, parameterValues, true, policyJob.id);
                    }
                }
            }
        }
    }

    public boolean checkParameterBelongDevice(String deviceId, String parameter) {
        DeviceTypeVersion deviceTypeVersion = deviceTypeVersionService.findbyDevice(deviceId);
        if (deviceTypeVersion != null) {
            ParameterDetail parameterDetail = parameterDetailService.getByTr069Name(tr069ParameterService.convertToTr069Param(parameter), deviceTypeVersion.id);
            // if param ton tai trong tap param cua thiet bi
            if (parameterDetail != null) {
                return true;
            }
            // neu khong thi thoi 1
        }
        return false;
    }

    public void processConfigurationPolicy(String deviceId, PolicyJob policyJob) {
        List<LinkedTreeMap> listSetParameters = (List<LinkedTreeMap>) policyJob.parameters.get("listSetParameters");
        Map<String, Object> parameterValues = new HashMap<>();
        for (LinkedTreeMap scSetParameter : listSetParameters) {
            // check if device has this parameter

            if (checkParameterBelongDevice(deviceId, scSetParameter.get("path").toString())) {
                if (scSetParameter.get("isSubscriberInformation") != null && scSetParameter.get("isSubscriberInformation").toString().equals("true")) {
                    if (getProvisioningValue(deviceId, scSetParameter.get("value").toString()) != null) {
                        parameterValues.put(scSetParameter.get("path").toString(), getProvisioningValue(deviceId, scSetParameter.get("value").toString()));
                    }
                } else {
                    parameterValues.put(scSetParameter.get("path").toString(), scSetParameter.get("value"));
                }
            }

        }
        if (parameterValues.size() > 0) {
            acsClient.setParameterValues(deviceId, parameterValues, true, policyJob.id);
        }
        List<LinkedTreeMap> listAddObjects = (List<LinkedTreeMap>) policyJob.parameters.get("listAddObjects");
        for (LinkedTreeMap scAddObject : listAddObjects) {
            Map<String, String> objectParameterValues = new HashMap<>();
            for (LinkedTreeMap scObjectParameter : (List<LinkedTreeMap>) scAddObject.get("listObjectParameters")) {
                String fullParameter = scAddObject.get("objectName").toString() + scObjectParameter.get("shortName").toString();
                if (checkParameterBelongDevice(deviceId, fullParameter)) {
                    if (scObjectParameter.get("isSubscriberInformation") != null && scObjectParameter.get("isSubscriberInformation").toString().equals("true")) {
                        if (getProvisioningValue(deviceId, scObjectParameter.get("value").toString()) != null) {
                            objectParameterValues.put(scObjectParameter.get("shortName").toString(), getProvisioningValue(deviceId, scObjectParameter.get("value").toString()));
                        }
                    } else {
                        objectParameterValues.put(scObjectParameter.get("shortName").toString(), scObjectParameter.get("value").toString());
                    }
                }
            }
            String objectName = scAddObject.get("objectName").toString().substring(0, scAddObject.get("objectName").toString().length() - 1);
            objectName = objectName.substring(0, objectName.lastIndexOf("."));
            acsClient.addObject(deviceId, objectName, objectParameterValues, true, policyJob.id);
        }
    }

    public void createTask(String deviceId, PolicyJob policyJob) {
        if ("parameters".equals(policyJob.actionName)) {
            processParametersPolicy(deviceId, policyJob);
        } else if ("backup".equals(policyJob.actionName)) {
            acsClient.uploadFile(deviceId, "1 Vendor Configuration File", true, policyJob.id);
        } else if ("restore".equals(policyJob.actionName)) {
            BackupFile backupFile = acsClient.searchBackupFile(deviceId);
            if (backupFile != null) {
                acsClient.createDownloadUrlFileTask(deviceId, "3 Vendor Configuration File", backupFile.url, "", "", "", "", "", 0, "", 0, true, "", "", true, policyJob.id);
            }
        } else if ("reboot".equals(policyJob.actionName)) {
            acsClient.reboot(deviceId, true, policyJob.id);
        } else if ("factoryReset".equals(policyJob.actionName)) {
            acsClient.factoryReset(deviceId, true, policyJob.id);
        } else if ("updateFirmware".equals(policyJob.actionName) || "downloadVendorConfigurationFile".equals(policyJob.actionName)) {
            String strFileId = policyJob.parameters.get("fileId").toString();
            acsClient.downloadFile(deviceId, strFileId, "", true, policyJob.id);
        } else if ("configuration".equals(policyJob.actionName)) {
            processConfigurationPolicy(deviceId, policyJob);
        }
        else if ("configXMPP".equals(policyJob.actionName)) {
            acsClient.configXMPP(deviceId, true, policyJob.id);
        }
        else if ("enableTR069".equals(policyJob.actionName)) {
            acsClient.enableTR069(deviceId, true, policyJob.id);
        }
    }

    public String getProvisioningValue(String deviceId, String subscriberDataKey) {
        List<Subscriber> subscribers = subscriberDeviceService.findByDeviceId(deviceId);
        if (subscribers.size() > 0) {
            Subscriber subscriber = subscribers.get(0);
            if (subscriber.subscriberData.get(subscriberDataKey) != null) {
                return subscriber.subscriberData.get(subscriberDataKey);
            }
        }
        return null;
    }

    public PolicyPreset createPolicyPreset(PolicyJob policyJob) {
        PolicyPreset policyPreset = new PolicyPreset();
        if (policyJob.deviceGroupId == null) {
            List<String> listDeviceIds = new ArrayList<String>();

            // Get list device id valid with limited
            if (policyJob.limited != null) {
                for (String deviceId : policyJob.externalDevices) {
                    if (loggingPolicyService.countStartTaskWithParams(deviceId, policyJob.id) >= policyJob.limited) {
                        listDeviceIds.add(String.format("{\"_id\":\"%s\"}", deviceId));
                    }
                }

            } else {
                for (String deviceId : policyJob.externalDevices) {
                    listDeviceIds.add(String.format("{\"_id\":\"%s\"}", deviceId));
                }
            }
            policyPreset.precondition = String.format("{\"$or\":[%s]}", StringUtils.join(listDeviceIds, ","));
        } else {
            DeviceGroup deviceGroup = deviceGroupService.get(policyJob.deviceGroupId);

            // Get list device ids valid with limited
            List<String> listDeviceIds = new ArrayList<String>();
            if (policyJob.limited != null) {
                List<String> listDevice = deviceGroupService.getListDeviceByGroup(policyJob.deviceGroupId);
                for (int intIndex = 0; intIndex < listDevice.size(); intIndex++) {
                    String deviceId = listDevice.get(intIndex);
                    if (loggingPolicyService.countStartTaskWithParams(deviceId, policyJob.id) >= policyJob.limited) {
                        listDeviceIds.add(String.format("{\"_id\":{\"$ne\":\"%s\"}}", deviceId));
                    }
                }
            }

            if (listDeviceIds.size() > 0) {
                policyPreset.precondition = String.format("{\"$and\":[%s,%s]}", String.join(",", listDeviceIds), deviceGroup.query);
            } else {
                policyPreset.precondition = deviceGroup.query;
            }
        }
        Map<String, Boolean> events = new HashMap<String, Boolean>();
        for (String event : policyJob.events) {
            events.put(event, true);
        }
        policyPreset.events = events;
        policyPreset.weight = policyJob.priority;
        if ("parameters".equals(policyJob.actionName) || "configuration".equals(policyJob.actionName)) {
            List<PolicyConfiguration> configurations = new ArrayList<PolicyConfiguration>();
            PolicyConfiguration policyConfiguration = new PolicyConfiguration();
            policyConfiguration.policyJobId = policyJob.id;
            policyConfiguration.type = "configuration";
            configurations.add(policyConfiguration);
            policyPreset.configurations = configurations;
        } else if ("reboot".equals(policyJob.actionName)) {
            List<PolicyConfiguration> configurations = new ArrayList<PolicyConfiguration>();
            PolicyConfiguration policyConfiguration = new PolicyConfiguration();
            policyConfiguration.policyJobId = policyJob.id;
            policyConfiguration.type = "reboot";
            configurations.add(policyConfiguration);
            policyPreset.configurations = configurations;
        } else if ("factoryReset".equals(policyJob.actionName)) {
            List<PolicyConfiguration> configurations = new ArrayList<PolicyConfiguration>();
            PolicyConfiguration policyConfiguration = new PolicyConfiguration();
            policyConfiguration.policyJobId = policyJob.id;
            policyConfiguration.type = "reboot";
            configurations.add(policyConfiguration);
            policyPreset.configurations = configurations;
        } else if ("updateFirmware".equals(policyJob.actionName) || "downloadVendorConfigurationFile".equals(policyJob.actionName)) {
            List<PolicyConfiguration> configurations = new ArrayList<PolicyConfiguration>();
            PolicyConfiguration policyConfiguration = new PolicyConfiguration();
            policyConfiguration.policyJobId = policyJob.id;
            policyConfiguration.type = "download";
            policyConfiguration.fileId = policyJob.parameters.get("fileId").toString();
            configurations.add(policyConfiguration);
            policyPreset.configurations = configurations;
        } else if ("restore".equals(policyJob.actionName)) {
            List<PolicyConfiguration> configurations = new ArrayList<PolicyConfiguration>();
            PolicyConfiguration policyConfiguration = new PolicyConfiguration();
            policyConfiguration.policyJobId = policyJob.id;
            policyConfiguration.type = "restore";
            configurations.add(policyConfiguration);
            policyPreset.configurations = configurations;
        } else if ("backup".equals(policyJob.actionName)) {
            List<PolicyConfiguration> configurations = new ArrayList<PolicyConfiguration>();
            PolicyConfiguration policyConfiguration = new PolicyConfiguration();
            policyConfiguration.policyJobId = policyJob.id;
            policyConfiguration.type = "backup";
            //policyConfiguration.fileType = policyJob.parameters.get("fileType").toString();
            policyConfiguration.fileType = "1 Vendor Configuration File";
            policyConfiguration.url = String.format("%s/backup-files/%s", acsClient.getBackupFileUploadUrl(), policyConfiguration.fileType.substring(0, 1));
            configurations.add(policyConfiguration);
            policyPreset.configurations = configurations;
        }
        else if ("configXMPP".equals(policyJob.actionName)) {
            List<PolicyConfiguration> configurations = new ArrayList<PolicyConfiguration>();
            PolicyConfiguration policyConfiguration = new PolicyConfiguration();
            policyConfiguration.policyJobId = policyJob.id;
            policyConfiguration.type = "configXMPP";
            configurations.add(policyConfiguration);
            policyPreset.configurations = configurations;
        }
        else if ("enableTR069".equals(policyJob.actionName)) {
            List<PolicyConfiguration> configurations = new ArrayList<PolicyConfiguration>();
            PolicyConfiguration policyConfiguration = new PolicyConfiguration();
            policyConfiguration.policyJobId = policyJob.id;
            policyConfiguration.type = "enableTR069";
            configurations.add(policyConfiguration);
            policyPreset.configurations = configurations;
        }

        if (policyJob.presetId != null) {
            policyPreset._id = "Policy Job : " + policyJob.id;
        }
        return policyPreset;
    }

    public void createUpdatePreset(Long policyJobId) {
        logger.info("Run update policy preset " + policyJobId);
        PolicyJob policyJob = this.repository.findOne(policyJobId);
        PolicyPreset policyPreset = createPolicyPreset(policyJob);
        acsClient.createPolicyPreset(policyPreset, "Policy Job " + policyJobId);
        if (policyJob.presetId == null) {
            policyJob.presetId = "Policy Job : " + policyJobId;
        }
        policyJob.currentNumber = 0;
        update(policyJobId, policyJob);
    }

    public void deletePreset(Long policyJobId) {
        acsClient.deletePolicyPreset("Policy Job " + policyJobId);
    }

    @Override
    public void beforeCreate(PolicyJob policyJob) {
        policyJob.status = "INIT";
    }

    @Override
    public void beforeUpdate(Long policyId, PolicyJob policyJob) {
        if (policyJob.scheduleTime == null && policyJob.status.equals("EXECUTE")) {
            Timestamp timestamp = new Timestamp(new Date().getTime());
            policyJob.scheduleTime = timestamp.getTime();
        }
    }

    @Override
    public void afterDelete(PolicyJob policyJob) {
        try {
            //1st.Delete Job
            deleteQuartzJob(policyJob.id);
            //2st.Delete Trigger
            deleteTriger(policyJob.id);
            //3st.Delete Preset
            deletePreset(policyJob.id);
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error(ex.toString());
        }
    }

    public Boolean findJobExecute(String device_group_id, String status) {
        List<PolicyJob> policyJobsList = null;
        Boolean exitJobs = false;
        String whereExp = "device_group_id=? and status=?";
        if (("").equals(status)) {
            whereExp = "device_group_id=?";
            policyJobsList = this.repository.search(whereExp, Long.parseLong(device_group_id));
        } else {
            policyJobsList = this.repository.search(whereExp, Long.parseLong(device_group_id), status);
        }
        if (policyJobsList.size() > 0) {
            exitJobs = true;
        }
        return exitJobs;
    }

    public Page<PolicyJob> getPage(int page, int limit) {
        return this.repository.findAll(new PageRequest(page, limit));
    }

    public Page<PolicyJob> getPageWithNumberOfExecution(int page, int limit) {
        Page<PolicyJob> policyJobPage = this.repository.findAll(new PageRequest(page, limit));

        for (PolicyJob policyJob : policyJobPage.getContent()) {
            policyJob.setNumberOfExecutions(loggingPolicyService.getTotalElement(policyJob.id));

            if (!ObjectUtils.empty(policyJob.deviceGroupId)) {
                policyJob.setDeviceGroupName(deviceGroupService.get(policyJob.deviceGroupId).name);
            }

            if (policyJob.isImmediately) {
                try {
                    List<PolicyTask> policyTasks = policyTaskService.getListPolicyTaskByPolicyId(1, 1, policyJob.id);
                    if (policyTasks.size() > 0) {
                        policyJob.setTimeCompleted(null);
                    } else {
                        PolicyTask policyTaskLast = policyTaskService.getLastCompleteTask(policyJob.id);
                        policyJob.setTimeCompleted(policyTaskLast.completed);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }

            } else {
                if (policyJob.status.equals("STOP")) {
                    policyJob.setTimeCompleted(policyJob.updated);
                }
            }
        }

        return policyJobPage;
    }

    public PolicyJob getWithNumberOfExecution(Long id) {
        PolicyJob policyJob = get(id);
        policyJob.setNumberOfExecutions(loggingPolicyService.getTotalElement(policyJob.id));

        return policyJob;
    }

    public void execute(Long policyJobId) {
        try {
            //1st update status job
            PolicyJob policyJob = get(policyJobId);
            //2st
            if (policyJob.isImmediately) {
                //1st get all list device
                if (policyJob.deviceGroupId == null) {
                    for (String deviceId : policyJob.externalDevices) {
                        if (!loggingPolicyService.existedTaskWithParams(deviceId, policyJobId)) {
                            createTask(deviceId, policyJob);
                        }
                    }
                } else {
                    List<String> listDevice = deviceGroupService.getListDeviceByGroup(policyJob.deviceGroupId);
                    for (int intIndex = 0; intIndex < listDevice.size(); intIndex++) {
                        String deviceId = listDevice.get(intIndex);
                        if (!loggingPolicyService.existedTaskWithParams(deviceId, policyJobId)) {
                            createTask(deviceId, policyJob);
                        }
                    }
                }
            } else {
                if (policyJob.ended == null) {
                    createQuartzJob(policyJob.startAt, policyJobId, policyJob.timeInterval);
                } else {
                    createQuartzJobWithEndTime(policyJob.startAt, policyJob.ended, policyJobId, policyJob.timeInterval);
                    createDeleteQuartzJob(policyJob.ended, policyJobId);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error(ex.toString());
        }
    }

    public String stop(Long policyJobId) {
        try {
            //1st.Delete Job
            deleteQuartzJob(policyJobId);
            //2st.Delete Trigger
            deleteTriger(policyJobId);
            //3st.Delete Preset
            deletePreset(policyJobId);
            //4st.Delete Task
            deleteTask(policyJobId);
            return "DELETE POLICY JOB SUCCESS";
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error(ex.toString());
            return "DELETE POLICY JOB ERROR";
        }
    }

    public void deleteTask(Long policyJobId) {
        Map<String, String> queryParams = new HashMap<String, String>();
        queryParams.put("query", "{\"$or\": [{\"policyJobId\":\"" + policyJobId + "\"},{\"policyJobIds\":{\"$elemMatch\":{\"$eq\":\"" + policyJobId + "\"}}}]}");
        ResponseEntity<String> responseEntity = this.acsClient.search("tasks", queryParams);
        if (!Strings.isNullOrEmpty(responseEntity.getBody())) {
            JsonArray arrTasks = new Gson().fromJson(responseEntity.getBody(), JsonArray.class);
            for (int i = 0; i < arrTasks.size(); i++) {
                JsonObject taskObjectFromArray = arrTasks.get(i).getAsJsonObject();
                String taskId = taskObjectFromArray.get("_id") == null ? "" : taskObjectFromArray.get("_id").getAsString();
                acsClient.deleteTask(taskId);
            }
        }
    }

    public List<PolicyJob> findByPage(String limit, String indexPage, String whereExp) {
        List<PolicyJob> listPolicyJobs = new ArrayList<PolicyJob>();
        if (!whereExp.isEmpty()) {
            listPolicyJobs = this.repository.search(whereExp, new PageRequest(Integer.parseInt(indexPage), Integer.parseInt(limit))).getContent();
        } else {
            listPolicyJobs = this.repository.findAll(new PageRequest(Integer.parseInt(indexPage), Integer.parseInt(limit))).getContent();
        }

        return listPolicyJobs;
    }

    public List<PolicyJob> findByQuery(String whereExp) {
        if (!Strings.isNullOrEmpty(whereExp)) {
            return this.repository.search(whereExp);
        }
        return this.repository.findAll();
    }

    public long countByQuery(String whereExp) {
        if (!Strings.isNullOrEmpty(whereExp)) {
            return this.repository.count(whereExp);
        }
        return this.repository.count();
    }
}
