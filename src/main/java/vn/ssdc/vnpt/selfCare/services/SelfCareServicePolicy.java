/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vn.ssdc.vnpt.selfCare.services;

import com.google.common.base.Strings;
import org.apache.commons.collections.map.HashedMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import vn.ssdc.vnpt.AcsClient;
import vn.ssdc.vnpt.logging.services.LoggingPolicyService;
import vn.ssdc.vnpt.policy.model.PolicyJob;
import vn.ssdc.vnpt.policy.model.PolicyTask;
import vn.ssdc.vnpt.policy.services.PolicyJobService;
import vn.ssdc.vnpt.policy.services.PolicyTaskService;
import vn.ssdc.vnpt.selfCare.model.*;
import vn.ssdc.vnpt.selfCare.model.searchForm.SCPolicyLogSearchForm;
import vn.ssdc.vnpt.selfCare.model.searchForm.SCPolicySearchForm;

import java.util.*;
import vn.ssdc.vnpt.devices.services.DeviceGroupService;

/**
 *
 * @author Admin
 */
@Service
public class SelfCareServicePolicy {

    @Autowired
    private PolicyJobService policyJobService;

    @Autowired
    private PolicyTaskService policyTaskService;

    @Autowired
    private LoggingPolicyService loggingPolicyService;

    @Autowired
    private SelfCareServiceDevice selfCareServiceDevice;

    @Autowired
    private DeviceGroupService deviceGroupService;

    @Autowired
    private SelfCareServiceUser selfCareServiceUser;

    public SCPolicy create(SCPolicy scPolicy) {
        PolicyJob policyJob = new PolicyJob();
        return convertToSCPolicy(policyJobService.create(convertToPolicyJob(scPolicy, policyJob)), new SCPolicy());
    }

    public SCPolicy update(Long id, SCPolicy scPolicy) {
        PolicyJob policyJob = policyJobService.get(id);
        return convertToSCPolicy(policyJobService.update(id, convertToPolicyJob(scPolicy, policyJob)), new SCPolicy());
    }

    public PolicyJob convertToPolicyJob(SCPolicy scPolicy, PolicyJob policyJob) {
        policyJob.id = scPolicy.id;
        policyJob.name = scPolicy.name;
        policyJob.status = scPolicy.status;
        policyJob.deviceGroupId = scPolicy.deviceGroupId;
        policyJob.isImmediately = scPolicy.isImmediately;
        if (scPolicy.startedTime != null) {
            policyJob.startAt = scPolicy.startedTime.getTime();
        }
        if (scPolicy.endedTime != null) {
            policyJob.ended = scPolicy.endedTime.getTime();
        }

        if (scPolicy.firstExecutedTime != null) {
            policyJob.scheduleTime = scPolicy.firstExecutedTime.getTime();
        }
        policyJob.timeInterval = scPolicy.timeInterval;
        policyJob.maxNumber = scPolicy.maxInterval;
        policyJob.events = scPolicy.events;
        policyJob.limited = scPolicy.limited;
        if (scPolicy.priority != null) {
            switch (scPolicy.priority) {
                case "low":
                    policyJob.priority = 1;
                    break;
                case "medium":
                    policyJob.priority = 2;
                    break;
                case "high":
                    policyJob.priority = 3;
                    break;
                case "urgent":
                    policyJob.priority = 4;
                    break;
                default:
                    policyJob.priority = 1;
            }
        }
        policyJob.actionName = scPolicy.operation;
        if (scPolicy.operation != null) {
            switch (scPolicy.operation) {
                case "configuration":
                    if (policyJob.parameters == null) {
                        policyJob.parameters = new HashedMap();
                    }
                    policyJob.parameters.put("listSetParameters", scPolicy.listSetParameters);
                    policyJob.parameters.put("listAddObjects", scPolicy.listAddObjects);
                    break;
                case "updateFirmware":
                case "downloadVendorConfigurationFile":
                    if (policyJob.parameters == null) {
                        policyJob.parameters = new HashedMap();
                    }
                    policyJob.parameters.put("fileId", scPolicy.downloadFileId);
                    break;
                case "backup":
                case "restore":
                    if (policyJob.parameters == null) {
                        policyJob.parameters = new HashedMap();
                    }
                    policyJob.parameters.put("fileType", "Vendor configuration file");
                    break;
            }
        }
        return policyJob;
    }

