package vn.ssdc.vnpt.mapping.model;

import vn.vnpt.ssdc.jdbc.SsdcEntity;

public class IpMapping extends SsdcEntity<Long> {

    public String ipMappings;
    public String label;
    public String labelId;
    public String startIp;
    public String endIp;
}
