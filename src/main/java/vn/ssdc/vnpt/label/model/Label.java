package vn.ssdc.vnpt.label.model;

import vn.vnpt.ssdc.jdbc.SsdcEntity;

public class Label extends SsdcEntity<Long> {

    public String name;
    public String description;
    public String parentId;
    public String parentName;
    public String ipMapping;
    
    public Long deviceGroupId;
}