    public List<SCPolicy> search(SCPolicySearchForm scPolicySearchForm) {
        List<PolicyJob> listPolicyJobs = new ArrayList<>();
        String whereExp = generateQuery(scPolicySearchForm);
        if (scPolicySearchForm.page == null || scPolicySearchForm.limit == null) {
            listPolicyJobs = policyJobService.findByQuery(whereExp);
        } else {
            listPolicyJobs = policyJobService.findByPage(String.valueOf(scPolicySearchForm.limit), String.valueOf(scPolicySearchForm.page - 1), whereExp);
        }
        List<SCPolicy> listSCPolicies = new ArrayList<>();
        for (PolicyJob policyJob : listPolicyJobs) {
            SCPolicy scPolicy = convertToSCPolicy(policyJob, new SCPolicy());
            scPolicy.deviceGroupName = deviceGroupService.get(scPolicy.deviceGroupId).name;
            listSCPolicies.add(scPolicy);
        }
        return listSCPolicies;
    }

    public int count(SCPolicySearchForm scPolicySearchForm) {
        List<PolicyJob> listPolicyJobs = new ArrayList<>();
        String whereExp = generateQuery(scPolicySearchForm);
        return (int) policyJobService.countByQuery(whereExp);
    }

    public String generateQuery(SCPolicySearchForm scPolicySearchForm) {
        Set<String> listQueries = new HashSet<>();
        if (scPolicySearchForm.name != null) {
            listQueries.add("name like '%" + scPolicySearchForm.name + "%'");
        }
        if (scPolicySearchForm.operation != null) {
            listQueries.add("action_name = '" + scPolicySearchForm.operation + "'");
        }
        if (scPolicySearchForm.fromStartedTime != null) {
            listQueries.add("start_at >= '" + scPolicySearchForm.fromStartedTime.getTime() + "'");
        }
        if (scPolicySearchForm.toStartedTime != null) {
            listQueries.add("start_at <= '" + scPolicySearchForm.toStartedTime.getTime() + "'");
        }
        if (scPolicySearchForm.fromCreatedTime != null) {
            listQueries.add("created >= '" + scPolicySearchForm.fromCreatedTime.getTime() + "'");
        }
        if (scPolicySearchForm.toCreatedTime != null) {
            listQueries.add("created <= '" + scPolicySearchForm.toCreatedTime.getTime() + "'");
        }
        if (scPolicySearchForm.isImmediately != null) {
            if (scPolicySearchForm.isImmediately) {
                listQueries.add("is_immediately = 1");
            } else {
                listQueries.add("is_immediately = 0");
            }
        }
        if (scPolicySearchForm.deviceGroupId != null) {
            listQueries.add("device_group_id = " + scPolicySearchForm.deviceGroupId);
        }

        if (!Strings.isNullOrEmpty(scPolicySearchForm.userName)) {
            String deviceGroupIds = selfCareServiceUser.getAllDeviceGroupIds(scPolicySearchForm.userName).toString();
            deviceGroupIds = deviceGroupIds.replaceAll("[\"\\[\\]]", "");
            listQueries.add("device_group_id IN (" + deviceGroupIds + ")");
        }

        return String.join(" AND ", listQueries);
    }

