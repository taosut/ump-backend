package vn.ssdc.vnpt.devices.services;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import vn.ssdc.vnpt.AcsClient;
import vn.ssdc.vnpt.devices.model.*;
import vn.vnpt.ssdc.core.ObjectCache;
import vn.vnpt.ssdc.core.SsdcCrudService;
import vn.vnpt.ssdc.jdbc.factories.RepositoryFactory;
import vn.vnpt.ssdc.utils.ObjectUtils;

import java.util.*;

/**
 * Created by vietnq on 11/1/16.
 */
@Service
public class DeviceTypeVersionService extends SsdcCrudService<Long, DeviceTypeVersion> {

    private static final Logger logger = LoggerFactory.getLogger(DeviceTypeVersionService.class);

    private static final String SOFTWARE_VERSION_KEY = "summary.softwareVersion";
    private static final String DEVICEID_KEY = "_deviceId";

    @Autowired
    private TagService tagService;

    @Autowired
    private AcsClient acsClient;

    @Autowired
    private DeviceTypeService deviceTypeService;

    @Autowired
    private ObjectCache ssdcCache;

    @Autowired
    DeviceGroupService deviceGroupService;

    @Autowired
    private ParameterDetailService parameterDetailService;

    @Autowired
    public DeviceTypeVersionService(RepositoryFactory repositoryFactory) {
        this.repository = repositoryFactory.create(DeviceTypeVersion.class);
    }

    @Override
    public DeviceTypeVersion get(Long aLong) {
        DeviceTypeVersion deviceTypeVersion = super.get(aLong);
        if (!ObjectUtils.empty(deviceTypeVersion.parameters)) {
            deviceTypeVersion.parameters = new TreeMap<String, Parameter>(deviceTypeVersion.parameters);
        }
        if (!ObjectUtils.empty(deviceTypeVersion.diagnostics)) {
            deviceTypeVersion.diagnostics = new TreeMap<String, Tag>(deviceTypeVersion.diagnostics);
        }
        return deviceTypeVersion;
    }

    public List<DeviceTypeVersion> findByDeviceType(Long deviceTypeId) {
        StringBuilder sb = new StringBuilder("device_type_id=?");
        Sort sort = new Sort(Sort.Direction.DESC, "created");
        return this.repository.search(sb.toString(), sort, deviceTypeId);
    }

    public List<DeviceTypeVersion> findByDeviceTypeVersionForReports(String model_name, String firmware_version) {
        StringBuilder sb = new StringBuilder("");
        if (model_name != null) {
            if (firmware_version != null) {
                sb.append("model_name in ('%s') and firmware_version in ('%s')");
                Sort sort = new Sort(Sort.Direction.DESC, "created");
                return this.repository.search(String.format(sb.toString(), model_name, firmware_version), sort);
            } else {
                sb.append("model_name in ('%s')");
                Sort sort = new Sort(Sort.Direction.DESC, "created");
                return this.repository.search(String.format(sb.toString(), model_name), sort);
            }
        } else {
            if (firmware_version != null) {
                sb.append("firmware_version in ('%s')");
                Sort sort = new Sort(Sort.Direction.DESC, "created");
                return this.repository.search(String.format(sb.toString(), firmware_version), sort);
            } else {
                sb.append(" 1 = 1");
                Sort sort = new Sort(Sort.Direction.DESC, "created");
                return this.repository.search(sb.toString(), sort);
            }
        }
    }

    public boolean checkExistSimilarDeviceTypeVersion(long deviceTypeId, long deviceTypeVersionId) {
        StringBuilder sb = new StringBuilder("device_type_id=? AND id!=?");
        Sort sort = new Sort(Sort.Direction.DESC, "created");
        boolean result = false;
        if (this.repository.search(sb.toString(), sort, deviceTypeId, deviceTypeVersionId).size() > 0) {
            result = true;
        }
        return result;
    }

