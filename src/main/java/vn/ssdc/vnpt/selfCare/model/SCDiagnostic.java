package vn.ssdc.vnpt.selfCare.model;

import vn.ssdc.vnpt.devices.model.DiagnosticTask;
import vn.ssdc.vnpt.devices.model.Parameter;

import java.util.Map;

/**
 * Created by THANHLX on 12/1/2017.
 */
public class SCDiagnostic {
    public long id;
    public String deviceId;
    public String diagnosticsName;
    public Map<String, Parameter> parameterFull;
    public Map<String, String> request;
    public Map<String, String> result;
    public Integer status;
    public Long completed;
    public String taskId;

    public SCDiagnostic(DiagnosticTask diagnosticTask) {
        this.id = diagnosticTask.id;
        this.taskId = diagnosticTask.taskId;
        this.deviceId = diagnosticTask.deviceId;
        this.diagnosticsName = diagnosticTask.diagnosticsName;
        this.parameterFull = diagnosticTask.parameterFull;
        this.request = diagnosticTask.request;
        this.result = diagnosticTask.result;
        this.status = diagnosticTask.status;
        this.completed = diagnosticTask.completed;
    }
}
