/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vn.ssdc.vnpt.selfCare.services;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import java.io.InputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import vn.ssdc.vnpt.devices.model.DeviceType;
import vn.ssdc.vnpt.devices.model.DeviceTypeVersion;
import vn.ssdc.vnpt.devices.model.Parameter;
import vn.ssdc.vnpt.devices.model.ParameterDetail;
import vn.ssdc.vnpt.devices.model.Tag;
import vn.ssdc.vnpt.devices.services.DataModelService;
import vn.ssdc.vnpt.devices.services.DeviceTypeService;
import vn.ssdc.vnpt.devices.services.DeviceTypeVersionService;
import vn.ssdc.vnpt.devices.services.ParameterDetailService;
import vn.ssdc.vnpt.devices.services.TagService;
import vn.ssdc.vnpt.devices.services.Tr069ParameterService;
import vn.ssdc.vnpt.selfCare.model.SCDataModel;
import vn.ssdc.vnpt.selfCare.model.SCDevice;
import vn.ssdc.vnpt.selfCare.model.SCDynamicProfile;
import vn.ssdc.vnpt.selfCare.model.SCParameter;
import vn.ssdc.vnpt.selfCare.model.SCProfileConfig;
import vn.ssdc.vnpt.selfCare.model.searchForm.SCDataModelSearchForm;
import vn.ssdc.vnpt.selfCare.model.searchForm.SCDeviceSearchForm;
import vn.ssdc.vnpt.selfCare.model.searchForm.SCParameterMove;
import vn.ssdc.vnpt.selfCare.model.searchForm.SCParameterSearchForm;
import vn.ssdc.vnpt.selfCare.model.searchForm.SCProfileSearchForm;
import vn.ssdc.vnpt.umpexception.DuplicationFirmwareVersionException;
import vn.ssdc.vnpt.utils.StringUtils;
import vn.vnpt.ssdc.jdbc.exceptions.EntityNotFoundException;
import vn.vnpt.ssdc.utils.ObjectUtils;

/**
 *
 * @author kiendt
 */
@Service
public class SelfCareServiceDataModel {

    @Autowired
    private DeviceTypeVersionService deviceTypeVersionService;

    @Autowired
    private SelfCareServiceDevice selfCareServiceDevice;

    @Autowired
    private DeviceTypeService deviceTypeService;

    @Autowired
    private ParameterDetailService parameterDetailService;

    @Autowired
    private DataModelService dataModelService;

    @Autowired
    private Tr069ParameterService tr069ParameterService;

    @Autowired
    private TagService tagService;

    public List<SCDataModel> searchDataModel(SCDataModelSearchForm scDataModelSearchForm) {
        List<SCDataModel> dataModels = new ArrayList<>();
        List<DeviceTypeVersion> deviceTypeVersions = doSearch(scDataModelSearchForm);
        if (!deviceTypeVersions.isEmpty()) {
            for (DeviceTypeVersion deviceTypeVersion : deviceTypeVersions) {
                dataModels.add(new SCDataModel(deviceTypeVersion));
            }
        }
        return dataModels;
    }

    public int countDataModel(SCDataModelSearchForm scDataModelSearchForm) {
        scDataModelSearchForm.limit = null;
        scDataModelSearchForm.page = null;
        List<DeviceTypeVersion> deviceTypeVersions = doSearch(scDataModelSearchForm);
        return deviceTypeVersions.isEmpty() ? 0 : deviceTypeVersions.size();
    }

    public List<SCProfileConfig> searchProfile(SCProfileSearchForm scProfileSearchForm) {
        return doSearch(scProfileSearchForm);
    }

    public int countProfile(SCProfileSearchForm scProfileSearchForm) {
        scProfileSearchForm.limit = null;
        scProfileSearchForm.page = null;
        List<SCProfileConfig> scProfiles = doSearch(scProfileSearchForm);
        return scProfiles != null && !scProfiles.isEmpty() ? scProfiles.size() : 0;
    }

