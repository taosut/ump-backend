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
public class SCDynamicParameter {

    public String read_only;
    public String alias;
    public String type;
    public String setting_value;
    public String is_pointer;
    public String path;

    public SCDynamicParameter() {
    }

    public SCDynamicParameter(Tag tag, String subName, String subParameter) {
        JsonArray array = new Gson().fromJson(tag.subProfileSetting, JsonArray.class);
        for (int i = 0; i < array.size(); i++) {
            JsonObject obj = array.get(i).getAsJsonObject();
            String subTitle = obj.get("sub_title") == null ? "" : obj.get("sub_title").getAsString();
            if (subTitle.equals(subName)) {
                JsonArray arrParam = obj.get("parameters").getAsJsonArray();
                for (int j = 0; j < arrParam.size(); j++) {
                    JsonObject objParam = arrParam.get(j).getAsJsonObject();
                    if (objParam.get("path") != null && objParam.get("path").getAsString().equals(subParameter)) {
                        this.alias = objParam.get("alias") == null ? "" : objParam.get("alias").getAsString();
                        this.is_pointer = objParam.get("is_pointer") == null ? "" : objParam.get("is_pointer").getAsString();
                        this.path = objParam.get("path") == null ? "" : objParam.get("path").getAsString();
                        this.read_only = objParam.get("read_only") == null ? "" : objParam.get("read_only").getAsString();
                        this.setting_value = objParam.get("setting_value") == null ? "" : objParam.get("setting_value").getAsString();
                        this.type = objParam.get("type") == null ? "" : objParam.get("type").getAsString();
                        break;
                    }
                }
            }

        }
    }

}
