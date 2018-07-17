package vn.ssdc.vnpt.selfCare.model.searchForm;

import io.swagger.annotations.ApiModelProperty;

import java.util.Date;
import java.util.Map;

/**
 * Created by THANHLX on 11/22/2017.
 */
public class SCAlarmSeachForm {

    public Integer limit;
    public Integer page;
    public String device_id;
    public String severity;
    @ApiModelProperty(example = "Request failed | Configuration device failed | Update firmware failed | Reboot failed | Factory reset failed | Alarm threshold")
    public String alarmTypeName;
    public String alarmName;
    public String status;
    public Date raisedFrom;
    public Date raisedTo;
    @ApiModelProperty(example = "1,2,3,4")
    public String deviceGroupIds;
    public String userName;
}
