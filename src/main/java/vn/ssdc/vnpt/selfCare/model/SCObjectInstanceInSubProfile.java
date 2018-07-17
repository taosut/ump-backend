/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vn.ssdc.vnpt.selfCare.model;

import java.util.Set;
import vn.ssdc.vnpt.devices.model.ParameterSubProfile;

/**
 *
 * @author Admin
 */
public class SCObjectInstanceInSubProfile {
    
    public int instance;
    public String sub_alias;
    public Set<ParameterSubProfile> list_parameters_in_sub_profile;
    
}
