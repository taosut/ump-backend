package vn.ssdc.vnpt.selfCare.model;

import vn.ssdc.vnpt.alarm.model.Alarm;
import vn.ssdc.vnpt.logging.model.ElkLoggingCwmp;
import vn.ssdc.vnpt.logging.model.LoggingDeviceActivity;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by THANHLX on 11/23/2017.
 */
public class SCDeviceActivity {
    public String taskId;
    public String taskName;
    public String parameter;
    public Date createdTime;
    public Date completedTime;
    public String errorCode;
    public String errorText;
    public SCDeviceActivity(LoggingDeviceActivity loggingDeviceActivity) throws java.text.ParseException{
        this.taskId = loggingDeviceActivity.taskId;
        this.taskName = loggingDeviceActivity.taskName;
        this.parameter = loggingDeviceActivity.parameter;
        this.errorCode = loggingDeviceActivity.errorCode;
        this.errorText = loggingDeviceActivity.errorText;
        DateFormat df = new SimpleDateFormat(ElkLoggingCwmp.FORMAT_DATETIME_TO_VIEW);
        this.createdTime = df.parse(loggingDeviceActivity.createdTime);
        if(loggingDeviceActivity.completedTime != null) {
            this.completedTime = df.parse(loggingDeviceActivity.completedTime);
        }
    }
}