    public DeviceTypeVersion findByPk(Long deviceTypeId, String version) {
        String cacheId = "device-type-version-" + deviceTypeId.toString() + "-" + version;
        DeviceTypeVersion result;
        try {
            result = (DeviceTypeVersion) ssdcCache.get(cacheId, DeviceTypeVersion.class);
            if (result != null) {
                return result;
            } else {
                String whereExp = "device_type_id=? and firmware_version=?";
                List<DeviceTypeVersion> versions = this.repository.search(whereExp, deviceTypeId, version);
                if (!ObjectUtils.empty(versions)) {
                    DeviceTypeVersion deviceTypeVersion = versions.get(0);
                    if (!ObjectUtils.empty(deviceTypeVersion.parameters)) {
                        deviceTypeVersion.parameters = new TreeMap<String, Parameter>(deviceTypeVersion.parameters);
                    }
                    if (!ObjectUtils.empty(deviceTypeVersion.diagnostics)) {
                        deviceTypeVersion.diagnostics = new TreeMap<String, Tag>(deviceTypeVersion.diagnostics);
                    }
                    result = deviceTypeVersion;
                    ssdcCache.put(cacheId, result, DeviceTypeVersion.class);
                    return result;
                }
            }
        } catch (Exception e) {
            String whereExp = "device_type_id=? and firmware_version=?";
            List<DeviceTypeVersion> versions = this.repository.search(whereExp, deviceTypeId, version);
            if (!ObjectUtils.empty(versions)) {
                DeviceTypeVersion deviceTypeVersion = versions.get(0);
                if (!ObjectUtils.empty(deviceTypeVersion.parameters)) {
                    deviceTypeVersion.parameters = new TreeMap<String, Parameter>(deviceTypeVersion.parameters);
                }
                if (!ObjectUtils.empty(deviceTypeVersion.diagnostics)) {
                    deviceTypeVersion.diagnostics = new TreeMap<String, Tag>(deviceTypeVersion.diagnostics);
                }
                result = deviceTypeVersion;
                ssdcCache.put(cacheId, result, DeviceTypeVersion.class);
                return result;
            }
        }
        return null;
    }

    @Override
    public void afterDelete(DeviceTypeVersion deviceTypeVersion) {
        String cacheId = "device-type-version-" + deviceTypeVersion.deviceTypeId.toString() + "-" + deviceTypeVersion.firmwareVersion;
        ssdcCache.remove(cacheId, DeviceTypeVersion.class);
    }

    public List<DeviceTypeVersion> findByDeviceTypeAndVersion(Long deviceTypeId, String version) {
        String whereExp = "device_type_id=? and firmware_version=?";
        List<DeviceTypeVersion> versions = this.repository.search(whereExp, deviceTypeId, version);
        if (!ObjectUtils.empty(versions)) {
            for (DeviceTypeVersion deviceTypeVersion : versions) {
                if (!ObjectUtils.empty(deviceTypeVersion.parameters)) {
                    deviceTypeVersion.parameters = new TreeMap<String, Parameter>(deviceTypeVersion.parameters);
                }
                if (!ObjectUtils.empty(deviceTypeVersion.diagnostics)) {
                    deviceTypeVersion.diagnostics = new TreeMap<String, Tag>(deviceTypeVersion.diagnostics);
                }
            }
            return versions;
        }
        return null;
    }

    public DeviceTypeVersion findbyDevice(String deviceId) {
        String paramters = "summary.softwareVersion,_deviceId._OUI,_deviceId._Manufacturer,_deviceId._ProductClass";
        ResponseEntity response = acsClient.getDevice(deviceId, paramters);
        String body = (String) response.getBody();
        JsonArray array = new Gson().fromJson(body, JsonArray.class);
        String version = "";
        if (array.size() > 0) {
            JsonObject object = array.get(0).getAsJsonObject();
            if (object.get(SOFTWARE_VERSION_KEY) != null && object.get(SOFTWARE_VERSION_KEY).getAsJsonObject().get("_value") != null) {
                version = object.get("summary.softwareVersion").getAsJsonObject().get("_value").getAsString();
            }
            String manufacture = object.get(DEVICEID_KEY).getAsJsonObject().get("_Manufacturer").getAsString();
            String productClass = object.get(DEVICEID_KEY).getAsJsonObject().get("_ProductClass").getAsString();
            String oui = object.get(DEVICEID_KEY).getAsJsonObject().get("_OUI").getAsString();
            DeviceType deviceType = deviceTypeService.findByPk(manufacture, oui, productClass);
            if (deviceType != null && version != "") {
                return findByPk(deviceType.id, version);
            }
        }
        return null;
    }

