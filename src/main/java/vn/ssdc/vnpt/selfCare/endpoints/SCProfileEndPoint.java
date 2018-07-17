/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vn.ssdc.vnpt.selfCare.endpoints;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import vn.ssdc.vnpt.devices.model.DeviceTypeVersion;
import vn.ssdc.vnpt.devices.services.DataModelService;
import vn.ssdc.vnpt.devices.services.DeviceTypeVersionService;
import vn.ssdc.vnpt.devices.services.TagService;
import vn.ssdc.vnpt.selfCare.model.SCFirmwareVersion;
import vn.ssdc.vnpt.selfCare.model.SCManufacturer;
import vn.ssdc.vnpt.selfCare.model.SCModelName;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import vn.ssdc.vnpt.devices.model.DeviceGroup;
import vn.ssdc.vnpt.devices.model.Parameter;
import vn.ssdc.vnpt.devices.model.ParameterSubProfile;
import vn.ssdc.vnpt.devices.model.Tag;
import vn.ssdc.vnpt.devices.services.DeviceGroupService;
import vn.ssdc.vnpt.devices.services.DeviceTypeService;
import vn.ssdc.vnpt.selfCare.model.SCObjectInstanceInSubProfile;
import vn.ssdc.vnpt.selfCare.model.SCProfileDisplay;
import vn.ssdc.vnpt.selfCare.model.SCSubProfile;

/**
 *
 * @author kiendt
 */
@Component
@Path("/self-care/profiles")
@Produces(APPLICATION_JSON)
@Consumes({APPLICATION_JSON, TEXT_PLAIN})
@Api("Self-Care Profiles")
public class SCProfileEndPoint {

    @Autowired
    private TagService tagService;

    @Autowired
    private DataModelService dataModelService;

    @Autowired
    private DeviceTypeVersionService deviceTypeVersionService;

    @Autowired
    private DeviceGroupService deviceGroupService;

    @Autowired
    private DeviceTypeService deviceTypeService;

    @POST
    @Path("/device/{deviceId}")
    @ApiOperation(value = "Get All Profile of device by deviceId")
    public String getListProfileByDeviceId(@PathParam("deviceId") String deviceId) {
        return tagService.getListTagByDeviceId(deviceId);
    }

    @POST
    @Path("/sub-profiles/{deviceId}/{profileId}")
    @ApiOperation(value = "Get All Sub profile setting by deviceId and profileId")
    public String getListSubProfileByDeviceIdAndTagId(@PathParam("deviceId") String deviceId,
            @PathParam("profileId") Long profileId) {
        return new Gson().toJson(tagService.parseInforToSCProfileDisplay(profileId, dataModelService.getProfileOfDevices(deviceId, profileId), "devices"));
//        return new Gson().toJson(tagService.parseInput(profileId, dataModelService.getProfileOfDevices(deviceId, profileId)));
    }

    @GET
    @Path("/group-device/{groupId}/{module}")
    @ApiOperation(value = "Get All SubProfile by GroupId", notes = "module = performance | devices | alarm | provisioning | policy")
    public String getListSubProfileByGroupId(@PathParam("groupId") Long groupId, @PathParam("module") String module) {
        DeviceGroup selectedDeviceGroup = deviceGroupService.get(groupId);
        // neu group chi chua 1 firmwareversion
        if (!Strings.isNullOrEmpty(selectedDeviceGroup.firmwareVersion) && !"All".equals(selectedDeviceGroup.firmwareVersion)) {
            DeviceTypeVersion deviceTypeVersion = deviceTypeVersionService.findByFirmwareVersion(selectedDeviceGroup.firmwareVersion);
            List<DeviceTypeVersion> deviceTypeVersions = new ArrayList<>();
            deviceTypeVersions.add(deviceTypeVersion);

            List<SCProfileDisplay> sCProfileConfigs = getScProfileConfigs(deviceTypeVersions, module);
            return new Gson().toJson(sCProfileConfigs);
        } else {
            // neu group la 1 tap cac firmware version
            List<DeviceTypeVersion> deviceTypeVersions = deviceTypeVersionService.findByListDeviceGroup(String.valueOf(selectedDeviceGroup.id));
            List<SCProfileDisplay> sCProfileConfigs = getScProfileConfigs(deviceTypeVersions, module);
            return new Gson().toJson(sCProfileConfigs);

        }
    }

