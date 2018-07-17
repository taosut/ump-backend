package vn.ssdc.vnpt.selfCare.model.searchForm;

import java.util.Date;

/**
 * Created by THANHLX on 11/27/2017.
 */
public class SCDeviceSearchForm {

    public Integer limit;
    public Integer page;
    public String manufacturer;
    public String modelName;
    public String label;
    public String ipAddress;
    public String firmwareVersion;
    public String serialNumber;
    public Date registeredFrom;
    public Date registeredTo;
    public Date lastInformFrom;
    public Date lastInformTo;
    public Boolean status;
    public String userName;
    public String oui;
    public String productClass;
    public String deviceId;
    public String account;
    public Long deviceGroupId;

}
