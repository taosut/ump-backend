/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vn.ssdc.vnpt.selfCare.endpoints;

import com.google.gson.Gson;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import java.io.File;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import javax.ws.rs.core.Response;
import org.dom4j.DocumentException;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import vn.ssdc.vnpt.devices.model.Tag;
import vn.ssdc.vnpt.devices.services.DataModelService;
import vn.ssdc.vnpt.devices.services.DeviceTypeVersionService;
import vn.ssdc.vnpt.devices.services.TagService;
import vn.ssdc.vnpt.selfCare.model.SCDataModel;
import vn.ssdc.vnpt.selfCare.model.SCDevice;
import vn.ssdc.vnpt.selfCare.model.SCDynamicParameter;
import vn.ssdc.vnpt.selfCare.model.SCDynamicProfile;
import vn.ssdc.vnpt.selfCare.model.SCParameter;
import vn.ssdc.vnpt.selfCare.model.SCProfileConfig;
import vn.ssdc.vnpt.selfCare.model.SCSettingProfile;
import vn.ssdc.vnpt.selfCare.model.searchForm.SCDataModelSearchForm;
import vn.ssdc.vnpt.selfCare.model.searchForm.SCParameterMove;
import vn.ssdc.vnpt.selfCare.model.searchForm.SCParameterSearchForm;
import vn.ssdc.vnpt.selfCare.model.searchForm.SCProfileSearchForm;
import vn.ssdc.vnpt.selfCare.services.SelfCareServiceDataModel;
import vn.ssdc.vnpt.umpexception.UmpNbiException;
import vn.vnpt.ssdc.jdbc.exceptions.EntityNotFoundException;

/**
 *
 * @author Admin
 */
@Component
@Path("/self-care/data-models")
@Produces(APPLICATION_JSON)
@Consumes({APPLICATION_JSON, TEXT_PLAIN})
@Api("Self-Care Data Model")
public class SCDataModelEndpoint {

    @Autowired
    private SelfCareServiceDataModel selfCareServiceDataModel;

    @Autowired
    private DeviceTypeVersionService deviceTypeVersionService;

    @Autowired
    private DataModelService dataModelService;

    @Autowired
    private TagService tagService;

    @POST
    @Path("/search")
    @ApiOperation(value = "Get datamodels from datamodel Searchform")
    @ApiResponse(code = 200, message = "Success", response = SCDataModel.class)
    public List<SCDataModel> search(@RequestBody SCDataModelSearchForm scDataModelSearchForm) {
        return selfCareServiceDataModel.searchDataModel(scDataModelSearchForm);
    }

    @POST
    @Path("/count")
    @ApiOperation(value = "Count datamodels from datamodel Searchform")
    @ApiResponse(code = 200, message = "Success", response = SCDataModel.class)
    public int count(@RequestBody SCDataModelSearchForm scDataModelSearchForm) {
        return selfCareServiceDataModel.countDataModel(scDataModelSearchForm);
    }

    @GET
    @ApiOperation(value = "Read dataModel by id")
    @ApiResponse(code = 200, message = "Success", response = SCDevice.class)
    @Path("/{id}")
    public SCDataModel read(@PathParam("id") Long dataModelID) {
        return new SCDataModel(deviceTypeVersionService.get(dataModelID));
    }

