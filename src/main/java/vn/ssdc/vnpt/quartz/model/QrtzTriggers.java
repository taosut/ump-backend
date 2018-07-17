package vn.ssdc.vnpt.quartz.model;

import vn.vnpt.ssdc.jdbc.SsdcEntity;

/**
 * Created by Admin on 11/9/2017.
 */
public class QrtzTriggers extends SsdcEntity<String> {
    public String triggerName;
    public String triggerGroup;
    public String jobName;
    public String jobGroup;
    public String nextFireTime;
    public String prevFireTime;
    public String triggerState;
    public String startTime;
    public String endTime;
}
