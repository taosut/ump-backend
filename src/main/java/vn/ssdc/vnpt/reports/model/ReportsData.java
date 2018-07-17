package vn.ssdc.vnpt.reports.model;

import vn.vnpt.ssdc.jdbc.SsdcEntity;

public class ReportsData extends SsdcEntity<Long> {
    public String label;
    public String model;
    public String firmware_version;
    public Integer count_by_firmware;
    public Integer count_by_label;
    public Integer count_by_label_model;
    public Long specific_id;
    public Integer count_online;
    //
    public String serial_number;
    public String ip_address;
    public String manufacturer;
}
