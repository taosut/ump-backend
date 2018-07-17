/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vn.ssdc.vnpt.selfCare.model;

import java.util.Set;
import vn.ssdc.vnpt.devices.model.DeviceGroup;
import vn.ssdc.vnpt.user.model.Role;

/**
 *
 * @author Admin
 */
public class SCUser {

    public String userName;
    public String fullName;
    public String email;
    public String phoneNumber;
    public Set<SCDeviceGroup> deviceGroups;
    public Set<SCRole> roles;
    public String password;
    public String description;
    public Set<String> operationIds;
    public Long userId;
}
