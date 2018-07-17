package vn.ssdc.vnpt.selfCare.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import vn.ssdc.vnpt.selfCare.model.SCBackupFile;
import vn.ssdc.vnpt.selfCare.model.SCFile;
import vn.ssdc.vnpt.logging.model.ElkLoggingCwmp;
import vn.ssdc.vnpt.logging.model.LoggingDeviceActivity;
import vn.ssdc.vnpt.logging.services.LoggingPolicyService;
import vn.ssdc.vnpt.selfCare.model.SCDeviceActivity;
import vn.ssdc.vnpt.selfCare.model.searchForm.SCDeviceActivitySearchForm;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by THANHLX on 12/20/2017.
 */
@Service
public class SelfCareServiceDeviceActivity {
    @Autowired
    private LoggingPolicyService loggingPolicyService;

    @Autowired
    private SelfCareServiceFile selfCareServiceFile;

    @Autowired
    private SelfCareServiceBackupFile selfCareServiceBackupFile;

    public List<SCDeviceActivity> search(SCDeviceActivitySearchForm searchParameter)  throws IOException, ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat(ElkLoggingCwmp.FORMAT_DATETIME_TO_VIEW);
        String fromDateTime = "";
        if (searchParameter.createdFrom != null) {
            fromDateTime = sdf.format(searchParameter.createdFrom);
        }
        String toDateTime = "";
        if (searchParameter.createdTo != null) {
            toDateTime = sdf.format(searchParameter.createdTo);
        }

