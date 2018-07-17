package vn.ssdc.vnpt.selfCare.model;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.util.Date;

/**
 * Created by THANHLX on 12/25/2017.
 */
public class SCPolicyLog {
    public String deviceId;
    public String manufacturer;
    public String modelName;
    public String firmwareVersion;
    public String serialNumber;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    public Date completedTime;
    public String errorCode;
    public String errorText;
}
