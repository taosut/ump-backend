package vn.ssdc.vnpt.selfCare.model;

import java.util.Date;

/**
 * Created by THANHLX on 11/27/2017.
 */
public class SCTask {
    public String taskId;
    public int httpStatus;
    
    public String name;
    public Date date;
    public String deviceId;
    public String fault;
    public Integer retries;
}
