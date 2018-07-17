/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vn.ssdc.vnpt.selfCare.model;

import java.util.HashMap;
import java.util.Map;
import vn.ssdc.vnpt.devices.model.DeviceTypeVersion;
import vn.ssdc.vnpt.devices.model.Parameter;
import vn.ssdc.vnpt.devices.model.Tag;

/**
 *
 * @author Admin
 */
public class SCDataModel {
    
    public Long dataModelId; // == deviceTypevVersionID
    public Long deviceTypeId;
    public String firmwareVersion;
    public Map<String, Parameter> parameters;
    public Map<String, SCProfileConfig> diagnostics;
    public String modelName;
    public String manufacturer;
    public String oui;
    public String productClass;

    public SCDataModel(DeviceTypeVersion deviceTypeVersion) {
        this.dataModelId = deviceTypeVersion.id;
        this.deviceTypeId = deviceTypeVersion.deviceTypeId;
        this.firmwareVersion = deviceTypeVersion.firmwareVersion;
        this.parameters = deviceTypeVersion.parameters;
        Map<String, SCProfileConfig> diagnostics = new HashMap<>();
        for (Map.Entry<String, Tag> entry : deviceTypeVersion.diagnostics.entrySet()) {
            diagnostics.put(entry.getKey(), new SCProfileConfig(entry.getValue()));
        }
        this.diagnostics = diagnostics;
        this.modelName = deviceTypeVersion.modelName;
        this.oui = deviceTypeVersion.oui;
        this.productClass = deviceTypeVersion.productClass;
        this.manufacturer = deviceTypeVersion.manufacturer;
    }

}
