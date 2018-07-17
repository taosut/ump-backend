package vn.ssdc.vnpt.selfCare.model;

import java.util.Date;
import java.util.Set;

/**
 * Created by THANHLX on 11/27/2017.
 */
public class SCDevice {

    public String id;
    public String serialNumber;
    public String manufacturer;
    public String oui;
    public String productClass;
    public String firmwareVersion;
    public String modelName;
    public String ip;
    public String connectionRequest;
    public String status;
    public Date lastBoot;
    public Date lastBootstrap;
    public Date lastInform;
    public Date lastSynchronize;
    public Date registered;
    public Set<String> labels;
    public boolean isLastFirmwareVersion;
    public String mac;
    public String account;

    public Set<Long> labelIds;

}
