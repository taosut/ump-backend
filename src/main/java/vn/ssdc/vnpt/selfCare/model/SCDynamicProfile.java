/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vn.ssdc.vnpt.selfCare.model;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.HashSet;
import java.util.Set;
import vn.ssdc.vnpt.devices.model.Tag;

/**
 *
 * @author kiendt
 */
public class SCDynamicProfile {

    public String sub_title;
    public String window_format;
    public String sub_object_title;
    public Set<SCDynamicParameter> parameters;

    public SCDynamicProfile() {
    }

    public SCDynamicProfile(Tag tag, String subName) {
        this.parameters = new HashSet<>();
        JsonArray array = new Gson().fromJson(tag.subProfileSetting, JsonArray.class);
        for (int i = 0; i < array.size(); i++) {
            JsonObject obj = array.get(i).getAsJsonObject();
            String subTitle = obj.get("sub_title") == null ? "" : obj.get("sub_title").getAsString();
            if (subTitle.equals(subName)) {
                this.sub_object_title = obj.get("sub_object_title") == null ? "" : obj.get("sub_object_title").getAsString();
                this.sub_title = obj.get("sub_title") == null ? "" : obj.get("sub_title").getAsString();
                this.window_format = obj.get("window_format") == null ? "" : obj.get("window_format").getAsString();

                Set<SCDynamicParameter> scDynamicParameters = new HashSet<>();
                JsonArray arrParam = obj.get("parameters").getAsJsonArray();
                for (int j = 0; j < arrParam.size(); j++) {
                    JsonObject objParam = arrParam.get(j).getAsJsonObject();
                    SCDynamicParameter scDynamicParameter = new SCDynamicParameter();
                    scDynamicParameter.alias = objParam.get("alias") == null ? "" : objParam.get("alias").getAsString();
                    scDynamicParameter.is_pointer = objParam.get("is_pointer") == null ? "" : objParam.get("is_pointer").getAsString();
                    scDynamicParameter.path = objParam.get("path") == null ? "" : objParam.get("path").getAsString();
                    scDynamicParameter.read_only = objParam.get("read_only") == null ? "" : objParam.get("read_only").getAsString();
                    scDynamicParameter.setting_value = objParam.get("setting_value") == null ? "" : objParam.get("setting_value").getAsString();
                    scDynamicParameter.type = objParam.get("type") == null ? "" : objParam.get("type").getAsString();
                    scDynamicParameters.add(scDynamicParameter);
                }
                this.parameters = scDynamicParameters;
                break;
            }

        }
    }
}