    public DeviceTypeVersion findByFirmwareVersion(String firmwareVersion) {
        String whereExp = "firmware_version=?";
        List<DeviceTypeVersion> deviceTypeVersions = this.repository.search(whereExp, firmwareVersion);
        if (!ObjectUtils.empty(deviceTypeVersions) && !deviceTypeVersions.isEmpty()) {
            return deviceTypeVersions.get(0);
        }
        return null;
    }

    public List<DeviceTypeVersion> findByManufacturer(String manufacturer) {
        String whereExp = "manufacturer=?";
        List<DeviceTypeVersion> deviceTypeVersions = this.repository.search(whereExp, manufacturer);
        return deviceTypeVersions;
    }

    public List<DeviceTypeVersion> searchDevices(String limit, String indexPage, String deviceTypeId) {
        List<DeviceTypeVersion> deviceTypeVersions = new ArrayList<DeviceTypeVersion>();
        Page<DeviceTypeVersion> all = null;
        if (!deviceTypeId.trim().equals("")) {
            deviceTypeVersions = this.repository.search("device_type_id=?", deviceTypeId);
        } else {
            all = this.repository.findAll(new PageRequest(Integer.parseInt(indexPage), Integer.parseInt(limit)));
            deviceTypeVersions = all.getContent();
        }
        return deviceTypeVersions;
    }

    public Map<String, DeviceTypeVersion> convertIdToDeviceTypeVersion(List<Tag> tagList) {
        Map<String, DeviceTypeVersion> deviceTypeVersionSet = new HashMap<String, DeviceTypeVersion>();
        for (Tag tag : tagList) {
            DeviceTypeVersion deviceTypeVersion = this.repository.findOne(tag.deviceTypeVersionId);
            DeviceType deviceType = deviceTypeService.get(deviceTypeVersion.deviceTypeId);
            deviceTypeVersionSet.put(deviceType.manufacturer + deviceType.productClass + deviceTypeVersion.firmwareVersion, deviceTypeVersion);
        }
        return deviceTypeVersionSet;

    }

