/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vn.ssdc.vnpt.selfCare.services;

import com.google.common.base.Strings;

import java.util.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import vn.ssdc.vnpt.AcsClient;
import vn.ssdc.vnpt.alarm.model.AlarmType;
import vn.ssdc.vnpt.alarm.services.AlarmTypeService;
import vn.ssdc.vnpt.common.services.ConfigurationService;
import vn.ssdc.vnpt.devices.model.DeviceGroup;
import vn.ssdc.vnpt.devices.model.DeviceTypeVersion;
import vn.ssdc.vnpt.devices.services.DeviceGroupService;
import vn.ssdc.vnpt.devices.services.DeviceTypeVersionService;
import vn.ssdc.vnpt.label.model.Label;
import vn.ssdc.vnpt.label.services.LabelService;
import vn.ssdc.vnpt.performance.model.PerformanceSetting;
import vn.ssdc.vnpt.performance.sevices.PerformanceSettingService;
import vn.ssdc.vnpt.policy.services.PolicyJobService;
import vn.ssdc.vnpt.selfCare.model.SCDevice;
import vn.ssdc.vnpt.selfCare.model.SCDeviceGroup;
import vn.ssdc.vnpt.selfCare.model.searchForm.SCDeviceGroupSearchForm;
import vn.ssdc.vnpt.user.model.User;
import vn.ssdc.vnpt.user.services.UserService;

/**
 *
 * @author Admin
 */
@Service
public class SelfCareServiceDeviceGroup {

    public final String DEVICE_GROUP_ID = "id";
    public final String DEVICE_GROUP_NAME = "name";
    public final String DEVICE_GROUP_MANUFACTURER = "manufacturer";
    public final String DEVICE_GROUP_MODEL_NAME = "model_name";
    public final String DEVICE_GROUP_FIRMWARE_VERSION = "firmware_version";
    public final String DEVICE_GROUP_LABEL = "label";

    @Autowired
    private SelfCareServiceUser selfCareServiceUser;

    @Autowired
    private DeviceGroupService deviceGroupService;

    @Autowired
    private DeviceTypeVersionService deviceTypeVersionService;

    @Autowired
    private LabelService labelService;

    @Autowired
    private PolicyJobService policyJobService;

    @Autowired
    private UserService userService;

    @Autowired
    private AlarmTypeService alarmTypeService;

    @Autowired
    private PerformanceSettingService performanceSettingService;

    @Autowired
    private AcsClient acsClient;

    @Autowired
    private SelfCareServiceDevice selfCareServiceDevice;

    @Autowired
    private ConfigurationService configurationService;

    /**
     * generate query from deviceGroupSearchForm
     *
     * @param scDeviceGroupSearchForm
     * @return
     */
    public String generateQueryDeviceGroup(SCDeviceGroupSearchForm scDeviceGroupSearchForm) {
        String whereExp = "";

        if (scDeviceGroupSearchForm.id != null) {
            whereExp += " and " + DEVICE_GROUP_ID + " = '" + scDeviceGroupSearchForm.id + "'";
        }

        if (!Strings.isNullOrEmpty(scDeviceGroupSearchForm.deviceGroupName)) {
            whereExp += " and " + DEVICE_GROUP_NAME + " like '%" + scDeviceGroupSearchForm.deviceGroupName + "%'";
        }

        if (!Strings.isNullOrEmpty(scDeviceGroupSearchForm.firmwareVersion)) {
            whereExp += " and " + DEVICE_GROUP_FIRMWARE_VERSION + " like '%" + scDeviceGroupSearchForm.firmwareVersion + "%'";
        }

        if (!Strings.isNullOrEmpty(scDeviceGroupSearchForm.manufacture)) {
            whereExp += " and " + DEVICE_GROUP_MANUFACTURER + " like '%" + scDeviceGroupSearchForm.manufacture + "%'";
        }

        if (!Strings.isNullOrEmpty(scDeviceGroupSearchForm.modelName)) {
            whereExp += " and " + DEVICE_GROUP_MODEL_NAME + " like '%" + scDeviceGroupSearchForm.modelName + "%'";
        }

        if (!Strings.isNullOrEmpty(scDeviceGroupSearchForm.label)) {
            if (scDeviceGroupSearchForm.label.contains(",")) {
                String[] labels = scDeviceGroupSearchForm.label.split(",");
                for (String label : labels) {
                    if (!label.isEmpty()) {
                        whereExp += " or " + DEVICE_GROUP_LABEL + " like '%" + label + "%'";
                    }
                }
                if (!whereExp.isEmpty() && whereExp.startsWith(" or")) {
                    whereExp = whereExp.substring(3);
                }
            } else {
                whereExp += " and " + DEVICE_GROUP_LABEL + " like '%" + scDeviceGroupSearchForm.label + "%'";
            }
        }

        if (!Strings.isNullOrEmpty(scDeviceGroupSearchForm.userName)) {
            String deviceGroupIds = selfCareServiceUser.getAllDeviceGroupIds(scDeviceGroupSearchForm.userName).toString();
            deviceGroupIds = deviceGroupIds.replaceAll("[\"\\[\\]]", "");
            if (!whereExp.isEmpty()) {
                whereExp += " and id IN (" + deviceGroupIds + ")";
            } else {
                whereExp += " id IN (" + deviceGroupIds + ")";
            }
        }

        if (!whereExp.isEmpty() && whereExp.startsWith(" and")) {
            whereExp = whereExp.substring(4);
        }

        return whereExp;
    }

