/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vn.ssdc.vnpt.devices.model;

import vn.vnpt.ssdc.utils.ObjectUtils;

/**
 *
 * @author Admin
 */
public class ParameterSubProfile {

    public String path;
    public String subPath;
    public String shortName;
    public String dataType;
    public String value;
    public String defaultValue;
    //range [1-4], list of possible values [1,2,3]
    public String rule;
    public String inputType;
    public Integer useSubscriberData;
    public String tr069Name;
    public String access; //ReadOnly or ReadWrite
    public String parentObject;
    public String subscriberData;
    public Boolean instance;
    public String tr069ParentObject;

    // setting profile
    public String readOnly;
    public String alias;
    public String type;
    public String settingValue;
    public String isPointer;
    public Long position;
    public String settingName;

    public String getValue() {
        return ObjectUtils.empty(value) ? (ObjectUtils.empty(defaultValue) ? "" : defaultValue) : value;
    }

}