    public List<DeviceTypeVersion> getDeviceTypeIDForSortAndSearch(String manufacturer, String modelName, String sort, String limit, String indexPage) {
        String whereExp = "";
        List<DeviceTypeVersion> deviceTypeVersions = new ArrayList<DeviceTypeVersion>();
        Page<DeviceTypeVersion> all = null;
        if (sort != null && (!manufacturer.equals("All") || !modelName.equals("All"))) {
            // search + sort
            String[] sortSplit = sort.split(":");
            Sort sortField = null;
            if (!manufacturer.equals("All") && (modelName.equals("All") || modelName.equals(""))) {
                whereExp += "manufacturer=?";
                sortField = new Sort(Sort.Direction.ASC, sortSplit[0]);
                if (sortSplit[1].contains("-1")) {
                    sortField = new Sort(Sort.Direction.DESC, sortSplit[0]);
                }
                deviceTypeVersions = this.repository.search(whereExp, new PageRequest(Integer.parseInt(indexPage), Integer.parseInt(limit),
                        sortField), manufacturer).getContent();

            } else if (!modelName.equals("All") && (manufacturer.equals("All") || manufacturer.equals(""))) {
                whereExp += "model_name=?";
                sortField = new Sort(Sort.Direction.ASC, sortSplit[0]);
                if (sortSplit[1].contains("-1")) {
                    sortField = new Sort(Sort.Direction.DESC, sortSplit[0]);
                }
                deviceTypeVersions = this.repository.search(whereExp, new PageRequest(Integer.parseInt(indexPage), Integer.parseInt(limit),
                        sortField), modelName).getContent();
            } else if (!modelName.equals("All") && !manufacturer.equals("All")
                    && !modelName.equals("") && !manufacturer.equals("")) {
                whereExp += "model_name=? and manufacturer=?";
                sortField = new Sort(Sort.Direction.ASC, "model_name", sortSplit[0]);
                if (sortSplit[1].contains("-1")) {
                    sortField = new Sort(Sort.Direction.DESC, "model_name", sortSplit[0]);
                }
                deviceTypeVersions = this.repository.search(whereExp, new PageRequest(Integer.parseInt(indexPage), Integer.parseInt(limit),
                        sortField), modelName, manufacturer).getContent();
            }
        } else if (sort != null && !sort.equals("null")) {
            // sort
            String[] sortSplit = sort.split(":");
            if (sortSplit[1].contains("-1")) {
                all = this.repository.findAll(new PageRequest(Integer.parseInt(indexPage), Integer.parseInt(limit),
                        Sort.Direction.DESC, sortSplit[0]));
            } else {
                all = this.repository.findAll(new PageRequest(Integer.parseInt(indexPage), Integer.parseInt(limit),
                        Sort.Direction.ASC, sortSplit[0]));
            }
            deviceTypeVersions = all.getContent();
        } else {
            Sort sortRequried = new Sort(Sort.Direction.ASC, "manufacturer")
                    .and(new Sort(Sort.Direction.ASC, "created"));
            all = this.repository.findAll(new PageRequest(Integer.parseInt(indexPage), Integer.parseInt(limit), sortRequried));
            deviceTypeVersions = all.getContent();
        }

        return deviceTypeVersions;
    }

    public int countDeviceTypeIDForSortAndSearch(String manufacturer, String modelName, String sort, String limit, String indexPage) {
        String whereExp = "";
        int count = 0;
        if (sort != null && (!manufacturer.equals("All") || !modelName.equals("All"))) {
            // search + sort
            String[] sortSplit = sort.split(":");
            Sort sortField = null;
            if (!manufacturer.equals("All") && (modelName.equals("All") || modelName.equals(""))) {
                whereExp += "manufacturer=?";
                sortField = new Sort(Sort.Direction.ASC, "manufacturer");
                if (sortSplit[1].contains("-1")) {
                    sortField = new Sort(Sort.Direction.DESC, "manufacturer");
                }
                count = this.repository.search(whereExp, sortField, manufacturer).size();

            } else if (!modelName.equals("All") && (manufacturer.equals("All") || manufacturer.equals(""))) {
                whereExp += "model_name=?";
                sortField = new Sort(Sort.Direction.ASC, "model_name");
                if (sortSplit[1].contains("-1")) {
                    sortField = new Sort(Sort.Direction.DESC, "model_name");
                }
                count = this.repository.search(whereExp, sortField, modelName).size();
            } else if (!modelName.equals("All") && !manufacturer.equals("All")
                    && !modelName.equals("") && !manufacturer.equals("")) {
                whereExp += "model_name=? and manufacturer=?";
                sortField = new Sort(Sort.Direction.ASC, "model_name", "manufacturer");
                if (sortSplit[1].contains("-1")) {
                    sortField = new Sort(Sort.Direction.DESC, "model_name", "manufacturer");
                }
                count = this.repository.search(whereExp, sortField, modelName, manufacturer).size();
            }
        } else if (sort != null && !sort.equals("null")) {
            // sort
            String[] sortSplit = sort.split(":");
            if (sortSplit[1].contains("-1")) {
                count = this.repository.findAll(new Sort(Sort.Direction.DESC, sortSplit[0])).size();
            } else {
                count = this.repository.findAll(new Sort(Sort.Direction.ASC, sortSplit[0])).size();
            }

        } else {
            Sort sortRequried = new Sort(Sort.Direction.ASC, "manufacturer")
                    .and(new Sort(Sort.Direction.ASC, "created"));
            count = this.repository.findAll(new PageRequest(Integer.parseInt(indexPage), Integer.parseInt(limit), sortRequried)).getContent().size();
        }
        return count;
    }

