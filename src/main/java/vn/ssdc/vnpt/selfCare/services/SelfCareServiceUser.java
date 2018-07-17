/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vn.ssdc.vnpt.selfCare.services;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.common.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import vn.ssdc.vnpt.devices.model.DeviceGroup;
import vn.ssdc.vnpt.devices.services.DeviceGroupService;
import vn.ssdc.vnpt.label.model.Label;
import vn.ssdc.vnpt.label.services.LabelService;
import vn.ssdc.vnpt.selfCare.model.SCDeviceGroup;
import vn.ssdc.vnpt.selfCare.model.SCRole;
import vn.ssdc.vnpt.selfCare.model.SCUser;
import vn.ssdc.vnpt.selfCare.model.searchForm.SCUserSearchForm;
import vn.ssdc.vnpt.user.model.User;
import vn.ssdc.vnpt.user.services.RoleService;
import vn.ssdc.vnpt.user.services.UserService;

/**
 *
 * @author Admin
 */
@Service
public class SelfCareServiceUser {

    @Autowired
    private UserService userService;

    @Autowired
    private DeviceGroupService deviceGroupService;

    @Autowired
    private SelfCareServiceDeviceGroup selfCareServiceDeviceGroup;

    @Autowired
    private SelfCareServiceRole selfCareServiceRole;

    @Autowired
    private RoleService roleService;

    @Autowired
    private LabelService labelService;

    @Autowired
    private SelfCareServiceUser selfCareServiceUser;

    public String generateQuery(SCUserSearchForm scUserSearchForm) {

        Set<String> parameters = new HashSet<>();
        if (!Strings.isNullOrEmpty(scUserSearchForm.fullName)) {
            parameters.add(" full_name LIKE '%" + scUserSearchForm.fullName + "%'");
        }
        if (!Strings.isNullOrEmpty(scUserSearchForm.description)) {
            parameters.add(" description LIKE '%" + scUserSearchForm.description + "%'");
        }
        if (!Strings.isNullOrEmpty(scUserSearchForm.userName)) {
            parameters.add(" user_name LIKE '%" + scUserSearchForm.userName + "%'");
        }
        if (!Strings.isNullOrEmpty(scUserSearchForm.email)) {
            parameters.add(" email LIKE '%" + scUserSearchForm.email + "%'");
        }
        if (!Strings.isNullOrEmpty(scUserSearchForm.phoneNumber)) {
            parameters.add(" phone LIKE '%" + scUserSearchForm.phoneNumber + "%'");
        }

        if (scUserSearchForm.userId != null) {
            parameters.add(" id = " + scUserSearchForm.userId);
        }

        if (scUserSearchForm.deviceGroupIds != null && !scUserSearchForm.deviceGroupIds.isEmpty()) {
            Set<String> tmpParameters = new HashSet<>();
            for (Integer deviceGroupId : scUserSearchForm.deviceGroupIds) {
                tmpParameters.add(" device_group_ids LIKE '%\"" + deviceGroupId + "\"%'");
            }
            parameters.add("(" + String.join(" OR ", tmpParameters) + ")");
        }

        if (scUserSearchForm.roles != null && !scUserSearchForm.roles.isEmpty()) {
            Set<String> tmpParameters = new HashSet<>();
            for (Integer roleId : scUserSearchForm.roles) {
                tmpParameters.add(" role_ids LIKE '%\"" + roleId + "\"%'");
            }
            parameters.add("(" + String.join(" OR ", tmpParameters) + ")");
        }

        if (scUserSearchForm.currentUserName != null){
            Set<String> tmpParameters = new HashSet<>();
            for (String deviceGroupId : selfCareServiceUser.getAllDeviceGroupIds(scUserSearchForm.currentUserName)) {
                tmpParameters.add(" device_group_ids LIKE '%\"" + deviceGroupId + "\"%'");
            }
            parameters.add("(" + String.join(" OR ", tmpParameters) + ")");
        }

        parameters.add(" user_name <> 'ump'");
        String query = String.join(" AND ", parameters);
        return query;
    }

    public List<SCUser> search(SCUserSearchForm scUserSearchForm) {
        List<SCUser> scUsers = new ArrayList<>();
        String query = generateQuery(scUserSearchForm);
        List<User> users = new ArrayList<>();

        if (scUserSearchForm.limit != null && scUserSearchForm.page != null) {
            users = userService.getPage(scUserSearchForm.page - 1, scUserSearchForm.limit, query).getContent();
        } else {
            users = userService.findByQuery(query);
        }
        // convert to SCUser
        for (User user : users) {
            scUsers.add(convertFromUserToSCUser(user));
        }
        return scUsers;
    }

    public long count(SCUserSearchForm scUserSearchForm) {
        String query = generateQuery(scUserSearchForm);
        return userService.countByQuery(query);
    }

