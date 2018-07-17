package vn.ssdc.vnpt.policy.model;

import vn.vnpt.ssdc.jdbc.SsdcEntity;

import java.util.List;
import java.util.Map;

/**
 * Created by Admin on 3/13/2017.
 */
public class PolicyJob extends SsdcEntity<Long> {
    public String name;
    public String status;
    //INIT, EXECUTE, STOP
    public Long deviceGroupId;
    public List<String> externalDevices;
    public String externalFilename;
    public Long startAt;
    public Long scheduleTime;
    public Integer timeInterval;
    public Integer maxNumber;
    public Integer currentNumber;
    public List<String> events;
    public Boolean isImmediately;
    public String actionName;
    public Map<String, Object> parameters;
    public String presetId;
    public Integer limited;
    public Integer priority;
    public Long ended;

    private Long numberOfExecutions;
    private String deviceGroupName;

    private Long timeCompleted;

    public Long getTimeCompleted() {
        return this.timeCompleted;
    }

    public void setTimeCompleted(Long timeCompleted) {
        this.timeCompleted = timeCompleted;
    }

    public Long getNumberOfExecutions() {
        return numberOfExecutions;
    }

    public void setNumberOfExecutions(Long numberOfExecutions) {
        this.numberOfExecutions = numberOfExecutions;
    }

    public String getDeviceGroupName() {
        return deviceGroupName;
    }

    public void setDeviceGroupName(String deviceGroupName) {
        this.deviceGroupName = deviceGroupName;
    }
}
