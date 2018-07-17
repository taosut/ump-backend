/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vn.ssdc.vnpt.selfCare.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.LinkedHashSet;
import java.util.Set;
import vn.ssdc.vnpt.devices.model.Parameter;
import vn.ssdc.vnpt.devices.model.Tag;

/**
 *
 * @author kiendt
 */
public class SCProfileConfig {

    public Long id;
    public String name;
    public Map<String, Parameter> parameters;
    public Long deviceTypeVersionId;
    //0 : unassigned, 1 : assigned, use integer for cross-platform db
    public Integer assigned;
    public String assignedGroup;
    public Set<String> correspondingModule;

    public String subProfileSetting;
    public String profileSetting;

    public SCProfileConfig(Tag tag) {
        this.id = tag.id;
        this.name = tag.name;
        this.parameters = tag.parameters != null ? tag.parameters : new HashMap<>();
        this.deviceTypeVersionId = tag.deviceTypeVersionId;
        this.assigned = tag.assigned;
        this.assignedGroup = tag.assignedGroup;
        this.correspondingModule = tag.correspondingModule != null ? tag.correspondingModule : new HashSet<>();
        this.subProfileSetting = tag.subProfileSetting;
        this.profileSetting = tag.profileSetting;
    }

    public SCProfileConfig() {
    }
}
