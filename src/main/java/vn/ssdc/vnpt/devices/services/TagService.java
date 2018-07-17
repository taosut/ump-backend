package vn.ssdc.vnpt.devices.services;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import vn.ssdc.vnpt.devices.model.DeviceTypeVersion;
import vn.ssdc.vnpt.devices.model.Parameter;
import vn.ssdc.vnpt.devices.model.Tag;
import vn.ssdc.vnpt.umpexception.DuplicationDeviceTypeVersionException;
import vn.vnpt.ssdc.core.SsdcCrudService;
import vn.vnpt.ssdc.jdbc.factories.RepositoryFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import vn.ssdc.vnpt.AcsClient;
import vn.ssdc.vnpt.devices.model.ParameterSubProfile;
import vn.ssdc.vnpt.devices.model.ProfileSetting;
import java.util.regex.*;
import org.apache.commons.lang3.StringUtils;
import vn.ssdc.vnpt.selfCare.model.SCDynamicParameter;
import vn.ssdc.vnpt.selfCare.model.SCDynamicProfile;
import vn.ssdc.vnpt.selfCare.model.SCObjectInstanceInSubProfile;
import vn.ssdc.vnpt.selfCare.model.SCProfileDisplay;
import vn.ssdc.vnpt.selfCare.model.SCSettingProfile;
import vn.ssdc.vnpt.selfCare.model.SCSubProfile;

/**
 * Created by vietnq on 11/3/16.
 */
@Service
public class TagService extends SsdcCrudService<Long, Tag> {

    @Autowired
    public DeviceTypeVersionService deviceTypeVersionService;
    private Tag profileOthers;

    @Autowired
    private AcsClient acsClient;

    @Autowired
    private DataModelService dataModelService;

    @Autowired
    public TagService(RepositoryFactory repositoryFactory) {
        this.repository = repositoryFactory.create(Tag.class);
    }

    public List<Tag> findByDeviceTypeVersionIdAssignedSynchronized(Long deviceTypeVersionId) {
        String whereExp = "device_type_version_id=? and assigned=0 and corresponding_module LIKE '%\"devices\"%'";
        return this.repository.search(whereExp, deviceTypeVersionId);
    }

    public List<Tag> findByDeviceTypeVersion(Long deviceTypeVersionId) {
        String whereExp = "device_type_version_id=? and assigned=0";
        return this.repository.search(whereExp, deviceTypeVersionId);
    }

    public List<Tag> findAssignedTags(Long deviceTypeVersionId) {
        String whereExp = "device_type_version_id=? and assigned=1";
        return this.repository.search(whereExp, deviceTypeVersionId);
    }

    public void deleteByRootTag(Long rootTagId, Long deviceTypeVersionId) {
        String query = "root_tag_id=? and device_type_version_id=?";
        List<Tag> listTag = this.repository.search(query, rootTagId, deviceTypeVersionId);
        if (!listTag.isEmpty()) {
            this.repository.delete(listTag.get(0).id);
        }
    }

    public void generateProfile(Map<String, Tag> listProfile, String profileNames, DeviceTypeVersion deviceTypeVersion) {
        Tag tag = new Tag();
        tag.name = profileNames;
        tag.deviceTypeVersionId = deviceTypeVersion.id;
        tag.parameters = new HashMap<String, Parameter>();
        tag.assigned = 0;
        tag.assignedGroup = "PROFILE";
        tag.rootTagId = null;
        listProfile.put(tag.name, tag);
    }

    public Tag generateProfileOther(String name, DeviceTypeVersion deviceTypeVersion) {
        Tag tag = new Tag();
        tag.name = name;
        tag.deviceTypeVersionId = deviceTypeVersion.id;
        tag.parameters = new HashMap<String, Parameter>();
        tag.assigned = 0;
        tag.assignedGroup = "PROFILE";
        tag.rootTagId = null;
        return tag;
    }

    public void addDeviceTypeVersionId(Long tagId, Long deviceTypeVersionId) {
        List<Tag> tagList = getListTagByRootTag(tagId);
        for (Tag tmp : tagList) {
            if (tmp.deviceTypeVersionId.equals(deviceTypeVersionId)) {
                throw new DuplicationDeviceTypeVersionException("DeviceTypeVersion " + deviceTypeVersionId + " existed!!!");
            }
        }
        Tag tag = this.get(tagId);
        tag.id = null;
        tag.rootTagId = tagId;
        tag.deviceTypeVersionId = deviceTypeVersionId;
        this.create(tag);
    }

    public List<Tag> getListTagByRootTag(Long rootTag) {
        String query = "root_tag_id=?";
        List<Tag> listTag = this.repository.search(query, rootTag);
        return listTag;
    }

    public Boolean checkNameExisted(String tagName) {
        String query = "name=?";
        List<Tag> listTag = this.repository.search(query, tagName);
        if (listTag.isEmpty()) {
            return false;
        }
        return true;
    }

    public List<Tag> getListRootTag() {
        return this.repository.search("assigned=0 AND device_type_version_id IS NULL AND root_tag_id IS NULL");
    }

    public List<Tag> getListAssigned() {
        return this.repository.search("assigned=1 AND device_type_version_id IS NOT NULL AND root_tag_id IS NOT NULL");
    }

    public List<Tag> getListProfiles() {
        return this.repository.search("assigned=0 AND device_type_version_id IS NOT NULL AND root_tag_id IS NULL");
    }

    public List<Tag> getProvisioningTagByDeviceTypeVersionId(Long id) {
        String whereExp = "assigned=1 AND device_type_version_id=? AND root_tag_id IS NOT NULL";
        return this.repository.search(whereExp, id);
    }

    public Page<Tag> getPageRootTag(int page, int limit) {
        String whereExp = "assigned=0 AND device_type_version_id IS NULL AND root_tag_id IS NULL";
        return this.repository.search(whereExp, new PageRequest(page, limit));
    }

    public List<Tag> getListProvisioningTagByRootTagId(Long id) {
        String whereExp = "assigned=1 AND device_type_version_id IS NOT NULL AND root_tag_id=?";
        return this.repository.search(whereExp, id);
    }

    public List<Tag> findSynchronizedByDeviceTypeVersion(Long deviceTypeVersionId) {
        String whereExp = "corresponding_module LIKE '%\"devices\"%' AND device_type_version_id=?";
        return this.repository.search(whereExp, deviceTypeVersionId);
    }

    public List<Tag> getListProfileSynchronized() {
        return this.repository.search("assigned=0 AND device_type_version_id IS NOT NULL AND root_tag_id IS NULL AND corresponding_module LIKE '%\"devices\"%'");
    }

    public Tag getProfileOthers(Long deviceTypeVersionId) {
        Tag tagResult = null;
        String whereExp = "name=? AND device_type_version_id=?";
        List<Tag> tags = this.repository.search(whereExp, "Others", deviceTypeVersionId);
        if (tags.size() > 0) {
            tagResult = tags.get(0);
        }

        return tagResult;
    }

    public List<Tag> getListProfilesOfVersion(Long deviceTypeVersionId) {
        return this.repository.search("device_type_version_id=? AND assigned_group=?", deviceTypeVersionId, "PROFILE");
    }

    public List<Tag> checkByDeviceTypeVersionId(Long deviceTypeVersionId) {
        String whereExp = "assigned=1 AND device_type_version_id=?";
        List<Tag> tags = this.repository.search(whereExp, deviceTypeVersionId);
        return tags;
    }

    public void deleteByDeviceTypeVersionId(Long deviceTypeVersionId) {
        String whereExp = " device_type_version_id=?";
        List<Tag> tags = this.repository.search(whereExp, deviceTypeVersionId);
        if (tags.size() > 0) {
            for (Tag tag : tags) {
                this.repository.delete(tag);
            }
        }
    }

    public List<Tag> getListByCorrespondingModule(Long deviceTypeVersionId, String correspondingModule) {
        String whereExp = "device_type_version_id=? AND corresponding_module LIKE '%\"" + correspondingModule + "\"%'";
        List<Tag> tags = this.repository.search(whereExp, deviceTypeVersionId);
        return tags;
    }

    public List<Tag> getListByCorrespondingModule(String correspondingModule) {
        String whereExp = "corresponding_module LIKE '%\"devices\"%' AND corresponding_module LIKE '%\"" + correspondingModule + "\"%'";
        List<Tag> tags = this.repository.search(whereExp);

        return tags;
    }