    public SCProfileConfig searchParameterInProfile(Long profileId, SCParameterSearchForm scParameterSearchForm) {
        if (Strings.isNullOrEmpty(scParameterSearchForm.name)) {
            return new SCProfileConfig(tagService.get(profileId));
        }

        Tag tag = tagService.get(profileId);
        Map<String, Parameter> parameterInSearch = new HashMap<>();
        for (Map.Entry<String, Parameter> entry : tag.parameters.entrySet()) {
            if (entry.getValue().path.contains(scParameterSearchForm.name)) {
                parameterInSearch.put(entry.getKey(), entry.getValue());
            }
        }
        tag.parameters = parameterInSearch;
        return new SCProfileConfig(tag);
    }

    public List<SCProfileConfig> doSearch(SCProfileSearchForm scProfileSearchForm) {
        if (scProfileSearchForm.deviceTypeVersionId == null) {
            return null;
        }
        if (Strings.isNullOrEmpty(scProfileSearchForm.name)) {
            scProfileSearchForm.name = "";
        }
        List<Tag> tags = tagService.getPage(scProfileSearchForm.deviceTypeVersionId, scProfileSearchForm.page, scProfileSearchForm.limit, scProfileSearchForm.name, scProfileSearchForm.correspondingModule);
        return convertFromTagToSCProfile(tags);
    }

    private List<DeviceTypeVersion> doSearch(SCDataModelSearchForm scDataModelSearchForm) {
        List<DeviceTypeVersion> deviceTypeVersions = new ArrayList<>();
        Set<String> conditions = new HashSet<>();
        if (!Strings.isNullOrEmpty(scDataModelSearchForm.firmwareVersion)) {
            conditions.add(String.format("firmware_version = '%s'", scDataModelSearchForm.firmwareVersion));
        }
        if (!Strings.isNullOrEmpty(scDataModelSearchForm.manufacturer)) {
            conditions.add(String.format("manufacturer = '%s'", scDataModelSearchForm.manufacturer));
        }
        if (!Strings.isNullOrEmpty(scDataModelSearchForm.modelName)) {
            conditions.add(String.format("model_name = '%s'", scDataModelSearchForm.modelName));
        }
        if (!Strings.isNullOrEmpty(scDataModelSearchForm.oui)) {
            conditions.add(String.format("oui = '%s'", scDataModelSearchForm.oui));
        }
        if (!Strings.isNullOrEmpty(scDataModelSearchForm.productClass)) {
            conditions.add(String.format("product_class = '%s'", scDataModelSearchForm.productClass));
        }
        if (scDataModelSearchForm.createdFrom != null) {
            conditions.add(String.format("created > %s", scDataModelSearchForm.createdFrom.getTime()));
        }
        if (scDataModelSearchForm.createdTo != null) {
            conditions.add(String.format("created < %s", scDataModelSearchForm.createdTo.getTime()));
        }

        if (scDataModelSearchForm.limit != null && scDataModelSearchForm.page != null) {
            if (!conditions.isEmpty()) {
                String query = String.join(" AND ", conditions);
                deviceTypeVersions = deviceTypeVersionService.findByQuery(query, scDataModelSearchForm.page - 1, scDataModelSearchForm.limit);
            } else {
                deviceTypeVersions = deviceTypeVersionService.getPage(scDataModelSearchForm.page - 1, scDataModelSearchForm.limit).getContent();
            }
        } else {
            if (!conditions.isEmpty()) {
                String query = String.join(" AND ", conditions);
                deviceTypeVersions = deviceTypeVersionService.findByQuery(query);
            } else {
                deviceTypeVersions = deviceTypeVersionService.getAll();
            }
        }

        return deviceTypeVersions;
    }

    public boolean checkDataModelIsInUse(Long dataModelId) throws ParseException, EntityNotFoundException {
        String firmwareVersion = deviceTypeVersionService.get(dataModelId).firmwareVersion;
        SCDeviceSearchForm searchForm = new SCDeviceSearchForm();
        searchForm.firmwareVersion = firmwareVersion;
        int numberDevice = selfCareServiceDevice.countDevice(searchForm);
        if (numberDevice > 0) {
            return true;
        }
        return false;
    }

