/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vn.ssdc.vnpt.selfCare.services;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import vn.ssdc.vnpt.devices.model.DeviceGroup;
import vn.ssdc.vnpt.devices.services.DeviceGroupService;
import vn.ssdc.vnpt.selfCare.model.SCRole;
import vn.ssdc.vnpt.user.model.Role;
import vn.ssdc.vnpt.user.model.User;
import vn.ssdc.vnpt.user.services.UserService;

/**
 *
 * @author Admin
 */
@Service
public class SelfCareServiceRole {
    public SCRole convertRoleToSCRole(Role role) {
        SCRole scRole = new SCRole();
        scRole.description = role.description;
        scRole.id = role.id;
        scRole.operationIds = role.operationIds;
        scRole.permissionsIds = role.permissionsIds;
        scRole.name = role.name;
        return scRole;
    }

}
