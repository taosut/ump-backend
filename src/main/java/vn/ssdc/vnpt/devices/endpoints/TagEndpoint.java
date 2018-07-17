package vn.ssdc.vnpt.devices.endpoints;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import io.swagger.annotations.Api;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import vn.ssdc.vnpt.devices.model.Parameter;
import vn.ssdc.vnpt.devices.model.Tag;
import vn.ssdc.vnpt.devices.services.DataModelService;
import vn.ssdc.vnpt.devices.services.TagService;
import vn.vnpt.ssdc.core.ObjectCache;
import vn.vnpt.ssdc.core.SsdcCrudEndpoint;

import javax.ws.rs.*;
import java.util.*;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import org.springframework.http.ResponseEntity;
import vn.ssdc.vnpt.AcsClient;
import vn.ssdc.vnpt.devices.model.DeviceTypeVersion;
import vn.ssdc.vnpt.devices.model.ParameterDetail;
import vn.ssdc.vnpt.devices.model.ProfileSetting;
import vn.ssdc.vnpt.devices.services.DeviceTypeService;
import vn.ssdc.vnpt.devices.services.DeviceTypeVersionService;

/**
 * Created by vietnq on 11/3/16.
 */
@Component
@Path("tags")
@Produces(APPLICATION_JSON)
@Api("Tags")
public class TagEndpoint extends SsdcCrudEndpoint<Long, Tag> {

    private TagService tagService;

    @Autowired
    private ObjectCache ssdcCache;

    @Autowired
    private AcsClient acsClient;

    @Autowired
    private DeviceTypeVersionService deviceTypeVersionService;

    @Autowired
    public TagEndpoint(TagService tagService) {
        this.service = this.tagService = tagService;
    }

    @Autowired
    public DataModelService dataModelService;

    @GET
    @Path("/find-by-device-type-version")
    public List<Tag> findByDeviceTypeVersion(@QueryParam("id") Long id) {
        return tagService.findByDeviceTypeVersion(id);
    }

    @GET
    @Path("/get-list-root-tag")
    public List<Tag> getListRootTag() {
        return tagService.getListRootTag();
    }

    @GET
    @Path("/get-provisioning-tag-by-device-type-version-id")
    public List<Tag> getRootTagByDeviceTypeVersionId(@QueryParam("id") Long id) {
        return tagService.getProvisioningTagByDeviceTypeVersionId(id);
    }

    @GET
    @Path("/get-list-provisioning-tag-by-root-tag-id")
    public List<Tag> getListProvisioningTagByRootTagId(@QueryParam("id") Long id) {
        return tagService.getListProvisioningTagByRootTagId(id);
    }

    @GET
    @Path("/get-list-assigned")
    public List<Tag> getListAssigned() {
        return tagService.getListAssigned();
    }

    @GET
    @Path("/get-list-profiles")
    public List<Tag> getListProfiles() {
        return tagService.getListProfiles();
    }

    @GET
    @Path("/get-page-root-tag")
    public Page<Tag> getPageRootTag(@QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("limit") @DefaultValue("20") int limit) {
        return tagService.getPageRootTag(page, limit);
    }

    @GET
    @Path("/get-page")
    public Page<Tag> getPage(@QueryParam("deviceTypeVersionId") @DefaultValue("0") Long deviceTypeVersionId,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("limit") @DefaultValue("20") int limit,
            @QueryParam("name") @DefaultValue("") String name,
            @QueryParam("correspondingModule") @DefaultValue("") String correspondingModule) {

        Set<String> correspondingModuleSet = new HashSet<String>();
        if (!"".equals(correspondingModule)) {
            correspondingModuleSet = new HashSet<String>(Arrays.asList(correspondingModule.split(",")));
        }
        return tagService.getPage(deviceTypeVersionId, page, limit, name, correspondingModuleSet);
    }