    public List<SCProfileConfig> getAllProfileByDataModelId(Long dataModelId) {
        List<Tag> tags = tagService.getListProfilesOfVersion(dataModelId);
        return convertFromTagToSCProfile(tags);
    }

    private List<SCProfileConfig> convertFromTagToSCProfile(List<Tag> tags) {
        if (tags.isEmpty()) {
            return null;
        }
        List<SCProfileConfig> scTags = new ArrayList<>();
        for (Tag tag : tags) {
            scTags.add(new SCProfileConfig(tag));
        }
        return scTags;
    }

    public Tag convertFromSCTagtoTag(SCProfileConfig scTag) {
        Tag tag = new Tag();
        tag.id = scTag.id;
        tag.assignedGroup = "PROFILE";
        tag.assigned = 0;
        tag.correspondingModule = scTag.correspondingModule != null ? scTag.correspondingModule : new HashSet<>();
        tag.deviceTypeVersionId = scTag.deviceTypeVersionId;
        tag.name = scTag.name;
        tag.parameters = scTag.parameters != null ? scTag.parameters : new HashMap<>();
//        tag.profileSetting = new Gson().toJson(scTag.scSettingProfile);
//        tag.subProfileSetting = new Gson().toJson(scTag.scDynamicProfiles);
        return tag;
    }

    private Map<String, ParameterDetail> parseParameterFromFile(Document document, Long deviceTypeVersionId) throws DocumentException {
        Map<String, ParameterDetail> mapParameters = new HashMap<>();
        List<Node> objects = document.selectNodes("//model/object");
        for (Node object : objects) {
            ParameterDetail objectParam = new ParameterDetail();
            objectParam.defaultValue = "";
            objectParam.rule = "";
            objectParam.path = object.valueOf("@name");
            objectParam.tr069Name = tr069ParameterService.convertToTr069Param(object.valueOf("@name"));
            objectParam.dataType = "object";
            objectParam.setShortName();
            objectParam.setParentObject();
            mapParameters.put(objectParam.path, objectParam);
            List<Node> listParameterNode = object.selectNodes("parameter");
            for (Node childNode : listParameterNode) {
                ParameterDetail parameterDetail = new ParameterDetail();
                parameterDetail.access = childNode.valueOf("@access");
                parameterDetail.deviceTypeVersionId = deviceTypeVersionId;
                String path = object.valueOf("@name") + childNode.valueOf("@name");
                parameterDetail.path = path;
                parameterDetail.tr069Name = tr069ParameterService.convertToTr069Param(path);
                parameterDetail.defaultValue = "";

                String[] strReturn = tr069ParameterService.parseDataType(null, childNode).split("@@@");
                parameterDetail.dataType = strReturn[0];

                parameterDetail.setShortName();
                parameterDetail.setParentObject();
                parameterDetail.tr069ParentObject = tr069ParameterService.convertToTr069Param(parameterDetail.parentObject);
                parameterDetail.instance = parameterDetail.tr069Name.lastIndexOf(".{i}.") > -1 && parameterDetail.tr069Name.lastIndexOf(".{i}.") + ".{i}.".length() == parameterDetail.tr069Name.length();
                mapParameters.put(parameterDetail.path, parameterDetail);
            }
        }

        return mapParameters;
    }

    private boolean checkTagExist(List<Tag> tags, String name) {
        for (Tag tag : tags) {
            if (tag.name.contains(name)) {
                return true;
            }
        }
        return false;
    }

