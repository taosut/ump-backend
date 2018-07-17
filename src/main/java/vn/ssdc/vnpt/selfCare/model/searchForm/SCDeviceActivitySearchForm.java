package vn.ssdc.vnpt.selfCare.model.searchForm;

import java.util.Date;

/**
 * Created by THANHLX on 11/23/2017.
 */
public class SCDeviceActivitySearchForm {
    public int limit;
    public int page;
    public String deviceId;
    public String taskName;
    public String parameter;
    public Date createdFrom;
    public Date createdTo;
    public Date completedFrom;
    public Date completedTo;
    public String errorCode;
    public String errorText;
}
