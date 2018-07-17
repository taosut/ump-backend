package vn.ssdc.vnpt.selfCare.model.searchForm;

import io.swagger.annotations.ApiModelProperty;

import java.util.Date;

/**
 * Created by THANHLX on 11/22/2017.
 */
public class SCAlarmQosSeachForm {
    public Date raisedFrom;
    public Date raisedTo;
    public Integer limit;
    public Integer page;
    public String device_id;
    public String severity;
    public Long qosKpiId;
}
