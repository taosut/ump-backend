package vn.ssdc.vnpt.selfCare.model.searchForm;

import io.swagger.annotations.ApiModelProperty;
import java.util.Date;

/**
 * Created by THANHLX on 11/29/2017.
 */
public class SCFileSearchForm {

    public Integer limit;
    public Integer page;
    public String oui;
    public String productClass;
    @ApiModelProperty(example = "1 Firmware Upgrade Image, 3 Vendor Configuration File")
    public String fileType;
    public String manufacturer;
    public String modelName;
    public String fileName;
    public String firmwareVersion;
    public Date createdFrom;
    public Date createdTo;
    public Boolean isBasicFirmware;
}