    /**
     * find List SCDeviceGroup from deviceGroupSearchForm
     *
     * @param sCDeviceGroupSearchForm
     * @return
     */
    public List<SCDeviceGroup> search(SCDeviceGroupSearchForm sCDeviceGroupSearchForm) {
        List<DeviceGroup> deviceGroups = new ArrayList<>();
        String whereExp = generateQueryDeviceGroup(sCDeviceGroupSearchForm);
        if (sCDeviceGroupSearchForm.page == null) {
            sCDeviceGroupSearchForm.page = Integer.valueOf(configurationService.get("page_default").value);
        }
        if (sCDeviceGroupSearchForm.limit == null) {
//            sCDeviceGroupSearchForm.limit = Integer.valueOf(configurationService.get("limit_default").value);
            deviceGroups = deviceGroupService.findByQuery(whereExp);
        } else {
            deviceGroups = deviceGroupService.findByPage(String.valueOf(sCDeviceGroupSearchForm.limit), String.valueOf(sCDeviceGroupSearchForm.page - 1), whereExp);
        }
        List<SCDeviceGroup> scDeviceGroups = new ArrayList<>();
        for (DeviceGroup deviceGroup : deviceGroups) {
            scDeviceGroups.add(convertDeviceGroupToSCDeviceGroup(deviceGroup));
        }
        return scDeviceGroups;
    }