    @GET
    @ApiOperation(value = "Get data model tree")
    @Path("/getDataModelTrees")
    public List<SCManufacturer> getDataModelTrees() {
        List<DeviceTypeVersion> listDeviceTypeVersion = deviceTypeVersionService.getAll();
        List<SCManufacturer> listSCManufacturers = new ArrayList<>();
        for (DeviceTypeVersion deviceTypeVersion : listDeviceTypeVersion) {
            boolean checkExistedManufacturer = false;
            for (int i = 0; i < listSCManufacturers.size(); i++) {
                SCManufacturer scManufacturer = listSCManufacturers.get(i);
                if (scManufacturer.manufacturer.equals(deviceTypeVersion.manufacturer)) {
                    boolean checkExistedModelName = false;
                    for (int j = 0; j < scManufacturer.listModelNames.size(); j++) {
                        SCModelName scModelName = scManufacturer.listModelNames.get(j);
                        if (scModelName.modelName.equals(deviceTypeVersion.modelName) && scModelName.productClass.equals(deviceTypeVersion.productClass)) {
                            boolean checkExistedFirmwareVersion = false;
                            for (SCFirmwareVersion scFirmwareVersion : scModelName.listFirmwareVersions) {
                                if (scFirmwareVersion.firmwareVersion.equals(deviceTypeVersion.firmwareVersion)) {
                                    checkExistedFirmwareVersion = true;
                                }
                            }
                            if (!checkExistedFirmwareVersion) {
                                SCFirmwareVersion scFirmwareVersion = new SCFirmwareVersion();
                                scFirmwareVersion.oui = deviceTypeVersion.oui;
                                scFirmwareVersion.manufacturer = deviceTypeVersion.manufacturer;
                                scFirmwareVersion.productClass = deviceTypeVersion.productClass;
                                scFirmwareVersion.modelName = deviceTypeVersion.modelName;
                                scFirmwareVersion.firmwareVersion = deviceTypeVersion.firmwareVersion;

                                listSCManufacturers.get(i).listModelNames.get(j).listFirmwareVersions.add(scFirmwareVersion);
                            }
                            checkExistedModelName = true;
                        }
                    }
                    if (!checkExistedModelName) {
                        SCFirmwareVersion scFirmwareVersion = new SCFirmwareVersion();
                        scFirmwareVersion.oui = deviceTypeVersion.oui;
                        scFirmwareVersion.manufacturer = deviceTypeVersion.manufacturer;
                        scFirmwareVersion.productClass = deviceTypeVersion.productClass;
                        scFirmwareVersion.modelName = deviceTypeVersion.modelName;
                        scFirmwareVersion.firmwareVersion = deviceTypeVersion.firmwareVersion;

                        SCModelName scModelName = new SCModelName();
                        scModelName.oui = deviceTypeVersion.oui;
                        scModelName.manufacturer = deviceTypeVersion.manufacturer;
                        scModelName.productClass = deviceTypeVersion.productClass;
                        scModelName.modelName = deviceTypeVersion.modelName;
                        scModelName.listFirmwareVersions = new ArrayList<>();
                        scModelName.listFirmwareVersions.add(scFirmwareVersion);

                        listSCManufacturers.get(i).listModelNames.add(scModelName);
                    }
                    checkExistedManufacturer = true;
                }
            }
            if (!checkExistedManufacturer) {
                SCFirmwareVersion scFirmwareVersion = new SCFirmwareVersion();
                scFirmwareVersion.oui = deviceTypeVersion.oui;
                scFirmwareVersion.manufacturer = deviceTypeVersion.manufacturer;
                scFirmwareVersion.productClass = deviceTypeVersion.productClass;
                scFirmwareVersion.modelName = deviceTypeVersion.modelName;
                scFirmwareVersion.firmwareVersion = deviceTypeVersion.firmwareVersion;

                SCModelName scModelName = new SCModelName();
                scModelName.oui = deviceTypeVersion.oui;
                scModelName.manufacturer = deviceTypeVersion.manufacturer;
                scModelName.productClass = deviceTypeVersion.productClass;
                scModelName.modelName = deviceTypeVersion.modelName;
                scModelName.listFirmwareVersions = new ArrayList<>();
                scModelName.listFirmwareVersions.add(scFirmwareVersion);

                SCManufacturer scManufacturer = new SCManufacturer();
                scManufacturer.manufacturer = deviceTypeVersion.manufacturer;
                scManufacturer.oui = deviceTypeVersion.oui;
                scManufacturer.listModelNames = new ArrayList<>();
                scManufacturer.listModelNames.add(scModelName);

                listSCManufacturers.add(scManufacturer);
            }
        }
        return listSCManufacturers;
    }