    public SCPolicy convertToSCPolicy(PolicyJob policyJob, SCPolicy scPolicy) {
        scPolicy.id = policyJob.id;
        scPolicy.name = policyJob.name;
        scPolicy.status = policyJob.status;
        scPolicy.deviceGroupId = policyJob.deviceGroupId;
        scPolicy.isImmediately = policyJob.isImmediately;
        if (policyJob.startAt != null) {
            scPolicy.startedTime = new Date(policyJob.startAt);
        }
        if (policyJob.ended != null) {
            scPolicy.endedTime = new Date(policyJob.ended);
        }
        if (policyJob.scheduleTime != null) {
            scPolicy.firstExecutedTime = new Date(policyJob.scheduleTime);
        }
        scPolicy.timeInterval = policyJob.timeInterval;
        scPolicy.maxInterval = policyJob.maxNumber;
        scPolicy.events = policyJob.events;
        scPolicy.limited = policyJob.limited;
        if (policyJob.priority != null) {
            switch (policyJob.priority) {
                case 1:
                    scPolicy.priority = "low";
                    break;
                case 2:
                    scPolicy.priority = "medium";
                    break;
                case 3:
                    scPolicy.priority = "high";
                    break;
                case 4:
                    scPolicy.priority = "urgent";
                    break;
                default:
                    scPolicy.priority = "low";
            }
        }
        scPolicy.operation = policyJob.actionName;
        if (policyJob.actionName != null) {
            if (policyJob.actionName.equals("configuration")) {
                scPolicy.listSetParameters = (List<SCSetParameter>) policyJob.parameters.get("listSetParameters");
                scPolicy.listAddObjects = (List<SCAddObject>) policyJob.parameters.get("listAddObjects");
            } else if (policyJob.actionName.equals("updateFirmware") || policyJob.actionName.equals("downloadVendorConfigurationFile")) {
                scPolicy.downloadFileId = (String) policyJob.parameters.get("fileId");
            }
        }
        if (policyJob.isImmediately != null && policyJob.isImmediately) {
            try {
                List<PolicyTask> policyTasks = policyTaskService.getListPolicyTaskByPolicyId(1, 1, policyJob.id);
                if (policyTasks.size() == 0) {
                    PolicyTask policyTaskLast = policyTaskService.getLastCompleteTask(policyJob.id);
                    if (policyTaskLast != null) {
                        scPolicy.completedTime = new Date(policyTaskLast.completed);
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

        } else {
            if (policyJob.status.equals("STOP")) {
                scPolicy.completedTime = new Date(policyJob.updated);
            }
        }
        Map<String, Long> summary = loggingPolicyService.getSummary(policyJob.id);
        if (summary.containsKey("totalCompleted")) {
            scPolicy.numberOfCompleted = summary.get("totalCompleted").intValue();
        }
        if (summary.containsKey("totalError")) {
            scPolicy.numberOfError = summary.get("totalError").intValue();
        }
        if (summary.containsKey("totalInprocess")) {
            scPolicy.numberOfQueue = summary.get("totalInprocess").intValue();
        }
        if (summary.containsKey("totalElements")) {
            scPolicy.numberOfDevices = summary.get("totalElements").intValue();
        }
        scPolicy.numberOfExecute = scPolicy.numberOfError + scPolicy.numberOfCompleted;
        if (policyJob.created != null) {
            scPolicy.createdTime = new Date(policyJob.created);
        }
        return scPolicy;
    }

    public List<SCPolicyLog> searchPolicyLog(Long policyId, SCPolicyLogSearchForm scPolicyLogSearchForm) {
        List<PolicyJob> listPolicyJobs = new ArrayList<>();
        String whereExp = generatePolicyLogQuery(scPolicyLogSearchForm);
        List<PolicyTask> listPolicyTasks = new ArrayList<>();
        if (scPolicyLogSearchForm.page == null && scPolicyLogSearchForm.limit == null) {
            listPolicyTasks = loggingPolicyService.getPage(0, 0, policyId);
        } else {
            listPolicyTasks = loggingPolicyService.getPage(scPolicyLogSearchForm.page, scPolicyLogSearchForm.limit, policyId);
        }
//        List<PolicyTask> listPolicyTasks = loggingPolicyService.getPage(scPolicyLogSearchForm.page, scPolicyLogSearchForm.limit, policyId);
        List<SCPolicyLog> listSCPolicyLogs = new ArrayList<>();
        for (PolicyTask policyTask : listPolicyTasks) {
            listSCPolicyLogs.add(convertToSCPolicyLog(policyTask));
        }
        return listSCPolicyLogs;
    }

    public SCPolicyLog convertToSCPolicyLog(PolicyTask policyTask) {
        SCPolicyLog scPolicyLog = new SCPolicyLog();
        scPolicyLog.deviceId = policyTask.deviceId;
        if (policyTask.deviceId != null) {
            try {
                SCDevice scDevice = selfCareServiceDevice.getDevice(policyTask.deviceId);
                scPolicyLog.manufacturer = scDevice.manufacturer;
                scPolicyLog.modelName = scDevice.modelName;
                scPolicyLog.firmwareVersion = scDevice.firmwareVersion;
                scPolicyLog.serialNumber = scDevice.serialNumber;
            } catch (Exception e) {
                return null;
            }
        }
        if (policyTask.completed != null) {
            scPolicyLog.completedTime = new Date(policyTask.completed);
        }
        scPolicyLog.errorCode = policyTask.errorCode;
        scPolicyLog.errorText = policyTask.errorText;
        return scPolicyLog;
    }

    public int countPolicyLog(Long policyId, SCPolicyLogSearchForm scPolicyLogSearchForm) {
        return loggingPolicyService.getTotalElement(policyId).intValue();
    }

    public String generatePolicyLogQuery(SCPolicyLogSearchForm scPolicyLogSearchForm) {
        return "";
    }

}