    @GET
    @Path("/find-synchronized-by-device-type-version")
    public List<Tag> findSynchronizedByDeviceTypeVersion(@QueryParam("id") @DefaultValue("0") Long deviceTypeVersionId) {
        return tagService.findSynchronizedByDeviceTypeVersion(deviceTypeVersionId);
    }

    @GET
    @Path("/get-parameters-of-device/{tagId}/{deviceId}")
    public Set<Parameter> getParametersOfDevice(@PathParam("tagId") Long tagId, @PathParam("deviceId") String deviceId) {
        String cacheId = deviceId + "-" + tagId.toString();
        Set<Parameter> result = new HashSet<>();
        try {
            Object object = ssdcCache.get(cacheId, new HashSet<Parameter>().getClass());
            if (object != null) {
                result = (Set<Parameter>) object;
            } else {
                result = dataModelService.getProfileOfDevices(deviceId, tagId);
                ssdcCache.put(cacheId, result, new HashSet<Parameter>().getClass());
            }
        } catch (Exception e) {
            result = dataModelService.getProfileOfDevices(deviceId, tagId);
            ssdcCache.put(cacheId, result, new HashSet<Parameter>().getClass());
        }
        return result;
    }

    @GET
    @Path("/get-list-profile-synchronized")
    public List<Tag> getListProfileSynchronized() {
        return tagService.getListProfileSynchronized();
    }

    @GET
    @Path("/get-profile-others")
    public Tag getProfileOthers(@QueryParam("deviceTypeVersionId") @DefaultValue("0") Long deviceTypeVersionId) {
        return tagService.getProfileOthers(deviceTypeVersionId);
    }

    @GET
    @Path("/check-by-device-type-version-id")
    public List<Tag> checkByDeviceTypeVersionId(@QueryParam("deviceTypeVersionId") @DefaultValue("0") Long deviceTypeVersionId) {
        return tagService.checkByDeviceTypeVersionId(deviceTypeVersionId);
    }

    @GET
    @Path("/delete-by-device-type-version-id")
    public void deleteByDeviceTypeVersionId(@QueryParam("deviceTypeVersionId") @DefaultValue("0") Long deviceTypeVersionId) {
        tagService.deleteByDeviceTypeVersionId(deviceTypeVersionId);
    }

    @GET
    @Path("/get-list-by-corresponding-module")
    public List<Tag> getListByCorrespondingModule(@QueryParam("deviceTypeVersionId") @DefaultValue("0") Long deviceTypeVersionId,
            @QueryParam("correspondingModule") @DefaultValue("") String correspondingModule) {

        if (deviceTypeVersionId == 0) {
            return tagService.getListByCorrespondingModule(correspondingModule);
        } else {
            return tagService.getListByCorrespondingModule(deviceTypeVersionId, correspondingModule);
        }
    }

    @GET
    @Path("/setting-profile/suggest-parameter")
    public List<String> getListSuggestParameter(@QueryParam("tagId") @DefaultValue("0") Long tagId) {
        List<String> lstParameter = new ArrayList<String>();
        Map<String, Parameter> map = tagService.get(tagId).parameters;
        for (Map.Entry<String, Parameter> entry : map.entrySet()) {
            lstParameter.add(entry.getKey());
        }
        return lstParameter;
    }

    @POST
    @Path("/setting-profile/detail")
    public String addParameterToSettingProfile(@QueryParam("tagId") @DefaultValue("0") Long tagId,
            Map<String, String> mapParam) {
        Map<String, String> error = tagService.validateSettingParameter(tagId, mapParam, "add");
        if (!error.isEmpty()) {
            error.put("success", String.valueOf(Boolean.FALSE));
        } else {
            error.put("success", tagService.addParameterToSettingProfile(tagId, mapParam).toString());
        }
        return new Gson().toJson(error);
//        return tagService.addParameterToSettingProfile(tagId, mapParam);
    }