    public Map<String, Long> generateDeviceTypeVersionWithDeviceId() {
        Map<String, Long> map = new HashMap<String, Long>();
        List<DeviceType> deviceTypeList = deviceTypeService.getAll();
        for (DeviceType tmp : deviceTypeList) {
            Long deviceTypeId = tmp.id;
            List<DeviceTypeVersion> deviceTypeVersionList = findByDeviceType(deviceTypeId);
            for (DeviceTypeVersion deviceTypeVersion : deviceTypeVersionList) {
                map.put(tmp.manufacturer + "@@@" + tmp.productClass + "@@@" + deviceTypeVersion.firmwareVersion, deviceTypeVersion.id);
            }
        }
        return map;
    }

    public Page<DeviceTypeVersion> getPage(int page, int limit) {
        return this.repository.findAll(new PageRequest(page, limit));
    }

    public List<DeviceTypeVersion> findByManufacturerAndModelName(String manufacturer, String modelName) {
        List<DeviceTypeVersion> deviceTypeVersions = new ArrayList<DeviceTypeVersion>();
        deviceTypeVersions = this.repository.search("model_name=? and manufacturer=?", modelName, manufacturer);
        return deviceTypeVersions;
    }

    public List<DeviceTypeVersion> findByManufacturerAndModelNameAndFrimware(String manufacturer, String modelName, String firmware_version) {
        List<DeviceTypeVersion> deviceTypeVersions = new ArrayList<DeviceTypeVersion>();
        String whereExp = "manufacturer='" + manufacturer + "'";
        if (modelName != null && !modelName.isEmpty()) {
            whereExp += " and model_name='" + modelName + "'";
        }
        if (firmware_version != null && !firmware_version.isEmpty()) {
            whereExp += " and firmware_version='" + firmware_version + "'";
        }
        deviceTypeVersions = this.repository.search(whereExp);
        return deviceTypeVersions;
    }

    public String pingDevice(String ipDevice) {
        ResponseEntity response = acsClient.pingDevice(ipDevice);
        String body = (String) response.getBody();
        if (!response.toString().contains("200 OK,PING")) {
            body = "Error Ping To " + ipDevice + " . Please Try Again Later !";
        }
        return body;
    }

    public List<DeviceTypeVersion> findByListDeviceGroup(String listDeviceGroup) {
        List<DeviceTypeVersion> deviceTypeVersions = new LinkedList<>();
        String whereExp = "";
        List<DeviceGroup> deviceGroupList = deviceGroupService.findAllByDeviceGroupIds(listDeviceGroup);
        boolean isAll = false;
        for (int i = 0; i < deviceGroupList.size(); i++) {
            DeviceGroup deviceGroup = deviceGroupList.get(i);
            if (("All").equals(deviceGroup.manufacturer) && ("All").equals(deviceGroup.modelName) && deviceGroup.firmwareVersion == null) {
                deviceTypeVersions = this.repository.findAll();
                isAll = true;
                break;
            }
            if (!("All").equals(deviceGroup.manufacturer) && ("All").equals(deviceGroup.modelName) && deviceGroup.firmwareVersion == null) {
                whereExp += " manufacturer='" + deviceGroup.manufacturer + "' or";
            }
            if (!("All").equals(deviceGroup.manufacturer) && !("All").equals(deviceGroup.modelName) && deviceGroup.firmwareVersion == null) {
                whereExp += " (manufacturer='" + deviceGroup.manufacturer + "' and";
                whereExp += " model_name='" + deviceGroup.modelName + "') or";
            }
            if (!("All").equals(deviceGroup.manufacturer) && !("All").equals(deviceGroup.modelName) && deviceGroup.firmwareVersion != null) {
                whereExp += " (manufacturer='" + deviceGroup.manufacturer + "' and";
                whereExp += " model_name='" + deviceGroup.modelName + "' and";
                whereExp += " firmware_version='" + deviceGroup.firmwareVersion + "') or";
            }
        }
        if (!isAll) {
            if (whereExp.length() > 2) {
                whereExp = whereExp.substring(0, whereExp.length() - 2);
                deviceTypeVersions = this.repository.search(whereExp);
            } else {
                deviceTypeVersions = this.repository.findAll();
            }
        }
        return deviceTypeVersions;
    }