        List<LoggingDeviceActivity> listLoggingDeviceActivities = loggingPolicyService.getPageDeviceActivity(searchParameter.page, searchParameter.limit, searchParameter.deviceId, fromDateTime,toDateTime,searchParameter.taskName,searchParameter.parameter,getListTaskIds(searchParameter));
        List<SCDeviceActivity> listSCDeviceActivities = new ArrayList<>();
        for (LoggingDeviceActivity loggingDeviceActivity : listLoggingDeviceActivities) {
            listSCDeviceActivities.add(new SCDeviceActivity(convertDataView(loggingDeviceActivity)));
        }
        return listSCDeviceActivities;
    }

    public long count(SCDeviceActivitySearchForm searchParameter) throws IOException, ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat(ElkLoggingCwmp.FORMAT_DATETIME_TO_VIEW);
        String fromDateTime = "";
        if (searchParameter.createdFrom != null) {
            fromDateTime = sdf.format(searchParameter.createdFrom);
        }
        String toDateTime = "";
        if (searchParameter.createdTo != null) {
            toDateTime = sdf.format(searchParameter.createdTo);
        }
        Map<String, Long> tmp = loggingPolicyService.getSummaryDeviceActivity(searchParameter.deviceId, fromDateTime, toDateTime,searchParameter.taskName,searchParameter.parameter,getListTaskIds(searchParameter));
        return tmp.get("totalElements").intValue();
    }

    public List<String> getListTaskIds(SCDeviceActivitySearchForm searchParameter) throws IOException{
        List<String> listTaskIds = null;
        List<String> listCompletedTaskIds = null;
        List<String> listFaultTaskIds = null;

        if(searchParameter.completedFrom != null || searchParameter.completedTo != null){
            SimpleDateFormat sdf = new SimpleDateFormat(ElkLoggingCwmp.FORMAT_DATETIME_TO_VIEW);
            String fromDateTime = "";
            if (searchParameter.completedFrom != null) {
                fromDateTime = sdf.format(searchParameter.completedFrom);
            }
            String toDateTime = "";
            if (searchParameter.completedTo != null) {
                toDateTime = sdf.format(searchParameter.completedTo);
            }
            listCompletedTaskIds = loggingPolicyService.getCompletedTaskIds(searchParameter.deviceId, fromDateTime, toDateTime);
        }
        if(searchParameter.errorCode != null || searchParameter.errorText != null){
            listFaultTaskIds = loggingPolicyService.getFaultTaskIds(searchParameter.deviceId, searchParameter.errorCode, searchParameter.errorText);
        }
        if(listCompletedTaskIds != null && listFaultTaskIds != null){
            for(String taskId : listCompletedTaskIds){
                if(listFaultTaskIds.contains(taskId)){
                    listTaskIds.add(taskId);
                }
            }
        }
        else if(listCompletedTaskIds != null){
            listTaskIds = listCompletedTaskIds;
        }
        else if(listFaultTaskIds != null){
            listTaskIds = listFaultTaskIds;
        }
        return listTaskIds;
    }

    public LoggingDeviceActivity convertDataView(LoggingDeviceActivity loggingDeviceActivity) {
        String taskNameView = "";
        String parameterView = "";
        switch (loggingDeviceActivity.taskName) {
            case "reboot":
                taskNameView = "Reboot";
                parameterView = "Reboot";
                break;
            case "factoryReset":
                taskNameView = "Factory Reset";
                parameterView = "Factory Reset";
                break;
            case "addObject":
                taskNameView = "Add Object";
                parameterView = loggingDeviceActivity.parameter;
                break;
            case "deleteObject":
                taskNameView = "Delete Object";
                parameterView = loggingDeviceActivity.parameter;
                break;
            case "refreshObject":
                taskNameView = "Refresh Object";
                parameterView = "Refresh Object";
                break;
            case "recheckStatus":
                taskNameView = "Recheck Status";
                parameterView = "Recheck Status";
                break;
            case "getParameterValues":
                // Template value: ["InternetGatewayDevice.ManagementServer.ConnectionRequestURL","InternetGatewayDevice.ManagementServer.URL","InternetGatewayDevice.ManagementServer.Username","InternetGatewayDevice.ManagementServer.Password"]
                taskNameView = "Refresh parameter";
                if (loggingDeviceActivity.parameter != null) {
                    parameterView = loggingDeviceActivity.parameter.replaceAll("[\\]\\[\"]", "");
                    parameterView = parameterView.replaceAll("[,]", ", ");
                }
                break;
            case "setParameterValues":
                // Template value: [["InternetGatewayDevice.ManagementServer.ConnectionRequestPassword","root","xsd:string"],["InternetGatewayDevice.ManagementServer.Username","root","xsd:string"],["InternetGatewayDevice.ManagementServer.ConnectionRequestUsername","root","xsd:string"],["InternetGatewayDevice.ManagementServer.Password","root","xsd:string"]]
                taskNameView = "Update Parameter";
                if (loggingDeviceActivity.parameter != null) {
                    try {
                        Set<String> paths = new LinkedHashSet<>();
                        for (String subParameter : loggingDeviceActivity.parameter.split("],")) {
                            int startStr = subParameter.indexOf("\"") + 1;
                            int endStr = subParameter.indexOf("\"", startStr);
                            String currentPath = subParameter.substring(startStr, endStr).trim();
                            paths.add(currentPath);
                        }
                        parameterView = String.join(", ", paths);
                    } catch (Exception e) {
                        parameterView = "Undefined";
                        e.printStackTrace();
                    }
                }
                break;
            case "createDiagnostic":
                // Template value: [["InternetGatewayDevice.ManagementServer.ConnectionRequestPassword","root","xsd:string"],["InternetGatewayDevice.ManagementServer.Username","root","xsd:string"],["InternetGatewayDevice.ManagementServer.ConnectionRequestUsername","root","xsd:string"],["InternetGatewayDevice.ManagementServer.Password","root","xsd:string"]]
                taskNameView = "Diagnostic";
                if (loggingDeviceActivity.parameter != null) {
                    try {
                        Set<String> paths = new LinkedHashSet<>();
                        for (String subParameter : loggingDeviceActivity.parameter.split("],")) {
                            int startStr = subParameter.indexOf("\"") + 1;
                            int endStr = subParameter.indexOf("\"", startStr);
                            String currentPath = subParameter.substring(startStr, endStr).trim();
                            paths.add(currentPath);
                        }
                        parameterView = String.join(", ", paths);
                    } catch (Exception e) {
                        parameterView = "Undefined";
                        e.printStackTrace();
                    }
                }
                break;
            case "download":
                // Template value: ["url","http://wiki.vnpt-technology.vn","1 Firmware Upgrade Image"]
                // Template value: ["file","59538b1ec8b6495a2bb3a1e6"]
                taskNameView = "Download";
                if (loggingDeviceActivity.parameter != null) {
                    try {
                        if (loggingDeviceActivity.parameter.contains("\"url\"")) {
                            String[] paths = loggingDeviceActivity.parameter.split("\",");
                            parameterView = paths[1].substring(
                                    paths[1].indexOf("\"") + 1,
                                    paths[1].length()).trim();
                            parameterView = parameterView.substring(parameterView.lastIndexOf("/") + 1, parameterView.length()).trim();

                            taskNameView = paths[2].substring(
                                    paths[2].indexOf("\"") + 1,
                                    paths[2].lastIndexOf("\"")).trim();
                            taskNameView = taskNameView.replace(taskNameView.substring(0, taskNameView.indexOf(" ")), "Download");
                            if(taskNameView.equals("Download Vendor Configuration File")){
                                taskNameView = "Restore";
                            }
                            else if(taskNameView.equals("Download Firmware Upgrade Image")){
                                taskNameView = "Update Firmware";
                            }

                        } else if (loggingDeviceActivity.parameter.contains("\"file\"")) {
                            String[] paths = loggingDeviceActivity.parameter.split("\",");
                            String fileId = paths[1].substring(
                                    paths[1].indexOf("\"") + 1,
                                    paths[1].lastIndexOf("\"")).trim();
                            SCFile scFile = selfCareServiceFile.get(fileId);
                            if (scFile != null) {
                                parameterView = scFile.fileName;
                                taskNameView = scFile.fileType;
                                taskNameView = taskNameView.replace(taskNameView.substring(0, taskNameView.indexOf(" ")), "Download");

                                if(taskNameView.equals("Download Vendor Configuration File")){
                                    taskNameView = "Restore";
                                }
                                else if(taskNameView.equals("Download Firmware Upgrade Image")){
                                    taskNameView = "Update Firmware";
                                }
                            } else {
                                SCBackupFile scBackupFile = selfCareServiceBackupFile.get(fileId);
                                if (scBackupFile != null) {
                                    parameterView = scBackupFile.filename;
                                    taskNameView = "Restore";
                                } else {
                                    parameterView = "Undefined";
                                }
                            }

                        }
                    } catch (Exception e) {
                        parameterView = "Undefined";
                        e.printStackTrace();
                    }
                }
                break;
            case "upload":
                taskNameView = "Upload";
                if (loggingDeviceActivity.parameter != null) {
                    String[] paths = loggingDeviceActivity.parameter.split("\",");
                    try {
                        parameterView = paths[0].substring(
                                paths[0].indexOf("\"") + 1,
                                paths[0].length()).trim();
                        parameterView = parameterView.substring(parameterView.lastIndexOf("/") + 1, parameterView.length()).trim();

                        taskNameView = paths[1].substring(
                                paths[1].indexOf("\"") + 1,
                                paths[1].lastIndexOf("\"")).trim();
                        taskNameView = taskNameView.replace(taskNameView.substring(0, taskNameView.indexOf(" ")), "Upload");
                        if(taskNameView.equals("Upload Vendor Configuration File")){
                            taskNameView = "Backup";
                        }
                    } catch (Exception e) {
                        parameterView = "Undefined";
                        e.printStackTrace();
                    }
                }
                break;
            default:
                taskNameView = loggingDeviceActivity.taskName;
                parameterView = loggingDeviceActivity.parameter;
                break;
        }
        loggingDeviceActivity.taskName = taskNameView;
        loggingDeviceActivity.parameter = parameterView;
        return loggingDeviceActivity;
    }
}