    public String getListTagByDeviceId(String deviceId) {
        DeviceTypeVersion deviceTypeVersion = deviceTypeVersionService.findbyDevice(deviceId);
        // Get list tags
        try {
            if (deviceTypeVersion != null) {
                List<Tag> tags = findSynchronizedByDeviceTypeVersion(deviceTypeVersion.id);
                JsonObject result = new JsonObject();
                JsonArray arrOther = new JsonArray();
                JsonArray arrFavourite = new JsonArray();
                JsonArray arrDiagnostic = new JsonArray();

                for (Map.Entry<String, Tag> entry : deviceTypeVersion.diagnostics.entrySet()) {
                    Tag tag = new Tag();
                    tag.id = entry.getValue().id;

                    if (Strings.isNullOrEmpty(tag.profileSetting)) {
                        tag.name = entry.getValue().name;
                    } else {
                        JsonObject obj = new Gson().fromJson(tag.profileSetting, JsonObject.class);
                        tag.name = obj.get("alias") != null ? obj.get("alias").getAsString() : entry.getValue().name;
                    }

                    arrDiagnostic.add(new Gson().toJsonTree(tag));
                }
                for (Tag tag : tags) {
                    Tag tmp = new Tag();
                    tmp.id = tag.id;
                    if (Strings.isNullOrEmpty(tag.profileSetting)) {
                        tmp.name = tag.name;
                    } else {
                        JsonObject obj = new Gson().fromJson(tag.profileSetting, JsonObject.class);
                        tmp.name = obj.get("alias") != null ? obj.get("alias").getAsString() : tag.name;
                    }

                    if (tag.profileSetting != null) {
                        JsonObject object = new Gson().fromJson(tag.profileSetting, JsonObject.class);
                        if (object.get("isOther") != null && object.get("isOther").getAsBoolean()) {
                            arrOther.add(new Gson().toJsonTree(tmp));
                        } else {
                            JsonObject tagWithPosition = new Gson().fromJson(new Gson().toJsonTree(tmp), JsonObject.class);
                            tagWithPosition.addProperty("position", object.get("number") == null ? 0 : object.get("number").getAsInt());
                            arrFavourite.add(tagWithPosition);
                        }
                    } else {
                        arrOther.add(new Gson().toJsonTree(tmp));
                    }
                }
                result.add("profiles_favourite", arrFavourite);
                result.add("profiles_other", arrOther);
                result.add("profiles_diagnostic", arrDiagnostic);
                return result.toString();
            } else {
                return "";
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return "";
        }

    }

    public List<Tag> getPage(Long deviceTypeVersionId, Integer page, Integer limit, String name, Set<String> correspondingModuleSet) {

        Set<String> conditions = new HashSet<>();
        if (deviceTypeVersionId != null) {
            conditions.add(String.format("device_type_version_id = %s", deviceTypeVersionId));
        }
        if (!Strings.isNullOrEmpty(name)) {
            conditions.add(String.format("name like '%s'", "%" + name + "%"));
        }
        if (correspondingModuleSet != null && !correspondingModuleSet.isEmpty()) {
            Set<String> coressModule = new HashSet<>();
            for (String tmp : correspondingModuleSet) {
                coressModule.add("corresponding_module LIKE '%\"" + tmp + "\"%'");
            }
            conditions.add(String.join(" OR ", coressModule));
        }
        if (page != null && limit != null) {
            int indexPage = page.intValue() - 1;
            if (!conditions.isEmpty()) {
                String query = String.join(" AND ", conditions);
                return this.repository.search(query, new PageRequest(indexPage, limit)).getContent();
            } else {
                return this.repository.search(" 1=1 ", new PageRequest(indexPage, limit)).getContent();
            }
        } else {
            if (!conditions.isEmpty()) {
                String query = String.join(" AND ", conditions);
                return this.repository.search(query);
            } else {
                return this.getAll();
            }
        }
    }

    public Page<Tag> getPage(Long deviceTypeVersionId, int page, int limit, String name, Set<String> correspondingModuleSet) {
        Set<String> correspondingModuleSetWhere = new HashSet<String>();
        for (String correspondingModule : correspondingModuleSet) {
            if (!"".equals(correspondingModule)) {
                correspondingModuleSetWhere.add("corresponding_module LIKE '%\"" + correspondingModule + "\"%'");
            }
        }

        String correspondingModuleWhere = "";
        if (correspondingModuleSetWhere.size() > 0) {
            correspondingModuleWhere = String.join(" OR ", correspondingModuleSetWhere);
        } else {
            correspondingModuleWhere = "1=1";
        }
        String whereExp = "device_type_version_id=" + deviceTypeVersionId + " AND name LIKE '%" + name + "%' AND (" + correspondingModuleWhere + ")";
        return this.repository.search(whereExp, new PageRequest(page, limit));
    }

    public Boolean addParameterToSettingProfile(Long tagId, Map<String, String> mapParam) {
        try {
            Tag tag = this.get(tagId);
            String profileSettingDetail = "";
            JsonArray array = new JsonArray();
            if (null != tag.subProfileSetting) {
                profileSettingDetail = tag.subProfileSetting;
                array = new Gson().fromJson(profileSettingDetail, JsonArray.class);
            }

            for (int i = 0; i < array.size(); i++) {
                JsonObject obj = array.get(i).getAsJsonObject();
                if (obj.get("sub_title").getAsString().equals(mapParam.get("name"))) {
                    array.remove(obj);
                    JsonArray arrayParameter = new JsonArray();
                    if (null != obj.get("parameters")) {
                        arrayParameter = obj.get("parameters").getAsJsonArray();
                    }
                    JsonObject object = new JsonObject();
                    object.addProperty("path", mapParam.get("path"));
                    object.addProperty("alias", mapParam.get("alias"));
                    object.addProperty("setting_value", mapParam.get("setting_value"));
                    object.addProperty("type", mapParam.get("type"));
                    object.addProperty("is_pointer", mapParam.get("is_pointer") == null ? "" : mapParam.get("is_pointer"));
                    object.addProperty("read_only", mapParam.get("read_only") == null ? "" : mapParam.get("read_only"));
                    object.addProperty("display_key", mapParam.get("display_key") == null ? "false" : mapParam.get("display_key"));
                    JsonArray arrayFilter = new JsonArray();
                    if (mapParam.containsKey("filter_key_commit") && mapParam.containsKey("filter_type_commit") && mapParam.containsKey("filter_value_commit")) {
                        String key = mapParam.get("filter_key_commit");
                        String type = mapParam.get("filter_type_commit");
                        String value = mapParam.get("filter_value_commit");
                        String[] arrFilter = key.split("@@@");
                        String[] arrType = type.split("@@@");
                        String[] arrValue = value.split("@@@");

                        for (int f = 0; f < arrFilter.length; f++) {
                            JsonObject objFilter = new JsonObject();
                            objFilter.addProperty("filter_key", arrFilter[f]);
                            objFilter.addProperty("filter_type", arrType[f]);
                            objFilter.addProperty("filter_value", arrValue[f]);
                            arrayFilter.add(objFilter);
                        }
                        object.add("filter", arrayFilter);
                    }

                    arrayParameter.add(object);
                    obj.remove("parameters");
                    obj.add("parameters", arrayParameter);
                    array.add(obj);;
                    break;
                } else {
                }
            }
            tag.subProfileSetting = new Gson().toJson(array);
            this.update(tagId, tag);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public Boolean editParameterToSettingProfile(Long tagId, Map<String, String> mapParam) {
        try {
            Tag tag = this.get(tagId);
            String profileSettingDetail = "";
            JsonArray array = new JsonArray();
            if (null != tag.subProfileSetting) {
                profileSettingDetail = tag.subProfileSetting;
                array = new Gson().fromJson(profileSettingDetail, JsonArray.class);
            }

            for (int i = 0; i < array.size(); i++) {
                JsonObject obj = array.get(i).getAsJsonObject();
                if (obj.get("sub_title").getAsString().equals(mapParam.get("name"))) {
                    array.remove(obj);
                    JsonArray arrayParameter = new JsonArray();
                    if (null != obj.get("parameters")) {
                        arrayParameter = obj.get("parameters").getAsJsonArray();
                    }
                    if (arrayParameter.size() > 0) {
                        for (int j = 0; j < arrayParameter.size(); j++) {
                            JsonObject obj1 = arrayParameter.get(j).getAsJsonObject();
                            if (obj1.get("path").getAsString().equals(mapParam.get("oldId").toString())) {
                                arrayParameter.remove(obj1);
                                JsonObject object = new JsonObject();
                                object.addProperty("path", mapParam.get("path"));
                                object.addProperty("alias", mapParam.get("alias"));
                                object.addProperty("setting_value", mapParam.get("setting_value"));
                                object.addProperty("type", mapParam.get("type"));
                                object.addProperty("is_pointer", mapParam.get("is_pointer") == null ? "false" : "true");
                                object.addProperty("read_only", mapParam.get("read_only") == null ? "false" : "true");

                                object.addProperty("display_key", mapParam.get("display_key") == null ? "false" : mapParam.get("display_key"));
                                JsonArray arrayFilter = new JsonArray();
                                if (mapParam.containsKey("filter_key_commit") && mapParam.containsKey("filter_type_commit") && mapParam.containsKey("filter_value_commit")) {
                                    String key = mapParam.get("filter_key_commit");
                                    String type = mapParam.get("filter_type_commit");
                                    String value = mapParam.get("filter_value_commit");
                                    String[] arrFilter = key.split("@@@");
                                    String[] arrType = type.split("@@@");
                                    String[] arrValue = value.split("@@@");

                                    for (int f = 0; f < arrFilter.length; f++) {
                                        JsonObject objFilter = new JsonObject();
                                        objFilter.addProperty("filter_key", arrFilter[f]);
                                        objFilter.addProperty("filter_type", arrType[f]);
                                        objFilter.addProperty("filter_value", arrValue[f]);
                                        arrayFilter.add(objFilter);
                                    }
                                    object.add("filter", arrayFilter);
                                }

                                arrayParameter.add(object);
                            }
                        }
                    } else {
                        JsonObject object = new JsonObject();
                        object.addProperty("path", mapParam.get("path"));
                        object.addProperty("alias", mapParam.get("alias"));
                        object.addProperty("setting_value", mapParam.get("setting_value"));
                        object.addProperty("type", mapParam.get("type"));
                        object.addProperty("is_pointer", mapParam.get("is_pointer") == null ? "false" : "true");
                        object.addProperty("read_only", mapParam.get("read_only") == null ? "false" : "true");

                        object.addProperty("display_key", mapParam.get("display_key") == null ? "false" : mapParam.get("display_key"));
                        JsonArray arrayFilter = new JsonArray();
                        if (mapParam.containsKey("filter_key_commit") && mapParam.containsKey("filter_type_commit") && mapParam.containsKey("filter_value_commit")) {
                            String key = mapParam.get("filter_key_commit");
                            String type = mapParam.get("filter_type_commit");
                            String value = mapParam.get("filter_value_commit");
                            String[] arrFilter = key.split("@@@");
                            String[] arrType = type.split("@@@");
                            String[] arrValue = value.split("@@@");

                            for (int f = 0; f < arrFilter.length; f++) {
                                JsonObject objFilter = new JsonObject();
                                objFilter.addProperty("filter_key", arrFilter[f]);
                                objFilter.addProperty("filter_type", arrType[f]);
                                objFilter.addProperty("filter_value", arrValue[f]);
                                arrayFilter.add(objFilter);
                            }
                            object.add("filter", arrayFilter);
                        }

                        arrayParameter.add(object);
                    }
                    array.add(obj);
                    break;
                }
            }
            tag.subProfileSetting = new Gson().toJson(array);
            this.update(tagId, tag);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

    }

    public void deleteParameterToSettingProfile(Long tagId, String listId, String path) {
        Tag tag = this.get(tagId);
        String profileSettingDetail = "";
        JsonArray array = new JsonArray();
        if (null != tag.subProfileSetting) {
            profileSettingDetail = tag.subProfileSetting;
            array = new Gson().fromJson(profileSettingDetail, JsonArray.class);
        }

        for (int i = 0; i < array.size(); i++) {
            JsonObject obj = array.get(i).getAsJsonObject();
            if (obj.get("sub_title").getAsString().equals(path)) {
                array.remove(obj);
                String list[] = listId.split("\\@@@");
                JsonArray arrayParameter = obj.get("parameters").getAsJsonArray();
                for (int j = 0; j < list.length; j++) {
                    String id = list[j];
                    id = id.replace("___", "{i}");
                    for (int i1 = 0; i1 < arrayParameter.size(); i1++) {
                        JsonObject obj1 = arrayParameter.get(i1).getAsJsonObject();
                        if (obj1.get("path").getAsString().equals(id)) {
                            arrayParameter.remove(obj1);
                        }
                    }
                }
                obj.remove("parameters");
                obj.add("parameters", arrayParameter);
                array.add(obj);
                break;
            }
        }
        tag.subProfileSetting = new Gson().toJson(array);
        this.update(tagId, tag);
    }

    public boolean addProfileSetting(Long tagId, Map<String, String> mapParam) throws Exception {
        Tag tag = this.get(tagId);
        String profileSettingDetail = "";
        JsonArray array = new JsonArray();
        if (Strings.isNullOrEmpty(tag.profileSetting)) {
            JsonObject object = new JsonObject();
            object.addProperty("alias", tag.name);
            object.addProperty("isOther", "false");
            object.addProperty("number", "0");
            tag.profileSetting = new Gson().toJson(object);
        }
        if (null != tag.subProfileSetting) {
            profileSettingDetail = tag.subProfileSetting;
            array = new Gson().fromJson(profileSettingDetail, JsonArray.class);
        }
        // check name existed
        boolean isExisted = false;
        for (int i = 0; i < array.size(); i++) {
            JsonObject obj = array.get(i).getAsJsonObject();
            if (obj.get("sub_title").getAsString().equals(mapParam.get("sub_title"))) {
                isExisted = true;
            }
        }

        if (isExisted) {
            return false;
        }
        JsonObject obj = new JsonObject();
        obj.addProperty("window_format", mapParam.get("window_format"));
        obj.addProperty("sub_object_title", mapParam.get("sub_object_title") != null ? mapParam.get("sub_object_title").trim() : mapParam.get("sub_object_title"));
        obj.addProperty("sub_title", mapParam.get("sub_title") != null ? mapParam.get("sub_title").trim() : mapParam.get("sub_title"));
        String action = "";
        if (mapParam.get("add_action") != null) {
            action += "add" + ",";
        }
        if (mapParam.get("edit_action") != null) {
            action += "edit" + ",";
        }
        if (mapParam.get("delete_action") != null) {
            action += "delete" + ",";
        }
        if (!action.equals("")) {
            action = action.substring(0, action.length() - 1);
        }
        obj.addProperty("action", action);
        obj.add("parameters", new JsonArray());
        array.add(obj);
        tag.subProfileSetting = new Gson().toJson(array);
        this.update(tagId, tag);
        return true;
    }

    public void editProfileSetting(Long tagId, Map<String, Object> mapParam) throws Exception {
        Tag tag = this.get(tagId);
        String profileSettingDetail = "";
        JsonArray array = new JsonArray();
        if (null != tag.subProfileSetting) {
            profileSettingDetail = tag.subProfileSetting;
            array = new Gson().fromJson(profileSettingDetail, JsonArray.class);
        }
        // check name existed
        for (int i = 0; i < array.size(); i++) {
            if (array.get(i).getAsJsonObject().get("sub_title").getAsString().equals(mapParam.get("oldPath"))) {
                JsonObject oldObject = array.get(i).getAsJsonObject();
                array.remove(i);
                JsonObject obj = new JsonObject();
                obj.addProperty("window_format", mapParam.get("window_format").toString());
                obj.addProperty("sub_object_title", mapParam.get("sub_object_title").toString() != null ? mapParam.get("sub_object_title").toString().trim() : mapParam.get("sub_object_title").toString());
                obj.addProperty("sub_title", mapParam.get("sub_title").toString() != null ? mapParam.get("sub_title").toString().trim() : mapParam.get("sub_title").toString());
                String action = "";
                if (mapParam.get("add_action") != null) {
                    action += "add" + ",";
                }
                if (mapParam.get("edit_action") != null) {
                    action += "edit" + ",";
                }
                if (mapParam.get("delete_action") != null) {
                    action += "delete" + ",";
                }
                if (!action.equals("")) {
                    action = action.substring(0, action.length() - 1);
                }
                obj.addProperty("action", action);
                if (mapParam.get("parameters") != null) {
                    obj.add("parameters", (JsonArray) mapParam.get("parameters"));
                } else {
                    obj.add("parameters", oldObject.get("parameters").getAsJsonArray());
                }
                array.add(obj);
                break;
            }
        }
        tag.subProfileSetting = new Gson().toJson(array);
        this.update(tagId, tag);
    }

    public List<SCDynamicProfile> searchSubProfile(Long tagId, String name) {
        List<SCDynamicProfile> scDynamicProfilesa = new ArrayList<>();
        Tag tag = this.get(tagId);
        String profileSettingDetail = "";
        JsonArray array = new JsonArray();
        if (null != tag.subProfileSetting) {
            profileSettingDetail = tag.subProfileSetting;
            array = new Gson().fromJson(profileSettingDetail, JsonArray.class);
        }
        // check name existed
        for (int i = 0; i < array.size(); i++) {
            if (array.get(i).getAsJsonObject().get("sub_title").getAsString().toLowerCase().contains(name.toLowerCase()) || Strings.isNullOrEmpty(name)) {
                scDynamicProfilesa.add(new SCDynamicProfile(get(tagId), array.get(i).getAsJsonObject().get("sub_title").getAsString()));
            }
        }
        return scDynamicProfilesa;
    }

    public List<SCDynamicParameter> searchParameter(Long tagId, String subName, String name) {
        List<SCDynamicParameter> scDynamicParameters = new ArrayList<>();
        Tag tag = get(tagId);
        JsonArray array = new Gson().fromJson(tag.subProfileSetting, JsonArray.class);
        for (int i = 0; i < array.size(); i++) {
            JsonObject obj = array.get(i).getAsJsonObject();
            String subTitle = obj.get("sub_title") == null ? "" : obj.get("sub_title").getAsString();
            if (subTitle.equals(subName)) {
                JsonArray arrParam = obj.get("parameters").getAsJsonArray();
                for (int j = 0; j < arrParam.size(); j++) {
                    JsonObject objParam = arrParam.get(j).getAsJsonObject();
                    if (objParam.get("path") != null && objParam.get("path").getAsString().toLowerCase().contains(name.toLowerCase()) || Strings.isNullOrEmpty(name)) {
                        SCDynamicParameter dynamicParameter = new SCDynamicParameter();
                        dynamicParameter.alias = objParam.get("alias") == null ? "" : objParam.get("alias").getAsString();
                        dynamicParameter.is_pointer = objParam.get("is_pointer") == null ? "" : objParam.get("is_pointer").getAsString();
                        dynamicParameter.path = objParam.get("path") == null ? "" : objParam.get("path").getAsString();
                        dynamicParameter.read_only = objParam.get("read_only") == null ? "" : objParam.get("read_only").getAsString();
                        dynamicParameter.setting_value = objParam.get("setting_value") == null ? "" : objParam.get("setting_value").getAsString();
                        dynamicParameter.type = objParam.get("type") == null ? "" : objParam.get("type").getAsString();
                        scDynamicParameters.add(dynamicParameter);
                    }
                }
            }
        }
        return scDynamicParameters;
    }

    public boolean checkDuplicate(Long tagId, String name) {
        Tag tag = this.get(tagId);
        String profileSettingDetail = "";
        JsonArray array = new JsonArray();
        if (null != tag.subProfileSetting) {
            profileSettingDetail = tag.subProfileSetting;
            array = new Gson().fromJson(profileSettingDetail, JsonArray.class);
        }
        // check name existed
        int count = 0;
        for (int i = 0; i < array.size(); i++) {
            if (array.get(i).getAsJsonObject().get("sub_title").getAsString().equals(name)) {
                count++;
            }
        }
        return false;
    }

    public void deleteProfileSetting(Long tagId, String subName) {
        Tag tag = this.get(tagId);
        String profileSettingDetail = "";
        JsonArray array = new JsonArray();
        if (subName.contains("@@@root")) {
            tag.profileSetting = null;
            tag.subProfileSetting = null;
        } else {
            if (null != tag.subProfileSetting) {
                profileSettingDetail = tag.subProfileSetting;
                array = new Gson().fromJson(profileSettingDetail, JsonArray.class);
            }
            for (int i = 0; i < array.size(); i++) {
                JsonObject obj = array.get(i).getAsJsonObject();
                if (obj.get("sub_title").getAsString().equals(subName)) {
                    array.remove(obj);
                }
            }
            tag.subProfileSetting = new Gson().toJson(array);
        }
        this.update(tagId, tag);
    }

    public List<ProfileSetting> getListNotSub(String deviceId, Long tagId, String subProfileName, Boolean now) {
        Tag tag = get(tagId);
        String profileSettingDetail = "";
        JsonArray array = new JsonArray();
        if (null != tag.subProfileSetting) {
            profileSettingDetail = tag.subProfileSetting;
            array = new Gson().fromJson(profileSettingDetail, JsonArray.class);
        }
        for (int i = 0; i < array.size(); i++) {
            JsonObject obj = array.get(i).getAsJsonObject();
            if (obj.get("sub_title").getAsString().equals(subProfileName)) {
                JsonArray arrParam = obj.get("parameters").getAsJsonArray();
                for (int j = 0; j < arrParam.size(); j++) {
                    String path = arrParam.get(i).getAsJsonObject().get("path").getAsString();

                }

            }
        }
        return null;
    }

    public String getInforSubProfile(String subProfileName, Long tagId) throws UnsupportedEncodingException {
        Tag tag = this.get(tagId);
        String profileSettingDetail = "";
        JsonArray array = new JsonArray();
        if (null != tag.subProfileSetting) {
            profileSettingDetail = tag.subProfileSetting;
            array = new Gson().fromJson(profileSettingDetail, JsonArray.class);
        }
        for (int i = 0; i < array.size(); i++) {
            JsonObject obj = array.get(i).getAsJsonObject();
            if (obj.get("sub_title").getAsString().equals(subProfileName)) {
                return new Gson().toJson(obj);
            }
        }
        return null;
    }

    public JsonObject parseInput(Long tagId, Set<Parameter> setsParameter) {
        Tag tag = get(tagId);
        JsonObject objectOutPut = new JsonObject();
        objectOutPut.addProperty("id", tag.id);
        if (Strings.isNullOrEmpty(tag.profileSetting)) {
            objectOutPut.addProperty("name", tag.name);
        } else {
            JsonObject obj = new Gson().fromJson(tag.profileSetting, JsonObject.class);
            if (obj.get("alias") != null) {
                objectOutPut.addProperty("alias", obj.get("alias").getAsString());
            } else {
                objectOutPut.addProperty("alias", "");
            }
            objectOutPut.addProperty("name", tag.name);
        }

        JsonArray listSubNotInstance = new JsonArray();
        JsonArray listSubInstance = new JsonArray();

        String profileSettingDetail = "";
        JsonArray array = new JsonArray();
        if (null != tag.subProfileSetting) {
            profileSettingDetail = tag.subProfileSetting;
            array = new Gson().fromJson(profileSettingDetail, JsonArray.class);
        }
        // for lay ra cai sub-profil trong truong subProfileSetting
        for (int i = 0; i < array.size(); i++) {
            JsonObject obj = array.get(i).getAsJsonObject();
            JsonObject o1 = new JsonObject();
            o1.addProperty("sub_title", obj.get("sub_title") == null ? "" : obj.get("sub_title").getAsString());
            o1.addProperty("sub_object_title", obj.get("sub_object_title") == null ? "" : obj.get("sub_object_title").getAsString());
            o1.addProperty("format", obj.get("window_format") == null ? "" : obj.get("window_format").getAsString());
            o1.addProperty("action", obj.get("action") == null ? "" : obj.get("action").getAsString());
            JsonArray arrayParameterInputSubNotInstance = new JsonArray();
            JsonArray arrayParameterInputSubInstance = new JsonArray();
            Set<String> listInstance = new HashSet<>();
            List<Parameter> listParameter = new ArrayList<Parameter>();
            Map<String, JsonObject> mapParam = new HashMap<>();

            boolean isSubNotInstacne = false;
            for (int j = 0; j < obj.getAsJsonArray("parameters").size(); j++) {
                JsonObject param1 = obj.getAsJsonArray("parameters").get(j).getAsJsonObject();
                // format loai 1
                if (!param1.get("path").getAsString().contains(".{i}.")) {
                    isSubNotInstacne = true;
                    for (Parameter parameter : setsParameter) {
                        if (parameter.path.equals(param1.get("path").getAsString())) {
                            arrayParameterInputSubNotInstance.add(new Gson().toJsonTree(convertToParameterSubProfile(parameter, param1, setsParameter)));
                            break;
                        }
                    }
                } // format loai 2
                else {
                    String pattern = param1.get("path").getAsString().replace("{i}", "\\d+");
                    Pattern p = Pattern.compile(pattern);//. represents single character
                    for (Parameter parameter : setsParameter) {
                        Matcher m = p.matcher(parameter.path);
                        boolean b = m.matches();
                        if (b) {
                            listParameter.add(parameter);
                            listInstance.add(parameter.parentObject);
                            mapParam.put(parameter.path, param1);
                        }
                    }
                }
            }
            if (isSubNotInstacne) {
                o1.add("parameters", arrayParameterInputSubNotInstance);
                listSubNotInstance.add(o1);
            } else {
                int ins = 1;
                JsonArray listSubObjecet = new JsonArray();
                for (String parent : listInstance) {
                    JsonObject object = new JsonObject();
                    object.addProperty("instance", ins);
                    object.addProperty("sub_alias", obj.get("sub_object_title").getAsString() + ins++);
                    JsonArray arr = new JsonArray();
                    for (Parameter tmp : listParameter) {
                        if (tmp.parentObject.equals(parent)) {
                            arr.add(new Gson().toJsonTree(convertToParameterSubProfile(tmp, mapParam.get(tmp.path), setsParameter)));
                        }
                    }
                    object.add("list_parameters_in_sub_profile", arr);
                    listSubObjecet.add(object);
                }
                o1.add("list_sub_object", listSubObjecet);
                listSubInstance.add(o1);
            }
        }
        objectOutPut.add("list_sub_not_instance", listSubNotInstance);
        objectOutPut.add("list_sub_instance", listSubInstance);
        return objectOutPut;
    }

    public SCProfileDisplay parseInforToSCProfileDisplayForPolicy(Long tagId, Set<Parameter> setsParameter) {
        Tag tag = get(tagId);
        SCProfileDisplay scProfileConfig = new SCProfileDisplay();
        scProfileConfig.id = tag.id;
        if (Strings.isNullOrEmpty(tag.profileSetting)) {
            scProfileConfig.name = tag.name;
        } else {
            JsonObject obj = new Gson().fromJson(tag.profileSetting, JsonObject.class);
            if (obj.get("alias") != null) {
                scProfileConfig.alias = obj.get("alias").getAsString();
//                objectOutPut.addProperty("alias", obj.get("alias").getAsString());
            } else {
                scProfileConfig.alias = "";
            }
            scProfileConfig.name = tag.name;
        }
//        scProfileConfig.name = tag.name;
        Set<SCSubProfile> listSubNotInstance = new HashSet<>();
        Set<SCSubProfile> listSubInstance = new HashSet<>();

        String profileSettingDetail = "";
        JsonArray array = new JsonArray();
        if (null != tag.subProfileSetting) {
            profileSettingDetail = tag.subProfileSetting;
            array = new Gson().fromJson(profileSettingDetail, JsonArray.class);
        }
        // for lay ra cai sub-profil trong truong subProfileSetting
        for (int i = 0; i < array.size(); i++) {

            JsonObject obj = array.get(i).getAsJsonObject();
            SCSubProfile scSubProfile = new SCSubProfile();
            scSubProfile.action = obj.get("action") == null ? "" : obj.get("action").getAsString();
            scSubProfile.format = obj.get("window_format") == null ? "" : obj.get("window_format").getAsString();
            scSubProfile.sub_object_title = obj.get("sub_object_title") == null ? "" : obj.get("sub_object_title").getAsString();
            scSubProfile.sub_title = obj.get("sub_title") == null ? "" : obj.get("sub_title").getAsString();
            Set<ParameterSubProfile> arrayParameterInputSubNotInstance = new HashSet<>();
            Set<ParameterSubProfile> arrayParameterInputSubInstance = new HashSet<>();
            Set<String> listInstance = new HashSet<>();
            List<Parameter> listParameter = new ArrayList<Parameter>();
            Map<String, JsonObject> mapParam = new HashMap<>();

            boolean isSubNotInstacne = false;
            boolean isContainMutilyInPolicy = false;
            String parentOjbectForInstance = "";
            for (int j = 0; j < obj.getAsJsonArray("parameters").size(); j++) {
                JsonObject param1 = obj.getAsJsonArray("parameters").get(j).getAsJsonObject();
//                if ("policy".equals(module)) {
                int count = StringUtils.countMatches(param1.get("path").getAsString(), "{i}");
                if (count > 1) {
                    isContainMutilyInPolicy = true;
                }
//                }

                // format loai 1
                if (!param1.get("path").getAsString().contains(".{i}.")) {
                    isSubNotInstacne = true;
                    for (Parameter parameter : setsParameter) {
                        if (parameter.path.equals(param1.get("path").getAsString())) {
                            arrayParameterInputSubNotInstance.add((convertToParameterSubProfile(parameter, param1, setsParameter)));
                            break;
                        }
                    }
                } // format loai 2
                else {
                    parentOjbectForInstance = param1.get("path").getAsString().substring(0, param1.get("path").getAsString().lastIndexOf("{i}"));
                    parentOjbectForInstance += "{i}.";
                    String pattern = param1.get("path").getAsString().replace("{i}", "\\d+");
                    Pattern p = Pattern.compile(pattern);//. represents single character
                    for (Parameter parameter : setsParameter) {
                        Matcher m = p.matcher(parameter.path);
                        boolean b = m.matches();
                        if (b) {
                            listParameter.add(parameter);
                            listInstance.add(parameter.parentObject);
                            mapParam.put(parameter.path, param1);
                        }
                    }
                }
            }

            if (isContainMutilyInPolicy) {
                continue;
            }

            if (isSubNotInstacne) {
                scSubProfile.parameters = arrayParameterInputSubNotInstance;
                listSubNotInstance.add(scSubProfile);
            } else {
                SCObjectInstanceInSubProfile sCObjectInstanceInSubProfiles = new SCObjectInstanceInSubProfile();
                Set<ParameterSubProfile> parameterInSubProfiles = new HashSet<>();
                for (String parent : listInstance) {
//                    SCObjectInstanceInSubProfile sCObjectInstanceInSubProfile = new SCObjectInstanceInSubProfile();
//                    sCObjectInstanceInSubProfile.parentObject = parentOjbectForInstance;
                    for (Parameter tmp : listParameter) {
                        if (tmp.parentObject.equals(parent)) {
                            parameterInSubProfiles.add((convertToParameterSubProfile(tmp, mapParam.get(tmp.path), setsParameter)));
                        }
                    }
//                    sCObjectInstanceInSubProfile.list_parameters_in_sub_profile = parameterInSubProfiles;
                    scSubProfile.parentObject = parentOjbectForInstance;
                    break;
                }
                scSubProfile.list_parameters_in_sub_profile = parameterInSubProfiles;
                listSubInstance.add(scSubProfile);
            }
        }
        scProfileConfig.list_sub_instance = listSubInstance;
        scProfileConfig.list_sub_not_instance = listSubNotInstance;
        return scProfileConfig;
    }

    public SCProfileDisplay parseInforToSCProfileDisplay(Long tagId, Set<Parameter> setsParameter, String module) {
        Tag tag = get(tagId);
        SCProfileDisplay scProfileConfig = new SCProfileDisplay();
        scProfileConfig.id = tag.id;
        if (Strings.isNullOrEmpty(tag.profileSetting)) {
            scProfileConfig.name = tag.name;
        } else {
            JsonObject obj = new Gson().fromJson(tag.profileSetting, JsonObject.class);
            if (obj.get("alias") != null) {
                scProfileConfig.alias = obj.get("alias").getAsString();
//                objectOutPut.addProperty("alias", obj.get("alias").getAsString());
            } else {
                scProfileConfig.alias = "";
            }
            scProfileConfig.name = tag.name;
        }

//        scProfileConfig.name = tag.name;
        Set<SCSubProfile> listSubNotInstance = new LinkedHashSet<>();
        Set<SCSubProfile> listSubInstance = new LinkedHashSet<>();

        String profileSettingDetail = "";
        JsonArray array = new JsonArray();
        if (null != tag.subProfileSetting) {
            profileSettingDetail = tag.subProfileSetting;
            array = new Gson().fromJson(profileSettingDetail, JsonArray.class);
        }
        // for lay ra cai sub-profil trong truong subProfileSetting
        for (int i = 0; i < array.size(); i++) {

            JsonObject obj = array.get(i).getAsJsonObject();
            SCSubProfile scSubProfile = new SCSubProfile();
            scSubProfile.list_sub_object = new ArrayList<>();
            scSubProfile.action = obj.get("action") == null ? "" : obj.get("action").getAsString();
            scSubProfile.format = obj.get("window_format") == null ? "" : obj.get("window_format").getAsString();
            scSubProfile.sub_object_title = obj.get("sub_object_title") == null ? "" : obj.get("sub_object_title").getAsString();
            scSubProfile.sub_title = obj.get("sub_title") == null ? "" : obj.get("sub_title").getAsString();
            Set<ParameterSubProfile> arrayParameterInputSubNotInstance = new LinkedHashSet<>();
            Set<ParameterSubProfile> arrayParameterInputSubInstance = new LinkedHashSet<>();
            Set<String> listInstance = new LinkedHashSet<>();
            Set<Parameter> listParameter = new LinkedHashSet<>();
            Map<String, JsonObject> mapParam = new LinkedHashMap<>();

            boolean isSubNotInstacne = false;
            long position = 1;
            for (int j = 0; j < obj.getAsJsonArray("parameters").size(); j++) {
                JsonObject param1 = obj.getAsJsonArray("parameters").get(j).getAsJsonObject();

                // format loai 1
                if (!param1.get("path").getAsString().contains(".{i}.")) {
                    isSubNotInstacne = true;
                    for (Parameter parameter : setsParameter) {
                        if (parameter.path.equals(param1.get("path").getAsString())) {
                            ParameterSubProfile parameterSubProfile = convertToParameterSubProfile(parameter, param1, setsParameter);
                            parameterSubProfile.position = position;
                            position++;
                            arrayParameterInputSubNotInstance.add(parameterSubProfile);
                            break;
                        }
                    }
                } // format loai 2
                else {
                    String pattern = param1.get("path").getAsString().replace("{i}", "\\d+");
                    Pattern p = Pattern.compile(pattern);//. represents single character
                    for (Parameter parameter : setsParameter) {
                        Matcher m = p.matcher(parameter.path);
                        boolean b = m.matches();
                        if (b) {
                            listParameter.add(parameter);
                            listInstance.add(parameter.parentObject);
                            mapParam.put(parameter.path, param1);
                        }
                    }
                }
            }

            if (isSubNotInstacne) {
                scSubProfile.parameters = arrayParameterInputSubNotInstance;
                listSubNotInstance.add(scSubProfile);
            } else {
                int ins = 1;
                List<String> orderParents = new ArrayList<>(listInstance);
                Collections.sort(orderParents, new Comparator<String>() {
                    public int compare(String o1, String o2) {
                        return extractInt(o1) - extractInt(o2);
                    }

                    int extractInt(String s) {
                        String num = s.replaceAll("\\D", "");
                        // return 0 if no digits found
                        return num.isEmpty() ? 0 : Integer.parseInt(num);
                    }
                });

                for (String parent : orderParents) {
                    Set<ParameterSubProfile> parameterInSubProfiles = new LinkedHashSet<>();
                    SCObjectInstanceInSubProfile sCObjectInstanceInSubProfile = new SCObjectInstanceInSubProfile();
//                    sCObjectInstanceInSubProfile.instance = ins;
//                    int next = ins++;
//                    sCObjectInstanceInSubProfile.sub_alias = obj.get("sub_object_title") == null ? String.valueOf(next) : obj.get("sub_object_title").getAsString() + next;
                    String[] arr = parent.split("\\.");
                    try {
                        if (Integer.valueOf(arr[arr.length - 1]) != -1) {
                            sCObjectInstanceInSubProfile.sub_alias = obj.get("sub_object_title") == null ? String.valueOf(Integer.valueOf(arr[arr.length - 1])) : obj.get("sub_object_title").getAsString() + Integer.valueOf(arr[arr.length - 1]);
                        }
                    } catch (Exception e) {
                        sCObjectInstanceInSubProfile.sub_alias = obj.get("sub_object_title") == null ? "" : obj.get("sub_object_title").getAsString();
                    }
                    for (Parameter tmp : listParameter) {
                        if (tmp.parentObject.equals(parent)) {
                            ParameterSubProfile parameterSubProfile = convertToParameterSubProfile(tmp, mapParam.get(tmp.path), setsParameter);
                            parameterSubProfile.position = position;
                            position++;
                            parameterInSubProfiles.add(parameterSubProfile);
                        }
                    }
                    sCObjectInstanceInSubProfile.list_parameters_in_sub_profile = parameterInSubProfiles;
                    scSubProfile.list_sub_object.add(sCObjectInstanceInSubProfile);
                }
                listSubInstance.add(scSubProfile);
            }
        }
        scProfileConfig.list_sub_instance = listSubInstance;
        scProfileConfig.list_sub_not_instance = listSubNotInstance;
        return scProfileConfig;
    }

    private ParameterSubProfile convertToParameterSubProfile(Parameter parameter, JsonObject object, Set<Parameter> setParameter) {
        ParameterSubProfile param = new ParameterSubProfile();
        param.path = parameter.path;
        param.subPath = parameter.subPath;
        param.shortName = parameter.shortName;
        param.dataType = parameter.dataType;
        param.value = parameter.value;
        param.defaultValue = parameter.defaultValue;
        param.rule = parameter.rule;
        param.inputType = parameter.inputType;
        param.useSubscriberData = parameter.useSubscriberData;
        param.tr069Name = parameter.tr069Name;
        param.access = parameter.access;
        param.parentObject = parameter.parentObject;
        param.subscriberData = parameter.subscriberData;
        param.instance = parameter.instance;
        param.tr069ParentObject = parameter.tr069ParentObject;
        param.alias = object.get("alias") == null ? "" : object.get("alias").getAsString();
        param.readOnly = object.get("read_only") == null ? "" : object.get("read_only").getAsString();
        param.type = object.get("type") == null ? "" : object.get("type").getAsString();
        param.isPointer = object.get("is_pointer") == null ? "false" : object.get("is_pointer").getAsString();

        Set<String> listParentObject = new LinkedHashSet<>();
        Set<Parameter> listParameter = new LinkedHashSet<>();
        Map<Integer, Parameter> mapInstanceSettingValue = new HashMap<>();

        List<String> arrSettingValue = new ArrayList<>();
        List<String> arrSettingName = new ArrayList<>();

        if (object.get("is_pointer").getAsString().equals("true") || object.get("is_pointer").getAsString().equals("on")) {
            String settingValue = object.get("setting_value") != null ? object.get("setting_value").getAsString() : "";
            int validateTemplate = validateValueSetting(settingValue, setParameter);
            switch (validateTemplate) {
                case -1:
                    param.settingValue = "";
                    break;
                case 0:
                    // Th setting value A.{i}.B.{i}
                    String pattern = settingValue.replace("{i}", "\\d+");
                    Pattern p = Pattern.compile(pattern);//. represents single character  
                    for (Parameter tmp : setParameter) {
                        Matcher m = p.matcher(tmp.path);
                        boolean yes = m.matches();
                        if (yes) {
                            listParameter.add(tmp);
                            listParentObject.add(tmp.parentObject);
                            String arr[] = tmp.parentObject.split("\\.");
                            try {
                                mapInstanceSettingValue.put(Integer.valueOf(arr[arr.length - 1]), tmp);
//                            listInstance.add(Integer.valueOf(arr[arr.length - 1]));
                            } catch (Exception e) {
                                System.out.println("error convert instance");
                            }
                        }
                    }
                    if (object.get("filter").getAsJsonArray() != null && object.get("filter").getAsJsonArray().size() > 0) {
                        listParameter = doFilter(object.get("filter").getAsJsonArray(), setParameter, mapInstanceSettingValue);
                    }

                    for (String parent : listParentObject) {
                        for (Parameter tmp : listParameter) {
                            if (tmp.parentObject.equals(parent)) {
                                arrSettingValue.add(tmp.value);
                            }
                        }
                    }
                    param.settingValue = generateOutPutSettingvalue(arrSettingValue);
                    break;
                case 1:
                    String pattern1 = "(.*?)\\[(.*?)\\](.*?)";
                    Pattern r1 = Pattern.compile(pattern1);
                    Matcher m1 = r1.matcher(settingValue);
                    List<String> arrValueTmp = new ArrayList<>();
                    List<String> arrValueFinal = new ArrayList<>();
                    if (m1.find()) {
                        String paramInTemplate = m1.group(2);
                        String patternI = paramInTemplate.replace("{i}", "\\d+");
                        Pattern pI = Pattern.compile(patternI);//. represents single character  
                        for (Parameter tmp : setParameter) {
                            Matcher m = pI.matcher(tmp.path);
                            boolean yes = m.matches();
                            if (yes) {
                                arrValueTmp.add(tmp.path);
                                String arr[] = tmp.parentObject.split("\\.");
                                try {
                                    mapInstanceSettingValue.put(Integer.valueOf(arr[arr.length - 1]), tmp);
//                            listInstance.add(Integer.valueOf(arr[arr.length - 1]));
                                } catch (Exception e) {
                                    System.out.println("error convert instance");
                                }
                            }
                        }
                        if (object.get("filter").getAsJsonArray() != null && object.get("filter").getAsJsonArray().size() > 0) {
                            listParameter = doFilter(object.get("filter").getAsJsonArray(), setParameter, mapInstanceSettingValue);
                        }

                        mapInstanceSettingValue.clear();

                        List<String> arr2 = new ArrayList<>();
                        for (Parameter pr : listParameter) {
                            for (String tmp : arrValueTmp) {
                                if (pr.path.equals(tmp)) {
                                    arrValueFinal.add(pr.value);
                                    String arr[] = pr.parentObject.split("\\.");
                                    try {
                                        mapInstanceSettingValue.put(Integer.valueOf(arr[arr.length - 1]), pr);
                                    } catch (Exception e) {
                                        System.out.println("error convert instance");
                                    }
                                }
                            }
                        }

                        for (String tmp : arrValueFinal) {
                            String replace = settingValue.replace("[" + paramInTemplate + "]", tmp);
                            arr2.add(replace);
                        }

                        param.settingValue = generateOutPutSettingvalue(arr2);
                    }

                    break;
                case 2:
                    String pattern2 = "ref:(.*?)\\[(.*?)\\](.*?)";
                    Pattern r2 = Pattern.compile(pattern2);
                    Matcher m2 = r2.matcher(settingValue);
                    List<String> arrValueTmp2 = new ArrayList<>();
                    List<String> arrValueFinal2 = new ArrayList<>();
                    if (m2.find()) {
                        String paramInTemplate = m2.group(2);
                        String patternI = paramInTemplate.replace("{i}", "\\d+");
                        Pattern pI = Pattern.compile(patternI);//. represents single character  
                        for (Parameter tmp : setParameter) {
                            Matcher m = pI.matcher(tmp.path);
                            boolean yes = m.matches();
                            if (yes) {
                                arrValueTmp2.add(tmp.path);
                                try {
                                    String arr[] = tmp.parentObject.split("\\.");
                                    mapInstanceSettingValue.put(Integer.valueOf(arr[arr.length - 1]), tmp);
                                } catch (Exception e) {
                                    System.out.println("error convert instance");
                                }
                            }
                        }
                        if (object.get("filter").getAsJsonArray() != null && object.get("filter").getAsJsonArray().size() > 0) {
                            listParameter = doFilter(object.get("filter").getAsJsonArray(), setParameter, mapInstanceSettingValue);
                        }

                        mapInstanceSettingValue.clear();

                        List<String> arr2 = new ArrayList<>();
                        for (Parameter pr : listParameter) {
                            for (String tmp : arrValueTmp2) {
                                if (pr.path.equals(tmp)) {
                                    arrValueFinal2.add(pr.value);

                                    String arr[] = pr.parentObject.split("\\.");
                                    try {
                                        mapInstanceSettingValue.put(Integer.valueOf(arr[arr.length - 1]), pr);
                                    } catch (Exception e) {
                                        System.out.println("error convert instance");
                                    }

                                }
                            }
                        }

                        for (String tmp : arrValueFinal2) {
                            String replace = settingValue.replace("[" + paramInTemplate + "]", tmp).replace("ref:", "");
                            arr2.add(replace);
                        }

                        List<String> arr3 = new ArrayList<>();
                        for (Parameter pr : listParameter) {
                            for (String tmp : arr2) {
                                if (pr.path.equals(tmp)) {
                                    arr3.add(pr.value);
                                }
                            }
                        }

                        param.settingValue = generateOutPutSettingvalue(arr3);
                    }
                    break;

                case 3:
                    for (Parameter tmp : setParameter) {
                        if (tmp.path.equals(settingValue)) {
                            param.settingValue = tmp.value;
                        }
                    }
                    break;
                case 4:
                    param.settingValue = settingValue;
                    break;
                default:
                    break;
            }
        } else {
            if (param.type.equals("combo-box")) {
                List<String> lstData = new ArrayList<>();
                if (object.get("setting_value") != null && object.get("setting_value").getAsString().contains(",")) {
                    String[] arr = object.get("setting_value").getAsString().split(",");
                    lstData.addAll(Arrays.asList(arr));
                } else {
                    lstData.add(object.get("setting_value").getAsString());
                }

                param.settingValue = generateOutPutSettingvalue(lstData);
                // bo sung them rule cho filter va display

            } else {
                param.settingValue = object.get("setting_value") == null ? "" : object.get("setting_value").getAsString();
            }
        }

        Set<String> listParentObjectDisplay = new LinkedHashSet<>();
        Set<Parameter> listParameterDisplay = new LinkedHashSet<>();
        Map<Integer, Parameter> mapInstanceDisplay = new HashMap<>();
        String displayKey = object.get("display_key") != null ? object.get("display_key").getAsString() : "";
        int validateTemplate = validateValueSetting(displayKey, setParameter);
        switch (validateTemplate) {
            case -1:
                param.settingName = "";
                break;
            case 0:
                //mapInstanceSettingValue
                // Th setting value A.{i}.B.{i}
                for (Map.Entry<Integer, Parameter> entry : mapInstanceSettingValue.entrySet()) {
                    String path = object.get("display_key").getAsString().replace("{i}", entry.getKey().toString());
                    for (Parameter tmp : setParameter) {
                        if (tmp.path.equals(path)) {
                            arrSettingName.add(tmp.value);
                        }
                    }
                }
//
//                String pattern = object.get("display_key").getAsString().replace("{i}", "\\d+");
//                Pattern p = Pattern.compile(pattern);//. represents single character  
//                for (Parameter tmp : setParameter) {
//                    Matcher m = p.matcher(tmp.path);
//                    boolean yes = m.matches();
//                    if (yes) {
//                        listParameterDisplay.add(tmp);
//                        listParentObjectDisplay.add(tmp.parentObject);
//                        String arr[] = tmp.parentObject.split("\\.");
//                        try {
//                            mapInstanceDisplay.put(Integer.valueOf(arr[arr.length - 1]), tmp);
//                        } catch (Exception e) {
//                            System.out.println("error convert instance");
//                        }
//                    }
//                }
//
//                if (object.get("filter").getAsJsonArray() != null && object.get("filter").getAsJsonArray().size() > 0) {
//                    listParameterDisplay = doFilter(object.get("filter").getAsJsonArray(), setParameter, mapInstanceDisplay);
//                }
//
//                for (String parent : listParentObjectDisplay) {
//                    for (Parameter tmp : listParameterDisplay) {
//                        if (tmp.parentObject.equals(parent)) {
//                            arrSettingName.add(tmp.value);
//                        }
//                    }
//                }

                param.settingName = generateOutPutSettingvalue(arrSettingName);
                break;
            case 1:

                String pattern1 = "(.*?)\\[(.*?)\\](.*?)";
                Pattern r1 = Pattern.compile(pattern1);
                Matcher m1 = r1.matcher(displayKey);
                List<String> arrValueTmp = new ArrayList<>();
                List<String> arrValueFinal = new ArrayList<>();
                List<String> arr2 = new ArrayList<>();
                if (m1.find()) {
                    String paramInTemplate = m1.group(2);
                    String patternI = paramInTemplate.replace("{i}", "\\d+");
                    Pattern pI = Pattern.compile(patternI);//. represents single character  
                    for (Parameter tmp : setParameter) {
                        Matcher m = pI.matcher(tmp.path);
                        boolean yes = m.matches();
                        if (yes) {
                            for (Map.Entry<Integer, Parameter> entry : mapInstanceSettingValue.entrySet()) {
                                String path = paramInTemplate.replace("{i}", entry.getKey().toString());
                                if (tmp.path.equals(path)) {
                                    String path2 = displayKey.replace("[" + paramInTemplate + "]", tmp.value);
                                    arr2.add(path2);
                                }
                            }
                        }
                    }

//                    List<String> arr2 = new ArrayList<>();
//                    for (Parameter pr : listParameterDisplay) {
//                        for (String tmp : arrValueTmp) {
//                            if (pr.path.equals(tmp)) {
//                                arrValueFinal.add(pr.value);
//                            }
//                        }
//                    }
//
//                    for (String tmp : arrValueFinal) {
//                        String replace = displayKey.replace("[" + paramInTemplate + "]", tmp);
//                        arr2.add(replace);
//                    }
                    param.settingName = generateOutPutSettingvalue(arr2);
                }
                break;
            case 2:
                String pattern2 = "ref:(.*?)\\[(.*?)\\](.*?)";
                Pattern r2 = Pattern.compile(pattern2);
                Matcher m2 = r2.matcher(displayKey);
                List<String> arr3 = new ArrayList<>();
                if (m2.find()) {
                    String paramInTemplate = m2.group(2);
                    String patternI = paramInTemplate.replace("{i}", "\\d+");
                    Pattern pI = Pattern.compile(patternI);//. represents single character  
                    for (Parameter tmp : setParameter) {
                        Matcher m = pI.matcher(tmp.path);
                        boolean yes = m.matches();
                        if (yes) {
                            for (Map.Entry<Integer, Parameter> entry : mapInstanceSettingValue.entrySet()) {
                                String path = paramInTemplate.replace("{i}", entry.getKey().toString());
                                if (tmp.path.equals(path)) {
                                    String path2 = displayKey.replace("[" + paramInTemplate + "]", tmp.value).replace("ref:", "");
                                    System.out.println(path2);
                                    for (Parameter tmp2 : setParameter) {
                                        if (path2.equals(tmp2.path)) {
                                            arr3.add(tmp2.value);
                                        }
                                    }
                                }
                            }
                        }
                    }

                    param.settingName = generateOutPutSettingvalue(arr3);
                }
                break;
            case 3:
                for (Parameter tmp : setParameter) {
                    if (tmp.path.equals(displayKey)) {
                        param.settingName = tmp.value;
                    }
                }
                break;
            case 4:
                param.settingName = displayKey;
                break;
            default:
                break;
        }

        return param;
    }

    private int validateValueSetting(String valueNeedValiadte, Set<Parameter> setParameter) {
        // == 0   :   contains i
        // == 1   :  belong A.B.[xxx].C.E
        // == 2   :  belong ref:A.B.[xxx].C.E
        // == 3   :  belong A.B.C
        // == 4   :  nhap linh tinh
        // == -1  : error
        if (Strings.isNullOrEmpty(valueNeedValiadte)) {
            return -1;
        }

        // check template is    ref:A.B.[xxx].C.E;
        String pattern2 = "ref:(.*?)\\[(.*?)\\](.*?)";
        Pattern r2 = Pattern.compile(pattern2);
        Matcher m2 = r2.matcher(valueNeedValiadte);
        if (m2.find()) {
            return 2;
        }

        // check template is     A.B.[xxx].C.E;
        String pattern1 = "(.*?)\\[(.*?)\\](.*?)";
        Pattern r1 = Pattern.compile(pattern1);
        Matcher m1 = r1.matcher(valueNeedValiadte);
        if (m1.find()) {
            return 1;
        }

        if (valueNeedValiadte.contains("{i}")) {
            return 0;
        }

        // TH setting value is a A.B.C
        boolean isParam = false;
        for (Parameter tmp : setParameter) {
            if (tmp.path.equals(valueNeedValiadte)) {
                isParam = true;
//                param.settingValue = tmp.value;
            }
        }
        // Th setting value la tham so nhap linh tinh
        if (!isParam) {
            return 3;
        } else {
            return 4;
        }

    }

    private Set<Parameter> doFilter(JsonArray arrFilter, Set<Parameter> setParameter, Map<Integer, Parameter> mapInstaceSettingValue) {
//        JsonArray arrFilter = object.get("filter").getAsJsonArray();
        Set<Parameter> paramters = new LinkedHashSet<>();
        if (!mapInstaceSettingValue.isEmpty()) {
            for (int i = 0; i < arrFilter.size(); i++) {
                Set<Parameter> setParameterFilter = new LinkedHashSet<>();
                JsonObject object = arrFilter.get(i).getAsJsonObject();

                for (Map.Entry<Integer, Parameter> entry : mapInstaceSettingValue.entrySet()) {
                    if (object.get("filter_key").getAsString().equals("")) {
                        paramters.add(entry.getValue());
                    } else if (object.get("filter_key").getAsString().contains("{i}")) {
                        String path = object.get("filter_key").getAsString().replace("{i}", entry.getKey().toString());
                        String value = "";
                        for (Parameter tmp : setParameter) {
                            if (path.equals(tmp.path)) {
                                value = tmp.value;
                            }
                        }
                        String operator = object.get("filter_type").getAsString();
                        String filterValue = object.get("filter_value").getAsString();
                        boolean isRight = false;
                        if (operator.equals("Ct")) {
                            if (value.contains(filterValue)) {
                                isRight = true;
                            }
                        } else if (operator.equals("Less")) {
                            if (Float.valueOf(value) > Float.valueOf(filterValue)) {
                                isRight = true;
                            }
                        } else if (operator.equals("Greater")) {
                            if (Float.valueOf(value) > Float.valueOf(filterValue)) {
                                isRight = true;
                            }
                        } else if (operator.equals("Eq")) {
                            if (value.equals(filterValue)) {
                                isRight = true;
                            }
                        } else if (operator.equals("Neq")) {
                            if (!value.equals(filterValue)) {
                                isRight = true;
                            }
                        }
                        if (isRight) {
                            setParameterFilter.add(entry.getValue());
                        }
                    }
                }
                // xu ly dieu kien AND
                if (paramters.isEmpty()) {
                    paramters.addAll(setParameterFilter);
                } else {
                    paramters.retainAll(setParameterFilter);
                }
            }
        }
        return paramters;

    }

    public boolean editProflieAlias(Long tagId, String profileAlias, String profile_display, String number) {
        try {
            Tag tag = get(tagId);
            JsonObject object = new JsonObject();
            object.addProperty("alias", profileAlias);
            object.addProperty("isOther", profile_display.equals("tab") ? "false" : "true");
            object.addProperty("number", number);
            tag.profileSetting = new Gson().toJson(object);
            this.update(tagId, tag);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public SCSettingProfile getProfileALias(Long tagId) {
        SCSettingProfile scSetting = new SCSettingProfile();
        Tag tag = get(tagId);
        JsonObject object = new Gson().fromJson(tag.profileSetting, JsonObject.class);
        if (object != null) {
            scSetting.alias = object.get("alias") == null ? "" : object.get("alias").getAsString();
            scSetting.isOther = object.get("isOther") == null ? "" : object.get("isOther").getAsString();
            scSetting.number = object.get("number") == null ? "" : object.get("number").getAsString();
        }
        return scSetting;
    }

    private String generateOutPutSettingvalue(List<String> listString) {
        String s1 = "[";
        String e1 = "]";
        String content = s1;
        for (String tmp : listString) {
            content += tmp + ";";
        }
        content = content.substring(0, content.length() - 1);
        content += e1;
        return content;
    }

    public boolean checkNumberExited(Long tagId, String number) {
        Tag tag = get(tagId);
        List<Tag> tags = (List<Tag>) getListProfilesOfVersion(tag.deviceTypeVersionId);
        Set<String> setNumber = new HashSet<String>();
        for (Tag tmp : tags) {
            // chi set truong hop cac profile khac  profile dang xet
            if (!tmp.id.equals(tagId)) {
                if (!Strings.isNullOrEmpty(tmp.profileSetting)) {
                    JsonObject settingObj = new Gson().fromJson(tmp.profileSetting, JsonObject.class);
                    if (settingObj.get("number") != null) {
                        setNumber.add(settingObj.get("number").getAsString());
                    }
                }
            }
        }

        for (String tmp : setNumber) {
            if (tmp.equals(number)) {
                return true;
            }
        }

        return false;
    }

    public boolean checkDuplicateAlias(Long tagId, String alias) {
        Tag tag = get(tagId);
        List<Tag> tags = (List<Tag>) getListProfilesOfVersion(tag.deviceTypeVersionId);
        Set<String> setNumber = new HashSet<String>();
        for (Tag tmp : tags) {
            // chi set truong hop cac profile khac  profile dang xet
            if (!tmp.id.equals(tagId)) {
                if (!Strings.isNullOrEmpty(tmp.profileSetting)) {
                    JsonObject settingObj = new Gson().fromJson(tmp.profileSetting, JsonObject.class);
                    if (settingObj.get("alias") != null) {
                        setNumber.add(settingObj.get("alias").getAsString());
                    }
                }
            }
        }

        for (String tmp : setNumber) {
            if (tmp.equals(alias)) {
                return true;
            }
        }

        return false;
    }

    public Map<String, Parameter> findDuplicateBetween2Map(Map<String, Parameter> map1, Map<String, Parameter> map2) {
        Map<String, Parameter> finalMap = new HashMap<>();
        for (Map.Entry<String, Parameter> entry : map1.entrySet()) {
            if (map2.containsKey(entry.getKey())) {
                finalMap.put(entry.getKey(), entry.getValue());
            }
        }
        return finalMap;
    }

    public Map<String, Parameter> getDuplicateInDeviceTypeVersions(List<DeviceTypeVersion> deviceTypeVersions) {
        Map<String, Parameter> finalMap = new HashMap<>();

        Map<String, Parameter> data = new HashMap<>();
        List<Tag> tagsi = getListByCorrespondingModule(deviceTypeVersions.get(0).id, "policy");
        for (Tag tag : tagsi) {
            data.putAll(tag.parameters);
        }

        for (int i = 0; i < deviceTypeVersions.size(); i++) {
            if (i + 1 < deviceTypeVersions.size()) {
                Map<String, Parameter> dataTmp = new HashMap<>();
                List<Tag> tagsj = getListByCorrespondingModule(deviceTypeVersions.get(i + 1).id, "policy");
                for (Tag tag : tagsj) {
                    if (tag.parameters != null) {
                        dataTmp.putAll(tag.parameters);
                    }
                }
                finalMap = findDuplicateBetween2Map(data, dataTmp);
                data = finalMap;
            }
        }
        return finalMap;
    }

    public boolean checkParameterExistedInListDeviceTypeVersios(Map<String, Parameter> parameter, String tr069Paramter) {
//        for (DeviceTypeVersion deviceTypeVersionTmp : deviceTypeVersions) {
//            boolean isBelong = false;
//            List<Tag> tags = getListByCorrespondingModule(deviceTypeVersionTmp.id, module);
//            Map<String, Parameter> map = new HashMap<>();
//            for (Tag tag : tags) {
//                if (!tag.id.equals(currentId))
//                map.putAll(tag.parameters);
//            }
//            Map<String, Parameter> map = new HashMap<>();
//
//            for (DeviceTypeVersion tag : deviceTypeVersions) {
//                map.putAll(tag.parameters);
//            }

        for (Map.Entry<String, Parameter> entry : parameter.entrySet()) {
            if (tr069Paramter.equals(entry.getValue().tr069Name)) {
                return true;
            }
        }
//            if (!isBelong) {
//                return false;
//            }
//        }
        return false;
    }

    // false - > not equal
    // true  - >  equal
    public boolean compareSCSubProfile(SCSubProfile sc1, SCSubProfile sc2) {
        if (sc1 == null || sc2 == null) {
            return true;
        }
        if (sc1.parameters != null) {
            for (ParameterSubProfile tmp1 : sc1.parameters) {
                boolean isBelong = false;
                // so sanh tmp1 co nam trong list cua sc2 khong
                for (ParameterSubProfile tmp2 : sc2.parameters) {
                    if (tmp1.tr069Name.equals(tmp2.tr069Name)) {
                        isBelong = true;
                        break;
                    }
                }
                // neu isbelong = false -> khong nam trong list
                if (!isBelong) {
                    return false;
                }
            }
        }

        if (sc1.list_parameters_in_sub_profile != null) {
            for (ParameterSubProfile tmp1 : sc1.list_parameters_in_sub_profile) {
                boolean isBelong = false;
                for (ParameterSubProfile tmp2 : sc2.list_parameters_in_sub_profile) {
                    if (tmp1.tr069Name.equals(tmp2.tr069Name)) {
                        isBelong = true;
                        break;
                    }
                }
                if (!isBelong) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean isBelongSubProfile(Set<SCSubProfile> subProfiles, SCSubProfile sc1) {
        if (subProfiles.isEmpty()) {
            return false;
        }

        for (SCSubProfile tmp : subProfiles) {
            if (compareSCSubProfile(tmp, sc1)) {
                return true;
            }
        }
        return false;
    }

    public void compareSCSubProfile(Set<SCSubProfile> s1, Set<SCSubProfile> s2) {
        Iterator<SCSubProfile> iter1 = s2.iterator();
        while (iter1.hasNext()) {
            SCSubProfile el1 = iter1.next();
            if (isBelongSubProfile(s1, el1)) {
                iter1.remove();
            }
        }
    }

    public boolean checkDuplicateSubProfileInSub(List<SCProfileDisplay> scProfileConfigs, Set<SCSubProfile> sc) {
        for (SCProfileDisplay scProfile : scProfileConfigs) {
            for (SCSubProfile scSubProfile : scProfile.list_sub_instance) {
                for (SCSubProfile sc2 : sc) {
                    if (scSubProfile.parameters != null) {
                        for (ParameterSubProfile tmp1 : scSubProfile.parameters) {
                            boolean isBelong = false;
                            for (ParameterSubProfile tmp2 : sc2.parameters) {
                                if (tmp1.tr069Name.equals(tmp2.tr069Name)) {
                                    isBelong = true;
                                    break;
                                }
                            }

                            if (!isBelong) {
                                return true;
                            }
                        }
                    }

                    if (scSubProfile.list_parameters_in_sub_profile != null) {
                        for (ParameterSubProfile tmp1 : scSubProfile.list_parameters_in_sub_profile) {
                            boolean isBelong = false;
                            for (ParameterSubProfile tmp2 : sc2.list_parameters_in_sub_profile) {
                                if (tmp1.tr069Name.equals(tmp2.tr069Name)) {
                                    isBelong = true;
                                    break;
                                }
                            }

                            if (!isBelong) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;

    }

    public boolean checkDuplicaterSubProfileInNotSub(List<SCProfileDisplay> scProfileConfigs, Set<SCSubProfile> sc) {
        for (SCProfileDisplay scProfile : scProfileConfigs) {
            for (SCSubProfile scSubProfile : scProfile.list_sub_not_instance) {
                for (SCSubProfile sc2 : sc) {

                    if (scSubProfile.parameters != null) {
                        for (ParameterSubProfile tmp1 : scSubProfile.parameters) {
                            boolean isBelong = false;
                            for (ParameterSubProfile tmp2 : sc2.parameters) {
                                if (tmp1.tr069Name.equals(tmp2.tr069Name)) {
                                    isBelong = true;
                                    break;
                                }
                            }
                            if (!isBelong) {
                                return true;
                            }
                        }
                    }

                    if (scSubProfile.list_parameters_in_sub_profile != null) {
                        for (ParameterSubProfile tmp1 : scSubProfile.list_parameters_in_sub_profile) {
                            boolean isBelong = false;
                            for (ParameterSubProfile tmp2 : sc2.list_parameters_in_sub_profile) {
                                if (tmp1.tr069Name.equals(tmp2.tr069Name)) {
                                    isBelong = true;
                                    break;
                                }
                            }
                            if (!isBelong) {
                                return true;
                            }
                        }
                    }

                }

            }
        }
        return false;

    }

    public SCProfileDisplay removeDuplicateSubInProfile(SCProfileDisplay scProfileConfig) {

        if (scProfileConfig.list_sub_instance != null) {
            Set<SCSubProfile> finalSet = new HashSet<>();
            for (SCSubProfile sc : scProfileConfig.list_sub_instance) {
                if (!isBelongSubProfile(finalSet, sc)) {
                    finalSet.add(sc);
                }
            }
            scProfileConfig.list_sub_instance = finalSet;
        }

        if (scProfileConfig.list_sub_not_instance != null) {
            Set<SCSubProfile> finalSet = new HashSet<>();
            for (SCSubProfile sc : scProfileConfig.list_sub_not_instance) {
                if (!isBelongSubProfile(finalSet, sc)) {
                    finalSet.add(sc);
                }
            }
            scProfileConfig.list_sub_not_instance = finalSet;
        }

        return scProfileConfig;
    }

    public Map<String, String> validateSettingParameter(Long tagId, Map<String, String> mapParam, String type) {
        Map<String, String> mapError = new HashMap<>();
        // 1 _ null or empty
        // 2_ duplicate
        // 3_ wrong type
        // 4 _max length
        // validate path
        if (Strings.isNullOrEmpty(mapParam.get("path"))) {
            mapError.put("path_error", "1");
        } else {
            Tag tag = get(tagId);

            String profileSettingDetail = "";
            JsonArray array = new JsonArray();
            if (null != tag.subProfileSetting) {
                profileSettingDetail = tag.subProfileSetting;
                array = new Gson().fromJson(profileSettingDetail, JsonArray.class);
            }

            if (type.equals("edit")) {
                String tmp = mapParam.get("path");
                if (mapParam.get("path").contains("_notchanged")) {
                    tmp = tmp.replaceAll("_notchanged", "");
                }
                tmp = tmp.replaceAll("\\.\\d+\\.", ".{i}.");
                boolean isBelong = false;
                for (Map.Entry<String, Parameter> entry : tag.parameters.entrySet()) {
                    if (tmp.equals(entry.getValue().tr069Name)) {
                        isBelong = true;
                    }
                }
                if (!isBelong) {
                    mapError.put("path_error", "3");
                }
            } else {
                String tmp = mapParam.get("path");
                tmp = tmp.replaceAll("\\.\\d+\\.", ".{i}.");
                boolean isBelong = false;
                for (Map.Entry<String, Parameter> entry : tag.parameters.entrySet()) {
                    // dang dung parameter
                    if (tmp.equals(entry.getValue().tr069Name)) {
                        isBelong = true;
                    }
                }
                if (!isBelong) {
                    mapError.put("path_error", "3");
                }
            }

            for (int i = 0; i < array.size(); i++) {
                JsonObject obj = array.get(i).getAsJsonObject();
                if (obj.get("sub_title").getAsString().equals(mapParam.get("name"))) {
                    JsonArray arrayParameter = new JsonArray();
                    if (null != obj.get("parameters")) {
                        arrayParameter = obj.get("parameters").getAsJsonArray();
                    }
                    if (arrayParameter.size() > 0) {
                        for (int j = 0; j < arrayParameter.size(); j++) {
                            JsonObject obj1 = arrayParameter.get(j).getAsJsonObject();
                            if (type.equals("edit")) {
                                if (!mapParam.get("path").contains("_notchanged") && obj1.get("path").getAsString().equals(mapParam.get("path"))) {
                                    mapError.put("path_error", "2");
                                    break;
                                }
                            } else {
                                if (obj1.get("path").getAsString().equals(mapParam.get("path"))) {
                                    mapError.put("path_error", "2");
                                    break;
                                }
                            }

                            if (obj1.get("path").getAsString().contains("{i}")) {
                                if (mapParam.get("path").contains("{i}")) {
                                    int lastIndex = obj1.get("path").getAsString().lastIndexOf("{i}");
                                    String parent = obj1.get("path").getAsString().substring(0, lastIndex);
                                    int lastIndexForm = mapParam.get("path").lastIndexOf("{i}");
                                    String parentForm = mapParam.get("path").substring(0, lastIndexForm);
                                    if (!parent.equals(parentForm)) {
                                        mapError.put("path_error", "3");
                                        break;
                                    }
                                } else {
                                    mapError.put("path_error", "3");
                                    break;
                                }
                            } else {
                                if (mapParam.get("path").contains("{i}")) {
                                    mapError.put("path_error", "3");
                                    break;
                                }
                            }

                        }
                    }
                }
            }
        }
        mapParam.put("path", mapParam.get("path").replaceAll("_notchanged", ""));
        // validate alias
        if (Strings.isNullOrEmpty(mapParam.get("alias"))) {
            mapError.put("alias_error", "1");
        } else if (mapParam.get("alias").length() < 3 || mapParam.get("alias").length() > 50) {
            mapError.put("alias_error", "4");
        }

        return mapError;
    }
}