    public List<SCDeviceGroup> findByDevice(String deviceId) {
        List<SCDeviceGroup> scDeviceGroups = new ArrayList<>();
        try {
            SCDevice device = selfCareServiceDevice.getDevice(deviceId);
            Set<String> conditionOrs = new HashSet<>();
            if (device.labelIds != null) {
                for (Long labelId : device.labelIds) {
                    conditionOrs.add(String.format("label_id = '%s,'", labelId));
                }
            }
            conditionOrs.add(String.format("devices like '%s'", "%" + deviceId + "%"));
            Set<String> conditionAnds = new HashSet<>();
            //(manufacturer = 'all' or manufacturer like 'VNPT Technology') AND (firmware_version = 'all' or firmware_version like '1.170DEV') AND devices like '%A65-STB-0121335S1F080244%' AND (model_name = 'all' or model_name like 'iGate IP001HD')
            conditionAnds.add(String.join(" OR ", conditionOrs));
            conditionAnds.add(String.format("(manufacturer = 'all' or manufacturer like '%s')", device.manufacturer));
            conditionAnds.add(String.format("(model_name = 'all' or model_name like '%s')", device.modelName));
            conditionAnds.add(String.format("(firmware_version = 'all' or firmware_version like '%s')", device.firmwareVersion));
            String query = String.join(" AND ", conditionAnds);
            System.out.println(query);
            List<DeviceGroup> deviceGroups = deviceGroupService.findByQuery(query);
            for (DeviceGroup deviceGroup : deviceGroups) {
                scDeviceGroups.add(convertDeviceGroupToSCDeviceGroup(deviceGroup));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return scDeviceGroups;
    }

    public long count(SCDeviceGroupSearchForm sCDeviceGroupSearchForm) {
        String whereExp = generateQueryDeviceGroup(sCDeviceGroupSearchForm);
        long totalAll = deviceGroupService.count(whereExp);
        return totalAll;
    }

    public SCDeviceGroup create(SCDeviceGroup scDeviceGroup) {
        DeviceGroup deviceGroup = new DeviceGroup();
        deviceGroup.manufacturer = scDeviceGroup.manufacturer;
        deviceGroup.modelName = scDeviceGroup.modelName;
        deviceGroup.firmwareVersion = scDeviceGroup.firmwareVersion;

        if (deviceGroup.manufacturer == null || ("").equals(deviceGroup.manufacturer)) {
            deviceGroup.manufacturer = "All";
        }
        if (deviceGroup.modelName == null || ("").equals(deviceGroup.modelName)) {
            deviceGroup.modelName = "All";
        }

        if (!("All").equals(deviceGroup.manufacturer) && !("All").equals(deviceGroup.modelName)) {
            List<DeviceTypeVersion> deviceTypeVersions = deviceTypeVersionService.findByManufacturerAndModelName(deviceGroup.manufacturer, deviceGroup.modelName);
            if (deviceTypeVersions != null && !deviceTypeVersions.isEmpty()) {
                deviceGroup.oui = deviceTypeVersions.get(0).oui;
                deviceGroup.productClass = deviceTypeVersions.get(0).productClass;
            }
        }

        if (deviceGroup.firmwareVersion == null || ("").equals(deviceGroup.firmwareVersion)) {
            deviceGroup.firmwareVersion = "All";
        }

        deviceGroup.name = scDeviceGroup.name;
        if (scDeviceGroup.devices == null || scDeviceGroup.devices.isEmpty()) {
            deviceGroup.devices = new HashSet<>();
            if (scDeviceGroup.labelIds == null || scDeviceGroup.labelIds.size() == 0) {
                deviceGroup.label = "";
                deviceGroup.labelId = "";
            } else {
                deviceGroup.label = createAllLabel(scDeviceGroup);
                deviceGroup.labelId = scDeviceGroup.labelIds.toString().replaceAll("[\"\\[\\]]", "").replaceAll("\\s+", "") + ",";
            }
        } else {
            deviceGroup.devices = scDeviceGroup.devices;
        }

        return convertDeviceGroupToSCDeviceGroup(deviceGroupService.create(deviceGroup));
    }

    public SCDeviceGroup update(Long id, SCDeviceGroup scDeviceGroup) {
        DeviceGroup deviceGroup = new DeviceGroup();
        deviceGroup.id = id;
        deviceGroup.manufacturer = scDeviceGroup.manufacturer;
        deviceGroup.modelName = scDeviceGroup.modelName;
        deviceGroup.firmwareVersion = scDeviceGroup.firmwareVersion;

        if (deviceGroup.manufacturer == null || ("").equals(deviceGroup.manufacturer)) {
            deviceGroup.manufacturer = "All";
        }
        if (deviceGroup.modelName == null || ("").equals(deviceGroup.modelName)) {
            deviceGroup.modelName = "All";
        }

        if (!("All").equals(deviceGroup.manufacturer) && !("All").equals(deviceGroup.modelName)) {
            List<DeviceTypeVersion> deviceTypeVersions = deviceTypeVersionService.findByManufacturerAndModelName(deviceGroup.manufacturer, deviceGroup.modelName);
            if (deviceTypeVersions != null && !deviceTypeVersions.isEmpty()) {
                deviceGroup.oui = deviceTypeVersions.get(0).oui;
                deviceGroup.productClass = deviceTypeVersions.get(0).productClass;
            }
        }
        if (deviceGroup.firmwareVersion == null || ("").equals(deviceGroup.firmwareVersion)) {
            deviceGroup.firmwareVersion = null;
        }
        deviceGroup.name = scDeviceGroup.name;
        System.out.println(deviceGroup.toString());
        if (scDeviceGroup.labelIds == null || scDeviceGroup.labelIds.size() == 0) {
            deviceGroup.label = "";
            deviceGroup.labelId = "";
        } else {
            deviceGroup.label = createAllLabel(scDeviceGroup);
            deviceGroup.labelId = scDeviceGroup.labelIds.toString().replaceAll("[\"\\[\\]]", "").replaceAll("\\s+", "") + ",";
        }

        if (scDeviceGroup.devices != null) {
            deviceGroup.devices = scDeviceGroup.devices;
        } else {
            deviceGroup.devices = new HashSet<>();
        }
        return convertDeviceGroupToSCDeviceGroup(deviceGroupService.update(id, deviceGroup));
    }

    public SCDeviceGroup convertDeviceGroupToSCDeviceGroup(DeviceGroup deviceGroup) {
        SCDeviceGroup scDeviceGroup = new SCDeviceGroup();
        scDeviceGroup.firmwareVersion = deviceGroup.firmwareVersion;
        scDeviceGroup.manufacturer = deviceGroup.manufacturer;
        scDeviceGroup.id = deviceGroup.id;
        scDeviceGroup.name = deviceGroup.name;
        scDeviceGroup.query = deviceGroup.query;
        scDeviceGroup.modelName = deviceGroup.modelName;
        scDeviceGroup.productClass = deviceGroup.productClass;
        scDeviceGroup.oui = deviceGroup.oui;

        if (!Strings.isNullOrEmpty(deviceGroup.labelId)) {
            Set<Long> labelIdsDeviceGroup = new HashSet<Long>();
            Set<String> labelsDeviceGroup = new HashSet<String>();
            if (deviceGroup.labelId.contains(",")) {
                String label = deviceGroup.labelId.substring(0, deviceGroup.labelId.length() - 1);
                String[] labels = label.split(",");
                for (int i = 0; i < labels.length; i++) {
                    Long labelId = Long.valueOf(labels[i].trim());
                    labelIdsDeviceGroup.add(labelId);
                    labelsDeviceGroup.add(labelService.get(labelId).name);
                }
            }
            scDeviceGroup.labelIds = labelIdsDeviceGroup;
            scDeviceGroup.labels = labelsDeviceGroup;
        }
        scDeviceGroup.devices = deviceGroup.devices;
        return scDeviceGroup;
    }

    public boolean checkDeviceGroupInUsed(long deviceGroupId) {
        Boolean policyJobses = policyJobService.findJobExecute(String.valueOf(deviceGroupId), "");
        if (policyJobses) {
            return true;
        }

        List<User> users = userService.getListUserByDeviceGroupId(String.valueOf(deviceGroupId));
        if (!users.isEmpty()) {
            return true;
        }

        List<AlarmType> alarmTypes = alarmTypeService.getListAlarmByDeviceGroupId(String.valueOf(deviceGroupId));
        if (!alarmTypes.isEmpty()) {
            return true;
        }

        List<PerformanceSetting> performanceSettings = performanceSettingService.getListPerformanceByDeviceGroupId(String.valueOf(deviceGroupId));
        if (!performanceSettings.isEmpty()) {
            return true;
        }

        return false;
    }

    public String createAllLabel(SCDeviceGroup scDeviceGroup) {
        Long[] ids = scDeviceGroup.labelIds.toArray(new Long[scDeviceGroup.labelIds.size()]);
        String[] names = scDeviceGroup.labels.toArray(new String[scDeviceGroup.labels.size()]);
        String label = "";
        for (int i = 0; i < ids.length; i++) {
            List<Label> labels = labelService.loadLabelTreeByNode(labelService.get(ids[i]).parentId);
            label += " OR " + createLabel(labels, names[i]);
        }
        return label.substring(4);
    }

    public String createLabel(List<Label> labels, String parentName) {
        String totalChild = "";
        Label label = null;
        String parentId = "";
        do {
            for (Label tmp : labels) {
                if (label != null) {
                    parentId = String.valueOf(label.parentId);
                } else {
                    parentId = tmp.parentId;
                }
                if (!parentId.equals("0")) {
                    label = labelService.get(Long.valueOf(parentId));
                    totalChild += " AND " + label.name;
                }
            }

        } while (!parentId.equals("0"));
        return parentName + totalChild;
    }
}
