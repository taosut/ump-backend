package vn.ssdc.vnpt.selfCare.model.searchForm;

import io.swagger.annotations.ApiModelProperty;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Created by THANHLX on 12/8/2017.
 */
public class SCPolicySearchForm {
    public Integer limit;
    public Integer page;
    public Long id;
    public String name;
    public String status;
    public Long deviceGroupId;
    public Boolean isImmediately;
    public Date fromStartedTime;
    public Date toStartedTime;
    public Date fromEndedTime;
    public Date toEndedTime;
    public Date fromFirstExcutedTime;
    public Date toFirstExcutedTime;
    public Integer timeInterval;
    public Integer maxInterval;
    public List<String> events;
    public Integer limited;
    public Integer priority;
    @ApiModelProperty(example = "configuration | reboot | factoryReset | updateFirmware | downloadVendorConfigurationFile | restore | backup")
    public String operation;
    public Map<String, Object> parameters;
    private Date fromCompletedTime;
    private Date toCompletedTime;
    public Date fromCreatedTime;
    public Date toCreatedTime;
    public String userName;
}
