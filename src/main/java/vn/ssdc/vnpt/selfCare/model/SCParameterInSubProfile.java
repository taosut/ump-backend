/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vn.ssdc.vnpt.selfCare.model;

import java.util.Set;

/**
 *
 * @author Admin
 */
public class SCParameterInSubProfile {

    public String path;
    public String shortName;
    public String dataType;
    public String defaultValue;
    public String rule;

    public String access;
    public String parentObject;
    public String version;
    public String description;
    public Long deviceTypeVersionId;
    public String tr069Name;
    public Boolean instance;
    public String tr069ParentObject;
    // public Set<Long> profile;
    // Use set string temporarily to workaround json convert number to double
    public Set<String> profile;

    public String value;
    public String alias;
    public String type;
    public String settingValue;
    public String isPointer;

}