    public List<SCProfileDisplay> getScProfileConfigs(List<DeviceTypeVersion> deviceTypeVersions, String module) {
        List<SCProfileDisplay> sCProfileConfigs = new ArrayList<>();
        // lay ra danh sach toan bo cac subprofile cua devicegroup  , voi module policy, loai bo cac subprofile chua nhieu hon 1 {i}
        for (DeviceTypeVersion deviceTypeVersionTmp : deviceTypeVersions) {
            Set<Parameter> parameters = new LinkedHashSet<Parameter>();
            for (Map.Entry<String, Parameter> parameterEntry : deviceTypeVersionTmp.parameters.entrySet()) {
                parameters.add(parameterEntry.getValue());
            }
            List<Tag> tags = tagService.getListByCorrespondingModule(deviceTypeVersionTmp.id, module);
            if (module.equals("policy")) {
                for (Tag tagTmp : tags) {
                    SCProfileDisplay sCProfileConfig = tagService.parseInforToSCProfileDisplayForPolicy(tagTmp.id, parameters);
                    sCProfileConfigs.add(sCProfileConfig);
                }
            } else {
                for (Tag tagTmp : tags) {
                    SCProfileDisplay sCProfileConfig = tagService.parseInforToSCProfileDisplay(tagTmp.id, parameters, module);
                    sCProfileConfigs.add(sCProfileConfig);
                }
            }
        }

        if (module.equals("policy")) {
            Map<String, Parameter> duplicateDataParameter = new HashMap<>();
            if (deviceTypeVersions.size() == 1) {
                duplicateDataParameter = deviceTypeVersions.get(0).parameters;
            } else {
                duplicateDataParameter = tagService.getDuplicateInDeviceTypeVersions(deviceTypeVersions);
            }

            // giu lai cac tham so dung chung giua cac subprofile
            for (SCProfileDisplay sCProfileConfigTmp : sCProfileConfigs) {
                Set<SCSubProfile> sCSubProfilesInstance = sCProfileConfigTmp.list_sub_not_instance;
                if (!sCSubProfilesInstance.isEmpty()) {
                    for (SCSubProfile scSubProfileTmp : sCSubProfilesInstance) {
                        Iterator<ParameterSubProfile> iterator = scSubProfileTmp.parameters.iterator();
                        while (iterator.hasNext()) {
                            ParameterSubProfile element = iterator.next();
                            if (!tagService.checkParameterExistedInListDeviceTypeVersios(duplicateDataParameter, element.tr069Name)) {
                                iterator.remove();
                            }
                        }
                    }
                }

                Set<SCSubProfile> sCSubProfilesNotInstance = sCProfileConfigTmp.list_sub_instance;
                if (!sCSubProfilesNotInstance.isEmpty()) {
                    for (SCSubProfile scSubProfileTmp : sCSubProfilesNotInstance) {
                        if (scSubProfileTmp.list_parameters_in_sub_profile != null) {
                            Iterator<ParameterSubProfile> iterator = scSubProfileTmp.list_parameters_in_sub_profile.iterator();
                            while (iterator.hasNext()) {
                                ParameterSubProfile element = iterator.next();
                                if (!tagService.checkParameterExistedInListDeviceTypeVersios(duplicateDataParameter, element.tr069Name)) {
                                    iterator.remove();
                                }
                            }
                        }
                    }
                }
            }

            // loai bo cac profile khong chua subprofile
            for (SCProfileDisplay scProfileConfig : sCProfileConfigs) {

                if (!scProfileConfig.list_sub_not_instance.isEmpty()) {
                    Set<SCSubProfile> cayvai = new HashSet<>();
                    Iterator<SCSubProfile> iter2 = scProfileConfig.list_sub_not_instance.iterator();
                    while (iter2.hasNext()) {
                        SCSubProfile tmp = iter2.next();
                        if (tmp.parameters != null && !tmp.parameters.isEmpty()) {
                            cayvai.add(tmp);
                        }

                    }
                    scProfileConfig.list_sub_not_instance = cayvai;
                }

                if (!scProfileConfig.list_sub_instance.isEmpty()) {
                    Set<SCSubProfile> cayvai = new HashSet<>();
                    Iterator<SCSubProfile> iter1 = scProfileConfig.list_sub_instance.iterator();
                    while (iter1.hasNext()) {
                        SCSubProfile tmp = iter1.next();
                        // if module la policy thi dung list_parameters_in_sub_profile con khong thi dung list_sub_object
                        if (tmp.list_parameters_in_sub_profile != null && !tmp.list_parameters_in_sub_profile.isEmpty()) {
                            cayvai.add(tmp);
                        } else if (tmp.list_sub_object != null && !tmp.list_sub_object.isEmpty()) {
                            cayvai.add(tmp);
                        }
                    }
                    scProfileConfig.list_sub_instance = cayvai;
                }
            }
            // loai bo cac subprofile co trung param trong tung profile
            List<SCProfileDisplay> list1 = new ArrayList<>();
            for (SCProfileDisplay scProfileConfig : sCProfileConfigs) {
                list1.add(tagService.removeDuplicateSubInProfile(scProfileConfig));
            }

            // loai bo cac subprofile co trung param giua cac profile
            for (int i = 0; i < sCProfileConfigs.size() - 1; i++) {
                for (int j = i + 1; j < sCProfileConfigs.size(); j++) {
                    if (sCProfileConfigs.get(i).list_sub_instance != null) {
                        tagService.compareSCSubProfile(sCProfileConfigs.get(i).list_sub_instance, sCProfileConfigs.get(j).list_sub_instance);
                    }
                    if (sCProfileConfigs.get(i).list_sub_not_instance != null) {
                        tagService.compareSCSubProfile(sCProfileConfigs.get(i).list_sub_not_instance, sCProfileConfigs.get(j).list_sub_not_instance);
                    }
                }
            }
            List<SCProfileDisplay> finalList = new ArrayList<>();
            for (SCProfileDisplay scProfileConfig : sCProfileConfigs) {
                if (!scProfileConfig.list_sub_not_instance.isEmpty() || !scProfileConfig.list_sub_instance.isEmpty()) {
                    finalList.add(scProfileConfig);
                }
            }

//        Iterator<SCProfileDisplay> iter1 = sCProfileConfigs.iterator();
//        while (iter1.hasNext()) {
//            SCProfileDisplay el1 = iter1.next();
//            Iterator<SCProfileDisplay> iter2 = sCProfileConfigs.iterator();
//            while (iter2.hasNext()) {
//                SCProfileDisplay el2 = iter2.next();
//                if (el1.list_sub_instance != null) {
//                    el1.list_sub_instance = tagService.compareSCSubProfile(el1.list_sub_instance, el2.list_sub_instance);
//                }
//                if (el1.list_sub_not_instance != null) {
//                    el1.list_sub_not_instance = tagService.compareSCSubProfile(el1.list_sub_not_instance, el2.list_sub_not_instance);
//                }
//            }
//            profileRemoveDuplicate.add(el1);
//        }
            //     loai bo cac subprofile bij duplicate param
//            List<SCProfileDisplay> finalList = new ArrayList<>();
//            if (!profileRemoveDuplicate.isEmpty()) {
//                finalList.add(profileRemoveDuplicate.remove(0));
//            }
//            for (SCProfileDisplay scProfile : profileRemoveDuplicate) {
//
//                if (tagService.checkDuplicateSubProfileInSub(finalList, scProfile.list_sub_instance)) {
//                    finalList.add(scProfile);
//                }
//                if (tagService.checkDuplicaterSubProfileInNotSub(finalList, scProfile.list_sub_not_instance)) {
//                    finalList.add(scProfile);
//                }
//
//            }
            return finalList;
        } else {
            return sCProfileConfigs;
        }

    }

}
