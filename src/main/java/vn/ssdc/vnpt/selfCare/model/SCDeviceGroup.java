package vn.ssdc.vnpt.selfCare.model;

import com.google.common.base.Strings;
import io.swagger.annotations.ApiModelProperty;
import java.util.HashSet;
import java.util.Set;
import vn.ssdc.vnpt.devices.model.DeviceGroup;

/**
 * Created by THANHLX on 11/30/2017.
 */
public class SCDeviceGroup {

    public Long id;
    public String name;

    public SCDeviceGroup(DeviceGroup deviceGroup) {
        this.id = deviceGroup.id;
        this.name = deviceGroup.name;
    }

    public SCDeviceGroup() {
    }

    public String query;
    public String manufacturer;
    public String modelName;
    public String firmwareVersion;
    @ApiModelProperty(example = "[\"Ha Noi\", \"Ho Chi Minh\"]")
    public Set<String> labels;
    public String oui;
    public String productClass;
    @ApiModelProperty(example = "[1,2,3,4]")
    public Set<Long> labelIds;
    
    public Set<String> devices;
}