    public SCDataModel importDataModel(InputStream file, String oui, String manufacture, String productClass, String firmwareVersion, String modelName) throws DocumentException {
        SAXReader reader = new SAXReader();
        Document document = reader.read(file);

        DeviceTypeVersion deviceTypeVersion = new DeviceTypeVersion();
        DeviceType currenDeviceType = deviceTypeService.findByPk(oui, productClass);
        Map<String, ParameterDetail> mapParam;
        if (currenDeviceType == null) {
            // new deviceType
            DeviceType deviceType = new DeviceType();
            deviceType.manufacturer = manufacture;
            deviceType.oui = oui;
            deviceType.productClass = productClass;
            deviceType.name = productClass + "_" + manufacture + "_" + oui;
            deviceType.modelName = modelName;
            currenDeviceType = deviceTypeService.create(deviceType);
        }

        if (deviceTypeVersionService.findByPk(currenDeviceType.id, firmwareVersion) != null) {
            throw new DuplicationFirmwareVersionException("firmwareVersion " + firmwareVersion + " existed!!");
        }
        deviceTypeVersion.id = null;
        deviceTypeVersion.deviceTypeId = currenDeviceType.id;
        deviceTypeVersion.firmwareVersion = firmwareVersion;
        deviceTypeVersion.modelName = modelName;
        deviceTypeVersion.productClass = productClass;
        deviceTypeVersion.oui = oui;
        deviceTypeVersion.manufacturer = manufacture;
        DeviceTypeVersion currentDeviceTypeVersion = new DeviceTypeVersion();
        currentDeviceTypeVersion = deviceTypeVersionService.create(deviceTypeVersion);

        mapParam = parseParameterFromFile(document, currentDeviceTypeVersion.id);
        currentDeviceTypeVersion.parameters = parameterDetailService.convertToMapParameter(mapParam);

        Map<String, Tag> mapDiagnostic = new HashMap<>();
        if (!deviceTypeVersionService.checkExistSimilarDeviceTypeVersion(currenDeviceType.id, currentDeviceTypeVersion.id)) {
            mapDiagnostic = dataModelService.createProfile(currentDeviceTypeVersion, mapParam);
        } else {
            DeviceTypeVersion similarDeviceTypeVersion = dataModelService.getSimilarDeviceTypeVersion(currenDeviceType, mapParam);
            mapDiagnostic = dataModelService.createProfileFromSimilarDeviceTypeVersion(currentDeviceTypeVersion, mapParam, similarDeviceTypeVersion);
        }

        currentDeviceTypeVersion.diagnostics = mapDiagnostic;
        deviceTypeVersionService.update(currentDeviceTypeVersion.id, currentDeviceTypeVersion);
        dataModelService.createDataModel(currentDeviceTypeVersion, mapParam);
        // import rieng profile
        importProfile(document, mapParam, currentDeviceTypeVersion.id);
        return null;
    }

    private void importProfile(Document document, Map<String, ParameterDetail> mapParam, Long deviceTypeVersionId) throws DocumentException {
        List<Tag> tags = tagService.getListProfilesOfVersion(deviceTypeVersionId);
        List<Node> objectProfiles = document.selectNodes("//model/profile");
        for (Node node : objectProfiles) {
            if (!checkTagExist(tags, node.valueOf("@name"))) {
                // create profile
                Tag tag = new Tag();
                tag.assignedGroup = "PROFILE";
                tag.assigned = 0;
                tag.deviceTypeVersionId = deviceTypeVersionId;
                tag.name = node.valueOf("@name");
                Map<String, Parameter> mapParameter = new HashMap<>();
                List<Node> objectNodes = node.selectNodes("object");
                for (Node objectNode : objectNodes) {
                    String parent = objectNode.valueOf("@ref");
                    List<Node> parameterNodes = objectNode.selectNodes("parameter");
                    for (Node paramterNode : parameterNodes) {
                        ParameterDetail parameterDetail = parameterDetailService.getByTr069Name(tr069ParameterService.convertToTr069Param(parent + paramterNode.valueOf("@ref")), deviceTypeVersionId);
                        mapParameter.put(parameterDetail.path, parameterDetailService.convertToParameter(parameterDetail));
                    }
                }
                tag.parameters = mapParameter;
                tagService.create(tag);
                // lay ra het dong parameter
                // tim lai thong tin cua parameter trong bang parameter_detail
                // add vao map
            }
        }
    }

