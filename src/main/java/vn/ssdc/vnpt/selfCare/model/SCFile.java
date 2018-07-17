package vn.ssdc.vnpt.selfCare.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.web.multipart.MultipartFile;

import java.util.Date;

/**
 * Created by THANHLX on 11/24/2017.
 */
public class SCFile {

    public String id;
    public String fileId;
    public String oui;
    public String productClass;
    public String fileName;
    public String firmwareVersion;
    public String manufacturer;
    public String modelName;
    public String fileType;
    public String fileUrl;
    public String username;
    public String password;
    public int length;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    public Date createdTime;
    public String url;
    public String md5;
    public boolean isBasicFirmware;
    public String size;
}
