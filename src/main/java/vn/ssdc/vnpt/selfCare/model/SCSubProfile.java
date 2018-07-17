/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vn.ssdc.vnpt.selfCare.model;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import vn.ssdc.vnpt.devices.model.ParameterSubProfile;

/**
 *
 * @author Admin
 */
public class SCSubProfile {

    public String sub_title;
    public String sub_object_title;
    public String format;
    public String action;
    public Set<ParameterSubProfile> parameters;
    
    public List<SCObjectInstanceInSubProfile> list_sub_object;
    
    public String parentObject;
    public Set<ParameterSubProfile> list_parameters_in_sub_profile;
 
}