    public SCProfileConfig udpateProfile(Long profileId, SCProfileConfig scTag) {
        Tag tag = tagService.get(profileId);
        if (!Strings.isNullOrEmpty(scTag.assignedGroup)) {
            tag.assignedGroup = scTag.assignedGroup;
        }
        if (!Strings.isNullOrEmpty(scTag.name)) {
            tag.name = scTag.name;
        }
//        if (scTag.scDynamicProfiles != null) {
//            tag.profileSetting = new Gson().toJson(scTag.scSettingProfile);
//        }
//        if (scTag.scDynamicProfiles != null) {
//            tag.subProfileSetting = new Gson().toJson(scTag.scDynamicProfiles);
//        }

//        if (!Strings.isNullOrEmpty(scTag.profileSetting)) {
//            tag.profileSetting = scTag.profileSetting;
//        }
//        if (!Strings.isNullOrEmpty(scTag.subProfileSetting)) {
//            tag.subProfileSetting = scTag.subProfileSetting;
//        }
        if (scTag.assigned != null) {
            tag.assigned = scTag.assigned;
        }
        if (scTag.correspondingModule != null && !scTag.correspondingModule.isEmpty()) {
            tag.correspondingModule = scTag.correspondingModule;

        }
        if (scTag.parameters != null && !scTag.parameters.isEmpty()) {
            tag.parameters = scTag.parameters;
        }
        if (scTag.deviceTypeVersionId != null) {
            tag.deviceTypeVersionId = scTag.deviceTypeVersionId;
        }
        return new SCProfileConfig(tagService.update(profileId, tag));
    }

    /**
     * Cac ham cho giao tiep voi parameter trong profile
     */
    public SCProfileConfig addParameterToProfile(Long dataModelId, Long profileId, Set<SCParameter> scParamters) {
        List<ParameterDetail> parameterDetails = parameterDetailService.findByDeviceTypeVersion2(dataModelId);

        Map<String, ParameterDetail> parameterDetailsMap = new HashMap<String, ParameterDetail>();
        for (ParameterDetail pd : parameterDetails) {
            parameterDetailsMap.put(pd.path, pd);
        }
        Tag tag = tagService.get(profileId);
        DeviceTypeVersion deviceTypeVersion = deviceTypeVersionService.get(dataModelId);
        for (SCParameter tmp : scParamters) {
            if (!ObjectUtils.empty(tmp) && parameterDetailsMap.containsKey(tmp.path)) {
                ParameterDetail parameterDetail = parameterDetailsMap.get(tmp.path);
                // Add parameter to profile
                Parameter parameter = new Parameter();
                parameter.path = parameterDetail.path;
                parameter.shortName = parameterDetail.shortName;
                parameter.dataType = parameterDetail.dataType;
                parameter.defaultValue = parameterDetail.defaultValue;
                parameter.rule = parameterDetail.rule;
                parameter.tr069Name = parameterDetail.tr069Name;
                parameter.tr069ParentObject = parameterDetail.tr069ParentObject;
                parameter.access = parameterDetail.access;
                tag.parameters.put(tmp.path, parameter);
                // Update device_type_version
                if (ObjectUtils.empty(deviceTypeVersion.parameters.get(parameter.path))) {
                    deviceTypeVersion.parameters.put(parameter.path, parameter);
                }
                // Update parameter_details
                parameterDetail.profile.add(String.valueOf(profileId));
                parameterDetailService.update(parameterDetail.id, parameterDetail);
            } else if (!ObjectUtils.empty(tmp.path)) {
                String defaultValue = Strings.isNullOrEmpty(tmp.defaultValue) ? "" : tmp.defaultValue;
                String dataType = Strings.isNullOrEmpty(tmp.dataType) ? "string" : tmp.dataType;
                String access = Strings.isNullOrEmpty(tmp.access) ? "true" : tmp.access;
                ParameterDetail parameterDetailNew = parameterDetailService.createNewParameter(dataModelId, profileId, tmp.path, defaultValue, dataType, access);
                tag.parameters.put(tmp.path, parameterDetailNew.toParameter());
                deviceTypeVersion.parameters.put(tmp.path, parameterDetailNew.toParameter());
            }
        }
        tagService.update(profileId, tag);
        deviceTypeVersionService.update(dataModelId, deviceTypeVersion);

        return new SCProfileConfig(tagService.get(profileId));
    }