    public List<DeviceTypeVersion> getListByDeviceGroupId(Long deviceGroupId) {

        DeviceGroup deviceGroup = deviceGroupService.get(deviceGroupId);

        List<DeviceTypeVersion> deviceTypeVersions = new ArrayList<>();
        String manufacturer = "All".equals(deviceGroup.manufacturer) || "".equals(deviceGroup.manufacturer) ? null : deviceGroup.manufacturer;
        String modelName = "All".equals(deviceGroup.modelName) || "".equals(deviceGroup.modelName) ? null : deviceGroup.modelName;
        String firmwareVersion = "All".equals(deviceGroup.firmwareVersion) || "".equals(deviceGroup.firmwareVersion) ? null : deviceGroup.firmwareVersion;
        String whereExp = "";

        if (manufacturer != null && modelName != null && firmwareVersion != null) {
            whereExp = "manufacturer=? AND model_name=? AND firmware_version=?";
            deviceTypeVersions = this.repository.search(whereExp, manufacturer, modelName, firmwareVersion);
        } else if (manufacturer != null && modelName != null) {
            whereExp = "manufacturer=? AND model_name=?";
            deviceTypeVersions = this.repository.search(whereExp, manufacturer, modelName);
        } else if (manufacturer != null) {
            whereExp = "manufacturer=?";
            deviceTypeVersions = this.repository.search(whereExp, manufacturer);
        } else {
            whereExp = "1=1";
            deviceTypeVersions = this.repository.search(whereExp);
        }

        return deviceTypeVersions;
    }

    public String findByOUIAndProductClass(String oui, String productClass, String version) {
        List<DeviceTypeVersion> deviceTypeVersions = new ArrayList<DeviceTypeVersion>();
        deviceTypeVersions = this.repository.search("oui=? and product_class=? and firmware_version=?", oui, productClass, version);
        if (deviceTypeVersions.size() > 0) {
            return deviceTypeVersions.get(0).manufacturer + "|" + deviceTypeVersions.get(0).modelName;
        }
        return "";
    }

    public List<DeviceTypeVersion> findByQuery(String query, Integer index, Integer limit) {
        return this.repository.search(query, new PageRequest(index, limit)).getContent();
    }

    public List<DeviceTypeVersion> findByQuery(String query) {
        return this.repository.search(query);
    }

    public DeviceTypeVersion copy(Long deviceTypeVersionId, String firmwareVersion) {
        DeviceTypeVersion deviceTypeVersionNew = new DeviceTypeVersion();
        try {

            // Copy device type version
            deviceTypeVersionNew = get(deviceTypeVersionId);
            deviceTypeVersionNew.id = null;
            deviceTypeVersionNew.firmwareVersion = firmwareVersion;
            deviceTypeVersionNew = create(deviceTypeVersionNew);

            if (deviceTypeVersionNew.id > 0) {
                // Copy parameter detail
                List<ParameterDetail> parameterDetails = parameterDetailService.findByDeviceTypeVersion2(deviceTypeVersionId);
                for (ParameterDetail parameterDetailNew : parameterDetails) {
                    parameterDetailNew.id = null;
                    parameterDetailNew.deviceTypeVersionId = deviceTypeVersionNew.id;
                    parameterDetailService.create(parameterDetailNew);
                }

                // Copy tag
                List<Tag> tags = tagService.findByDeviceTypeVersion(deviceTypeVersionId);
                for (Tag tagNew : tags) {
                    tagNew.id = null;
                    tagNew.deviceTypeVersionId = deviceTypeVersionNew.id;
                    tagService.create(tagNew);
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return deviceTypeVersionNew;
    }
}
