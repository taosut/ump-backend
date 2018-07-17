package vn.ssdc.vnpt.reports.model;

import vn.vnpt.ssdc.jdbc.SsdcEntity;
import java.util.Set;

public class DeviceTempBirt extends SsdcEntity<Long> {
    public String serialNumber;
    public String manufacturer;
    public String productClass;
    public String oui;
    public String firmwareVersion;
    public String modelName;
    public String label;
}