    public void deleteParameterToProfile(Long dataModelId, Long profileId, String path) {
        Tag tag = tagService.get(profileId);
        Tag tagOthers = tagService.getProfileOthers(dataModelId);

        if (!ObjectUtils.empty(path) && !ObjectUtils.empty(tag.parameters.get(path))) {
            Parameter parameter = tag.parameters.get(path);
            Parameter parameterMove = new Parameter();
            parameterMove.path = parameter.path;
            parameterMove.shortName = parameter.shortName;
            parameterMove.dataType = parameter.dataType;
            parameterMove.value = parameter.value;
            parameterMove.defaultValue = parameter.defaultValue;
            parameterMove.rule = parameter.rule;
            parameterMove.inputType = parameter.inputType;
            parameterMove.useSubscriberData = parameter.useSubscriberData;
            parameterMove.access = parameter.access;
            parameterMove.tr069Name = parameter.tr069Name;
            parameterMove.tr069ParentObject = parameter.tr069ParentObject;
            tagOthers.parameters.put(path, parameterMove);
            tag.parameters.remove(path);
        }
        tagService.update(profileId, tag);
        tagService.update(tagOthers.id, tagOthers);

        // delete in parameter_detail
        ParameterDetail parameterDetail = parameterDetailService.findByParams(path, dataModelId);
        if (parameterDetail != null) {
            parameterDetailService.delete(parameterDetail.id);
        }
    }

    public SCProfileConfig updateParameterInProfile(Long dataModelId, Long profileId, SCParameter scParamters) {
        List<ParameterDetail> parameterDetails = parameterDetailService.findByDeviceTypeVersion2(dataModelId);

        if (!Strings.isNullOrEmpty(scParamters.path)) {
            String value = scParamters.value;
            // Convert data if data type is date time
            String dataType = Strings.isNullOrEmpty(scParamters.dataType) ? "" : scParamters.dataType;

            if ("dateTime".equals(dataType)) {
                value = StringUtils.toZoneDateTime(value);
            }

            // Update parameter_details
            for (ParameterDetail parameterDetail : parameterDetails) {
                if (scParamters.path.equals(parameterDetail.path)) {

                    Set<String> profiles = parameterDetail.profile;
                    for (String tmp : profiles) {
                        Tag tag = tagService.get(Long.valueOf(tmp));
                        if (!ObjectUtils.empty(tag.parameters.get(scParamters.path))) {
                            tag.parameters.get(scParamters.path).value = value;
                            tagService.update(profileId, tag);
                        }
                    }

//                    parameterDetail.profile = new HashSet<>();
//                    parameterDetail.profile.add(String.valueOf(profileId));
                    parameterDetailService.update(parameterDetail.id, parameterDetail);
                    break;
                }
            }
            // Update device_type_version
            DeviceTypeVersion deviceTypeVersion = deviceTypeVersionService.get(dataModelId);
            if (!ObjectUtils.empty(deviceTypeVersion.parameters.get(scParamters.path))) {
                deviceTypeVersion.parameters.get(scParamters.path).value = value;
                deviceTypeVersionService.update(dataModelId, deviceTypeVersion);
            }

            // Update tags
            // update in other tag contains this paramter 
        }
        return new SCProfileConfig(tagService.get(profileId));
    }