    @GET
    @Path("/exportXML/{id}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @ApiOperation(value = "Get datamodels from datamodel Searchform")
    @ApiResponse(code = 200, message = "Success", response = Response.class)
    public Response exportXML(@PathParam("id") Long dataModelID) {
        String path = dataModelService.exportDataModelXML(dataModelID);
        File file = new File(path);
        Response.ResponseBuilder builder = Response.ok(file, MediaType.APPLICATION_OCTET_STREAM);
        builder.header("Content-Disposition", "attachment; filename=" + file.getName());
        Response response = builder.build();
        return response;
    }

    @GET
    @Path("/exportJSON/{id}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @ApiOperation(value = "Get datamodels from datamodel Searchform")
    @ApiResponse(code = 200, message = "Success", response = Response.class)
    public Response exportJSON(@PathParam("id") Long dataModelID) {
        String path = dataModelService.exportDataModelJson(dataModelID);
        File file = new File(path);
        Response.ResponseBuilder builder = Response.ok(file, MediaType.APPLICATION_OCTET_STREAM);
        builder.header("Content-Disposition", "attachment; filename=" + file.getName());
        Response response = builder.build();
        return response;
    }

    @DELETE
    @ApiOperation(value = "Delete dataModel by id")
    @ApiResponse(code = 200, message = "Success", response = SCDevice.class)
    @Path("/{id}")
    public void delete(@PathParam("id") Long dataModelID) throws ParseException, UmpNbiException {
        try {
            if (selfCareServiceDataModel.checkDataModelIsInUse(dataModelID)) {
                throw new UmpNbiException("error_data_model_in_use");
            }
        } catch (EntityNotFoundException e) {
            throw new UmpNbiException("error_data_model_not_found");
        }

        deviceTypeVersionService.delete(dataModelID);
    }

    @POST
    @Path("/importXML")
    @ApiOperation(value = "import datamodel")
    @ApiResponse(code = 200, message = "Success", response = SCDataModel.class)
    @Produces(APPLICATION_JSON)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public SCDataModel importDataModelXML(
            @FormDataParam("file") InputStream file,
            @FormDataParam("oui") String oui,
            @FormDataParam("productClass") String productClass,
            @FormDataParam("manufacturer") String manufacturer,
            @FormDataParam("modelName") String modelName,
            @FormDataParam("firmwareVersion") String firmwareVersion
    ) throws UmpNbiException {
        try {
            return selfCareServiceDataModel.importDataModel(file, oui, manufacturer, productClass, firmwareVersion, modelName);
        } catch (Exception e) {
            throw new UmpNbiException("error_import_data_model");
        }

    }

    @GET
    @ApiOperation(value = "Get all profile of datamodel")
    @ApiResponse(code = 200, message = "Success", response = SCProfileConfig.class)
    @Path("/{dataModelId}/profiles")
    public List<SCProfileConfig> getAllProfileOfDataModel(@PathParam("dataModelId") Long dataModelID) {
        return selfCareServiceDataModel.getAllProfileByDataModelId(dataModelID);
    }

    @DELETE
    @ApiOperation(value = "delete parameter in datamodel")
    @ApiResponse(code = 200, message = "Success", response = void.class)
    @Path("/{dataModelId}/parameters/{parameterName}")
    public void deletePrameterInDataModel(@PathParam("dataModelId") Long dataModelID, @PathParam("parameterName") String parameterName) throws UnsupportedEncodingException {
        selfCareServiceDataModel.deleteParameterInDatamodel(dataModelID, URLDecoder.decode(parameterName, "UTF-8"));
    }

    @GET
    @ApiOperation(value = "GET all profile in datamodel")
    @ApiResponse(code = 200, message = "Success", response = SCProfileConfig.class)
    @Path("/{dataModelId}/parameters/{parameterName}")
    public List<SCProfileConfig> getProfileContainParameter(@PathParam("dataModelId") Long dataModelID, @PathParam("parameterName") String parameterName) throws UnsupportedEncodingException {
        return selfCareServiceDataModel.getProfilesContainParameter(dataModelID, URLDecoder.decode(parameterName, "UTF-8"));
    }

    @POST
    @ApiOperation(value = "GET all profile in datamodel")
    @ApiResponse(code = 200, message = "Success", response = SCDataModel.class)
    @Path("/{dataModelId}/parameters/search")
    public SCDataModel searchParameterIndataModel(@PathParam("dataModelId") Long dataModelID, @RequestBody SCParameterSearchForm scParameterSearchForm) {
        return selfCareServiceDataModel.searchParameterInDataModel(dataModelID, scParameterSearchForm);
    }

    @POST
    @ApiOperation(value = "Search profile datamodel")
    @ApiResponse(code = 200, message = "Success", response = SCProfileConfig.class)
    @Path("/{dataModelId}/profiles/search")
    public List<SCProfileConfig> search(@PathParam("dataModelId") Long dataModelId, @RequestBody SCProfileSearchForm scProfileSearchForm) {
        scProfileSearchForm.deviceTypeVersionId = dataModelId;
        return selfCareServiceDataModel.searchProfile(scProfileSearchForm);
    }

    @POST
    @ApiOperation(value = "Search profile datamodel")
    @ApiResponse(code = 200, message = "Success", response = SCProfileConfig.class)
    @Path("/{dataModelId}/profiles/count")
    public int count(@PathParam("dataModelId") Long dataModelId, @RequestBody SCProfileSearchForm scProfileSearchForm) {
        scProfileSearchForm.deviceTypeVersionId = dataModelId;
        return selfCareServiceDataModel.countProfile(scProfileSearchForm);
    }

    @GET
    @ApiOperation(value = "Get infor profile")
    @ApiResponse(code = 200, message = "Success", response = SCProfileConfig.class)
    @Path("/{dataModelId}/profiles/{profileId}")
    public SCProfileConfig getProfile(@PathParam("dataModelId") Long dataModelId, @PathParam("profileId") Long profileId) {
        Tag tag = tagService.get(profileId);
        return new SCProfileConfig(tag);
    }

    @PUT
    @ApiOperation(value = "update profile")
    @ApiResponse(code = 200, message = "Success", response = SCProfileConfig.class)
    @Path("/{dataModelId}/profiles/{profileId}")
    public SCProfileConfig updateProfile(@PathParam("dataModelId") Long dataModelId, @PathParam("profileId") Long profileId, @RequestBody SCProfileConfig scTag) {
        return selfCareServiceDataModel.udpateProfile(profileId, scTag);
    }

    @DELETE
    @ApiOperation(value = "delete profile")
    @ApiResponse(code = 200, message = "Success", response = SCProfileConfig.class)
    @Path("/{dataModelId}/profiles/{profileId}")
    public void deleteProfile(@PathParam("dataModelId") Long dataModelId, @PathParam("profileId") Long profileId) {
        tagService.delete(profileId);
    }

    @POST
    @ApiOperation(value = "create profile")
    @ApiResponse(code = 200, message = "Success", response = SCProfileConfig.class)
    @Path("/{dataModelId}/profiles")
    public SCProfileConfig createProfile(@PathParam("dataModelId") Long dataModelId, @RequestBody SCProfileConfig scTag) {

        SCProfileConfig profile = new SCProfileConfig(tagService.create(selfCareServiceDataModel.convertFromSCTagtoTag(scTag)));
        tagService.editProflieAlias(profile.id, scTag.name, "combo", "0");
        return profile;
    }

    @POST
    @ApiOperation(value = "clone profile")
    @ApiResponse(code = 200, message = "Success", response = SCProfileConfig.class)
    @Path("/{dataModelId}/profiles/{profileId}/clone")
    public SCProfileConfig cloneProfile(@PathParam("dataModelId") Long dataModelId, @PathParam("profileId") Long profileId, @RequestBody SCProfileConfig scTag) {
        scTag.id = profileId;
        return selfCareServiceDataModel.cloneProfile(scTag);
    }

    @POST
    @ApiOperation(value = "search parameter in profile")
    @ApiResponse(code = 200, message = "Success", response = SCProfileConfig.class)
    @Path("/{dataModelId}/profiles/{profileId}/parameters/search")
    public SCProfileConfig searhInfoInProfile(@PathParam("dataModelId") Long dataModelId, @PathParam("profileId") Long profileId, @RequestBody SCParameterSearchForm scParameterSearchForm) {
        return selfCareServiceDataModel.searchParameterInProfile(profileId, scParameterSearchForm);
    }

    @POST
    @ApiOperation(value = "add parameter to profile")
    @ApiResponse(code = 200, message = "Success", response = SCProfileConfig.class)
    @Path("/{dataModelId}/profiles/{profileId}/parameters")
    public SCProfileConfig addparameter(@PathParam("dataModelId") Long dataModelId, @PathParam("profileId") Long profileId, @RequestBody Set<SCParameter> scPrameters) {
        return selfCareServiceDataModel.addParameterToProfile(dataModelId, profileId, scPrameters);
    }

    @PUT
    @ApiOperation(value = "update parameter in profile")
    @ApiResponse(code = 200, message = "Success", response = SCProfileConfig.class)
    @Path("/{dataModelId}/profiles/{profileId}/parameters/{path}")
    public SCProfileConfig updateParameter(@PathParam("dataModelId") Long dataModelId, @PathParam("profileId") Long profileId, @RequestBody SCParameter scPrameters) {
        return selfCareServiceDataModel.updateParameterInProfile(dataModelId, profileId, scPrameters);
    }

    @DELETE
    @ApiOperation(value = "delete parameters in profile")
    @ApiResponse(code = 200, message = "Success", response = SCProfileConfig.class)
    @Path("/{dataModelId}/profiles/{profileId}/parameters/{path}")
    public void deleteParameter(@PathParam("dataModelId") Long dataModelId, @PathParam("profileId") Long profileId, @PathParam("path") String path) {
        selfCareServiceDataModel.deleteParameterToProfile(dataModelId, profileId, path);
    }

    @POST
    @ApiOperation(value = "move parameter to another profile")
    @ApiResponse(code = 200, message = "Success", response = SCProfileConfig.class)
    @Path("/{dataModelId}/profiles/{profileId}/parameters/move")
    public SCProfileConfig moveParameter(@PathParam("dataModelId") Long dataModelId, @PathParam("profileId") Long profileId, @RequestBody SCParameterMove scPrameters) {
        return selfCareServiceDataModel.moveParameter(scPrameters);
    }

    @GET
    @ApiOperation(value = "get dynamic-config profile")
    @ApiResponse(code = 200, message = "Success", response = SCSettingProfile.class)
    @Path("/{dataModelId}/profiles/{profileId}/setting")
    public SCSettingProfile getDynamicConfigurationOfProfile(@PathParam("dataModelId") Long dataModelId, @PathParam("profileId") Long profileId, @RequestBody SCSettingProfile scSettingProfile) {
        return tagService.getProfileALias(profileId);
    }

    @PUT
    @ApiOperation(value = "edit dynamic-config profile")
    @ApiResponse(code = 200, message = "Success", response = SCSettingProfile.class)
    @Path("/{dataModelId}/profiles/{profileId}/setting")
    public SCSettingProfile editDynamicConfigurationProfile(@PathParam("dataModelId") Long dataModelId, @PathParam("profileId") Long profileId, @RequestBody SCSettingProfile scSettingProfile) throws UmpNbiException {
        if (tagService.checkNumberExited(profileId, scSettingProfile.number)) {
            throw new UmpNbiException("error_number_duplicate");
        }
        if (tagService.checkDuplicateAlias(profileId, scSettingProfile.alias)) {
            throw new UmpNbiException("error_alias_duplicate");
        }
        tagService.editProflieAlias(profileId, scSettingProfile.alias, scSettingProfile.isOther, scSettingProfile.number);
        return getDynamicConfigurationOfProfile(dataModelId, profileId, scSettingProfile);
    }

    @POST
    @ApiOperation(value = "add dynamic-config profile")
    @ApiResponse(code = 200, message = "Success", response = SCSettingProfile.class)
    @Path("/{dataModelId}/profiles/{profileId}/setting")
    public SCSettingProfile addDynamicConfigurationProfile(@PathParam("dataModelId") Long dataModelId, @PathParam("profileId") Long profileId, @RequestBody SCSettingProfile scSettingProfile) throws UmpNbiException {
        if (tagService.checkNumberExited(profileId, scSettingProfile.number)) {
            throw new UmpNbiException("error_number_duplicate");
        }
        if (tagService.checkDuplicateAlias(profileId, scSettingProfile.alias)) {
            throw new UmpNbiException("error_alias_duplicate");
        }
        tagService.editProflieAlias(profileId, scSettingProfile.alias, scSettingProfile.isOther, scSettingProfile.number);
        return getDynamicConfigurationOfProfile(dataModelId, profileId, scSettingProfile);
    }

    @POST
    @ApiOperation(value = "add subprofile")
    @ApiResponse(code = 200, message = "Success", response = SCDynamicProfile.class)
    @Path("/{dataModelId}/profiles/{profileId}/sub-profile")
    public SCDynamicProfile addSubProfile(@PathParam("dataModelId") Long dataModelId, @PathParam("profileId") Long profileId, @RequestBody SCDynamicProfile scDynamicProfile) throws UmpNbiException {
        try {
            Map<String, String> mapParam = new HashMap<>();
            mapParam.put("window_format", scDynamicProfile.window_format);
            mapParam.put("sub_object_title", scDynamicProfile.sub_object_title);
            mapParam.put("sub_title", scDynamicProfile.sub_title);
            tagService.addProfileSetting(profileId, mapParam);
            return getSubProfile(dataModelId, profileId, URLEncoder.encode(scDynamicProfile.sub_title, "UTF-8"));
        } catch (Exception e) {
            e.printStackTrace();
            throw new UmpNbiException("error_add_profile_setting");
        }
    }

    @GET
    @ApiOperation(value = "get all subprofile")
    @ApiResponse(code = 200, message = "Success", response = SCDynamicProfile.class)
    @Path("/{dataModelId}/profiles/{profileId}/sub-profile")
    public List<SCDynamicProfile> getAllSubProfile(@PathParam("dataModelId") Long dataModelId, @PathParam("profileId") Long profileId) {
        return tagService.searchSubProfile(profileId, "");
    }

    @POST
    @ApiOperation(value = "search subprofile")
    @ApiResponse(code = 200, message = "Success", response = SCDynamicProfile.class)
    @Path("/{dataModelId}/profiles/{profileId}/sub-profile/search")
    public List<SCDynamicProfile> searchSubProfile(@PathParam("dataModelId") Long dataModelId, @PathParam("profileId") Long profileId, @RequestBody SCParameterSearchForm form) {
        return tagService.searchSubProfile(profileId, form == null ? "" : form.name);
    }

    @PUT
    @ApiOperation(value = "edit subprofile")
    @ApiResponse(code = 200, message = "Success", response = SCDynamicProfile.class)
    @Path("/{dataModelId}/profiles/{profileId}/sub-profile/{subName}")
    public SCDynamicProfile editSubProfile(@PathParam("dataModelId") Long dataModelId, @PathParam("profileId") Long profileId, @PathParam("subName") String subName, @RequestBody SCDynamicProfile scDynamicProfile) throws UmpNbiException {
        try {
            Map<String, Object> mapParam = new HashMap<>();
            mapParam.put("window_format", scDynamicProfile.window_format);
            mapParam.put("sub_object_title", scDynamicProfile.sub_object_title);
            mapParam.put("sub_title", scDynamicProfile.sub_title);
            mapParam.put("oldPath", URLDecoder.decode(subName, "UTF-8"));
            mapParam.put("parameters", new Gson().toJsonTree(scDynamicProfile.parameters));

            tagService.editProfileSetting(profileId, mapParam);
            return getSubProfile(dataModelId, profileId, URLEncoder.encode(scDynamicProfile.sub_title, "UTF-8"));
        } catch (Exception e) {
            throw new UmpNbiException("error_edit_profile_setting");
        }

    }

    @DELETE
    @ApiOperation(value = "delete subprofile")
    @ApiResponse(code = 200, message = "Success", response = SCProfileConfig.class)
    @Path("/{dataModelId}/profiles/{profileId}/sub-profile/{subName}")
    public void deleteSubProfile(@PathParam("dataModelId") Long dataModelId, @PathParam("profileId") Long profileId, @PathParam("subName") String subName) throws UnsupportedEncodingException {
        tagService.deleteProfileSetting(profileId, URLDecoder.decode(subName, "UTF-8"));
    }

    @GET
    @ApiOperation(value = "get info subprofile")
    @ApiResponse(code = 200, message = "Success", response = SCDynamicProfile.class)
    @Path("/{dataModelId}/profiles/{profileId}/sub-profile/{subName}")
    public SCDynamicProfile getSubProfile(@PathParam("dataModelId") Long dataModelId, @PathParam("profileId") Long profileId, @PathParam("subName") String subName) throws UnsupportedEncodingException {
        return new SCDynamicProfile(tagService.get(profileId), URLDecoder.decode(subName, "UTF-8"));
    }

    @POST
    @ApiOperation(value = "add parameter to subprofile")
    @ApiResponse(code = 200, message = "Success", response = SCDynamicParameter.class)
    @Path("/{dataModelId}/profiles/{profileId}/sub-profile/{subName}/parameter")
    public SCDynamicParameter addParameterToSubProfile(@PathParam("dataModelId") Long dataModelId,
            @PathParam("profileId") Long profileId,
            @PathParam("subName") String subName,
            @RequestBody SCDynamicParameter scDynamicParameter) throws UnsupportedEncodingException {
        Map<String, String> mapParam = new HashMap<>();
        mapParam.put("name", subName);
        mapParam.put("path", scDynamicParameter.path);
        mapParam.put("alias", scDynamicParameter.alias);
        mapParam.put("setting_value", scDynamicParameter.setting_value);
        mapParam.put("type", scDynamicParameter.type);
        mapParam.put("is_pointer", scDynamicParameter.is_pointer);
        mapParam.put("read_only", scDynamicParameter.read_only);
        tagService.addParameterToSettingProfile(profileId, mapParam);
        return getParameterToSubProfile(dataModelId, profileId, URLDecoder.decode(subName, "UTF-8"), scDynamicParameter.path);
    }

    @POST
    @ApiOperation(value = "search parameter by parametername")
    @ApiResponse(code = 200, message = "Success", response = SCDynamicParameter.class)
    @Path("/{dataModelId}/profiles/{profileId}/sub-profile/{subName}/parameter/search")
    public List<SCDynamicParameter> searchParameterInSubprofiles(@PathParam("dataModelId") Long dataModelId,
            @PathParam("profileId") Long profileId,
            @PathParam("subName") String subName,
            @RequestBody SCParameterSearchForm searchForm) throws UnsupportedEncodingException {
        return tagService.searchParameter(profileId, URLDecoder.decode(subName, "UTF-8"), searchForm == null ? "" : searchForm.name);
    }

    @PUT
    @ApiOperation(value = "edit parameter in subprofile")
    @ApiResponse(code = 200, message = "Success", response = SCDynamicParameter.class)
    @Path("/{dataModelId}/profiles/{profileId}/sub-profile/{subName}/parameter/{parametername}")
    public SCDynamicParameter editParameterToSubProfile(@PathParam("dataModelId") Long dataModelId,
            @PathParam("profileId") Long profileId,
            @PathParam("subName") String subName,
            @PathParam("parametername") String parametername,
            @RequestBody SCDynamicParameter scDynamicParameter) throws UnsupportedEncodingException {
        Map<String, String> mapParam = new HashMap<>();
        mapParam.put("name", URLDecoder.decode(subName, "UTF-8"));
        mapParam.put("oldId", parametername);
        mapParam.put("path", scDynamicParameter.path);
        mapParam.put("alias", scDynamicParameter.alias);
        mapParam.put("setting_value", scDynamicParameter.setting_value);
        mapParam.put("type", scDynamicParameter.type);
        mapParam.put("is_pointer", scDynamicParameter.is_pointer);
        mapParam.put("read_only", scDynamicParameter.read_only);
        tagService.editParameterToSettingProfile(profileId, mapParam);
        return getParameterToSubProfile(dataModelId, profileId, URLDecoder.decode(subName, "UTF-8"), scDynamicParameter.path);
    }

    @GET
    @ApiOperation(value = "get parameter in subprofile")
    @ApiResponse(code = 200, message = "Success", response = SCDynamicParameter.class)
    @Path("/{dataModelId}/profiles/{profileId}/sub-profile/{subName}/parameter/{parametername}")
    public SCDynamicParameter getParameterToSubProfile(@PathParam("dataModelId") Long dataModelId,
            @PathParam("profileId") Long profileId,
            @PathParam("subName") String subName,
            @PathParam("parametername") String parametername) throws UnsupportedEncodingException {
        return new SCDynamicParameter(tagService.get(profileId), URLDecoder.decode(subName, "UTF-8"), parametername);
    }

    @DELETE
    @ApiOperation(value = "delete parameter in subprofile")
    @ApiResponse(code = 200, message = "Success", response = SCDynamicParameter.class)
    @Path("/{dataModelId}/profiles/{profileId}/sub-profile/{subName}/parameter/{parametername}")
    public void deleteParameterToSubProfile(@PathParam("dataModelId") Long dataModelId,
            @PathParam("profileId") Long profileId,
            @PathParam("subName") String subName,
            @PathParam("parametername") String parametername) throws UnsupportedEncodingException {
        tagService.deleteParameterToSettingProfile(profileId, parametername, URLDecoder.decode(subName, "UTF-8"));
    }

}