    public User convertFromSCUserToUser(SCUser scUser) {

        User user = new User();
        user.userName = scUser.userName;
        user.email = scUser.email;
        user.phone = scUser.phoneNumber;
        user.password = scUser.password;
        user.fullName = scUser.fullName;
        user.description = scUser.description;
        user.id = scUser.userId;

        for (SCDeviceGroup deviceGroup : scUser.deviceGroups) {
            user.deviceGroupNames.add(deviceGroup.name);
            user.deviceGroupIds.add(String.valueOf(deviceGroup.id));
        }

        // Set role data
        for (SCRole role : scUser.roles) {
            user.roleIds.add(String.valueOf(role.id));
            user.roleNames.add(role.name);
            user.operationIds.addAll(role.operationIds);
        }

        return user;
    }

    public SCUser convertFromUserToSCUser(User user) {
        SCUser scUser = new SCUser();
        scUser.userName = user.userName;
        scUser.fullName = user.fullName;
        scUser.phoneNumber = user.phone;
        scUser.email = user.email;
        scUser.description = user.description;
        scUser.password = user.password;
        scUser.fullName = user.fullName;
        scUser.operationIds = user.operationIds;
        scUser.userId = user.id;

        Set<SCDeviceGroup> scDeviceGroups = new HashSet<>();
        for (String deviceGroupId : user.deviceGroupIds) {
            scDeviceGroups.add(selfCareServiceDeviceGroup.convertDeviceGroupToSCDeviceGroup(deviceGroupService.get(Long.valueOf(deviceGroupId))));
        }
        Set<SCRole> scRoles = new HashSet<>();
        for (String roleId : user.roleIds) {
            scRoles.add(selfCareServiceRole.convertRoleToSCRole(roleService.get(Long.valueOf(roleId))));
        }
        scUser.deviceGroups = scDeviceGroups;
        scUser.roles = scRoles;
        return scUser;
    }

    public Set<String> getAllDeviceGroupIds(String userName) {
        List<DeviceGroup> listAllDeviceGroups = deviceGroupService.getAll();
        User user = userService.findByUserName(userName);
        Set<String> deviceGroupIds = new HashSet<>();
        for (DeviceGroup deviceGroup : listAllDeviceGroups) {
            for (String deviceGroupId : user.deviceGroupIds) {
                DeviceGroup userDeviceGroup = deviceGroupService.get(Long.parseLong(deviceGroupId));
                if(checkChildGroup(deviceGroup, userDeviceGroup)){
                    deviceGroupIds.add(deviceGroup.id.toString());
                }
            }
        }
        return deviceGroupIds;
    }

    public boolean checkChildGroup(DeviceGroup child, DeviceGroup parent){
        if(child.id == parent.id){
            return true;
        }

        boolean checkManufacturer = false;
        if(parent.manufacturer == null || parent.manufacturer.equals("All") || parent.manufacturer.equals(child.manufacturer)){
            checkManufacturer = true;
        }

        boolean checkModelName = false;
        if(parent.modelName == null || parent.modelName.equals("All") || parent.modelName.equals(child.modelName)){
            checkModelName = true;
        }

        boolean checkFirmVersion = false;
        if(parent.firmwareVersion == null || parent.firmwareVersion.equals("All") || parent.firmwareVersion.equals(child.firmwareVersion)){
            checkFirmVersion = true;
        }

        boolean checkLabel = false;
        if(parent.labelId == null || parent.labelId.isEmpty()){
           checkLabel = true;
        }
        else{
            Set<String> listAllChildLabelIds = new HashSet<>();
            String[] labelIds = StringUtils.split(parent.labelId,",");
            for(String labelId : labelIds){
                Label label = labelService.get(Long.parseLong(labelId));
                if(label != null) {
                    if(!listAllChildLabelIds.contains(labelId)){
                        listAllChildLabelIds.add(labelId);
                    }
                    Set<Long> currentListChildLabelIds = new HashSet<>();
                    currentListChildLabelIds.add(Long.parseLong(labelId));
                    while (currentListChildLabelIds.size()>0) {
                        Set<Long> tmpListChildLabelIds = new HashSet<>();
                        for(Long childLabelId : currentListChildLabelIds){
                            List<Label> tmpListChildLabels = labelService.loadLabelTreeByNode(childLabelId.toString());
                            for(Label tmpChildLabel : tmpListChildLabels){
                                if(!listAllChildLabelIds.contains(tmpChildLabel.id)){
                                    listAllChildLabelIds.add(tmpChildLabel.id.toString());
                                }
                                tmpListChildLabelIds.add(tmpChildLabel.id);
                            }
                        }
                        currentListChildLabelIds = tmpListChildLabelIds;
                    }
                }
            }
            if(child.labelId == null || child.labelId.isEmpty()){
                checkLabel = false;
            }
            else {
                checkLabel = true;
                String[] childLabelIds = StringUtils.split(child.labelId, ",");
                for (String childLabelId : childLabelIds) {
                    if (!listAllChildLabelIds.contains(childLabelId)) {
                        checkLabel = false;
                    }
                }
            }
        }
        return (checkManufacturer && checkModelName && checkFirmVersion && checkLabel);
    }
}