    public SCProfileConfig moveParameter(SCParameterMove scParameterMove) {
        Tag tag = tagService.get(scParameterMove.sourceProfileId);
        Tag tagMove = tagService.get(scParameterMove.desProfileId);
        for (String path : scParameterMove.scParameters) {
            if (!ObjectUtils.empty(path) && !ObjectUtils.empty(tag.parameters.get(path))) {

                Parameter parameter = tag.parameters.get(path);
                Parameter parameterMove = new Parameter();
                parameterMove.path = parameter.path;
                parameterMove.shortName = parameter.shortName;
                parameterMove.dataType = parameter.dataType;
                parameterMove.value = parameter.value;
                parameterMove.defaultValue = parameter.defaultValue;
                parameterMove.rule = parameter.rule;
                parameterMove.inputType = parameter.inputType;
                parameterMove.useSubscriberData = parameter.useSubscriberData;
                parameterMove.access = parameter.access;
                parameterMove.tr069Name = parameter.tr069Name;
                parameterMove.tr069ParentObject = parameter.tr069ParentObject;
                tagMove.parameters.put(path, parameterMove);
                tag.parameters.remove(path);
            }
        }
        tagService.update(scParameterMove.sourceProfileId, tag);
        tagService.update(scParameterMove.desProfileId, tagMove);
        return new SCProfileConfig(tagService.get(scParameterMove.sourceProfileId));
    }

    /**
     * Cac ham cho giao tiep voi dynamic configuration
     */
    public Set<SCDynamicProfile> getDynamicConfigOfProfile(Long profileId) {
        return null;
    }

    public void deleteParameterInDatamodel(Long dataModelId, String path) {
        // delete in parameter detail
        parameterDetailService.deleteSingleParamerter(dataModelId, path);
        // delete in deviceTypeVersion
        DeviceTypeVersion dataModel = deviceTypeVersionService.get(dataModelId);
        Map<String, Parameter> parametesAfterDelete = new HashMap<>();
        for (Map.Entry<String, Parameter> param : dataModel.parameters.entrySet()) {
            if (!param.getKey().equals(path)) {
                parametesAfterDelete.put(param.getKey(), param.getValue());
            }
        }
        dataModel.parameters = parametesAfterDelete;
        deviceTypeVersionService.update(dataModelId, dataModel);
        // delete parameter in Tag
        List<Tag> tags = tagService.getListProfilesOfVersion(dataModelId);
        for (Tag tag : tags) {
            Map<String, Parameter> tagAfterDeleted = new HashMap<>();
            for (Map.Entry<String, Parameter> entry : tag.parameters.entrySet()) {
                if (!entry.getKey().equals(path)) {
                    tagAfterDeleted.put(entry.getKey(), entry.getValue());
                }
            }
            tag.parameters = tagAfterDeleted;
            tagService.update(tag.id, tag);
        }

    }

    public List<SCProfileConfig> getProfilesContainParameter(Long dataModelId, String path) {
        List<Tag> tags = tagService.getListProfilesOfVersion(dataModelId);
        List<SCProfileConfig> scProfileConfigs = new ArrayList<>();
        for (Tag tag : tags) {
            for (Map.Entry<String, Parameter> entry : tag.parameters.entrySet()) {
                if (entry.getKey().contains(path)) {
                    scProfileConfigs.add(new SCProfileConfig(tag));
                }
            }
        }
        return scProfileConfigs;
    }

    public SCDataModel searchParameterInDataModel(Long dataModelId, SCParameterSearchForm form) {
        SCDataModel dataModel = new SCDataModel(deviceTypeVersionService.get(dataModelId));
        Map<String, Parameter> mapResult = new HashMap<>();
        for (Map.Entry<String, Parameter> entry : dataModel.parameters.entrySet()) {
            if (entry.getKey().contains(form.name)) {
                mapResult.put(entry.getKey(), entry.getValue());
            }
        }
        dataModel.parameters = mapResult;
        return dataModel;
    }

    public SCProfileConfig cloneProfile(SCProfileConfig scTag) {
        // create new tag
        Tag tag = tagService.get(scTag.id);
        tag.id = null;
        tag.name = scTag.name;
        SCProfileConfig profileCloned = new SCProfileConfig(tagService.create(tag));

        for (Map.Entry<String, Parameter> entry : profileCloned.parameters.entrySet()) {
            String path = entry.getKey();
            ParameterDetail parameterDetail = parameterDetailService.findByParams(path, profileCloned.deviceTypeVersionId);
            parameterDetail.profile.add(profileCloned.id.toString());
            parameterDetailService.update(parameterDetail.id, parameterDetail);
        }
        return profileCloned;
        // update data in parameter detail
    }

}