    @POST
    @Path("/setting-profile/update/detail")
    public String editParameterToSettingProfile(@QueryParam("tagId") @DefaultValue("0") Long tagId,
            Map<String, String> mapParam) {
        Map<String, String> error = tagService.validateSettingParameter(tagId, mapParam, "edit");
        if (!error.isEmpty()) {
            error.put("success", String.valueOf(Boolean.FALSE));
        } else {
            error.put("success", tagService.editParameterToSettingProfile(tagId, mapParam).toString());
        }
        return new Gson().toJson(error);
//        tagService.editParameterToSettingProfile(tagId, mapParam);
    }

    @DELETE
    @Path("/setting-profile/detail")
    public void deleteParameterToSettingProfile(@QueryParam("tagId") @DefaultValue("0") Long tagId,
            @QueryParam("id") @DefaultValue("0") String id,
            @QueryParam("path") @DefaultValue("0") String path, Map<String, String> mapParam) {
        tagService.deleteParameterToSettingProfile(tagId, id, path);
    }

    @POST
    @Path("/setting-profile/sub-profile")
    public Boolean addSettingProfile(@QueryParam("tagId") @DefaultValue("0") Long tagId,
            Map<String, String> mapParam) throws Exception {
        return tagService.addProfileSetting(tagId, mapParam);
    }

    @PUT
    @Path("/setting-profile/sub-profile")
    public void editSettingProfile(@QueryParam("tagId") @DefaultValue("0") Long tagId,
            Map<String, Object> mapParam) throws Exception {
        tagService.editProfileSetting(tagId, mapParam);
    }

    @POST
    @Path("/setting-profile/sub-profile/checkDuplicate")
    public boolean checkDuplicateNameSubProfile(@QueryParam("tagId") @DefaultValue("0") Long tagId,
            @QueryParam("name") String name) throws Exception {
        return tagService.checkDuplicate(tagId, name);
    }

    @DELETE
    @Path("/setting-profile/sub-profile")
    public void deleteSettingProfile(@QueryParam("tagId") @DefaultValue("0") Long tagId,
            @QueryParam("sub-profile-name") String subProfilename) throws Exception {
        tagService.deleteProfileSetting(tagId, subProfilename);
    }

    @GET
    @Path("/setting-profile/sub-profile")
    public String getInforSubProfile(@QueryParam("tagId") @DefaultValue("0") Long tagId,
            @QueryParam("sub-name") @DefaultValue("0") String subProfileName) throws UnsupportedEncodingException {
        return tagService.getInforSubProfile(subProfileName, tagId);
    }

    @GET
    @Path("/setting-profile/profile-alias")
    public String editProfileAlias(@QueryParam("tagId") @DefaultValue("0") Long tagId,
            @QueryParam("name_profile") @DefaultValue("0") String subProfileName,
            @QueryParam("profile_display") @DefaultValue("false") String isOther,
            @QueryParam("number") @DefaultValue("0") String number) throws Exception {
        JsonObject object = new JsonObject();
        boolean error = false;
        if (tagService.checkNumberExited(tagId, number)) {
            object.addProperty("error", "Number Existed");
            object.addProperty("result", "1");
            error = true;
        }
        if (tagService.checkDuplicateAlias(tagId, subProfileName)) {
            object.addProperty("error", "Duplicate Profile TItle");
            object.addProperty("result", "2");
            error = true;
        }
        if (!error) {
            object.addProperty("error", "None");
            object.addProperty("result", tagService.editProflieAlias(tagId, subProfileName, isOther, number));
        }
        return object.toString();
//        return tagService.editProflieAlias(tagId, subProfileName, isOther);
    }

    @GET
    @Path("/sub-profile-setting/{deviceId}")
    public String getSubProfileSetting(
            @PathParam("deviceId") String deviceId,
            @QueryParam("tagId") @DefaultValue("0") Long tagId) {
        return new Gson().toJson(tagService.parseInput(tagId, dataModelService.getProfileOfDevices(deviceId, tagId)));
    }

    @GET
    @Path("/profiles")
    public String getListProfileByDeviceId(@QueryParam("deviceId") @DefaultValue("0") String deviceId) {
        return tagService.getListTagByDeviceId(deviceId);
    }

}
