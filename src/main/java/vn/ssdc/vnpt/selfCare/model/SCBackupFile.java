package vn.ssdc.vnpt.selfCare.model;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.util.Date;
import java.util.Set;

/**
 * Created by THANHLX on 1/8/2018.
 */
public class SCBackupFile {
    public String id;
    public String oui;
    public String productClass;
    public String filename;
    public String firmwareVersion;
    public String serialNumber;
    public String manufacturer;
    public String modelName;
    public String fileType;
    public int length;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    public Date createdTime;
    public String url;
    public Set<String> labels;
    public String deviceId;
}
