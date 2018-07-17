package vn.ssdc.vnpt.selfCare.model.searchForm;

import java.util.Date;

/**
 * Created by THANHLX on 12/25/2017.
 */
public class SCPolicyLogSearchForm {
    public Integer limit;
    public Integer page;
    public String manufacturer;
    public String modelName;
    public String firmwareVersion;
    public String serialNumber;
    public Date fromCompletedTime;
    public Date toCompletedTime;
    public String errorCode;
    public String errorText;
}
