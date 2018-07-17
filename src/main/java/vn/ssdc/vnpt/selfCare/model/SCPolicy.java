package vn.ssdc.vnpt.selfCare.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.beans.factory.annotation.Autowired;
import vn.ssdc.vnpt.devices.model.Parameter;
import vn.ssdc.vnpt.policy.model.PolicyJob;
import vn.ssdc.vnpt.policy.model.PolicyTask;
import vn.ssdc.vnpt.policy.services.PolicyTaskService;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Created by THANHLX on 12/8/2017.
 */
public class SCPolicy {
    public Long id;
    public String name;
    @ApiModelProperty(example = "INIT | EXECUTE | STOP")
    public String status;
    public Long deviceGroupId;
    public String deviceGroupName;
    public Boolean isImmediately;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    public Date startedTime;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    public Date endedTime;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    public Date firstExecutedTime;
    public Integer timeInterval;
    public Integer maxInterval;
    public List<String> events;
    public Integer limited;
    @ApiModelProperty(example = "low | medium | high | urgent")
    public String priority;
    @ApiModelProperty(example = "configuration | reboot | factoryReset | updateFirmware | downloadVendorConfigurationFile | restore | backup")
    public String operation;
    public List<SCSetParameter> listSetParameters;
    public List<SCAddObject> listAddObjects;
    public String downloadFileId;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    public Date completedTime;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    public Date createdTime;
    public Integer numberOfCompleted;
    public Integer numberOfError;
    public Integer numberOfQueue;
    public Integer numberOfDevices;
    public Integer numberOfExecute;
}
