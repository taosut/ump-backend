/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vn.ssdc.vnpt.devices.model;

import java.util.List;

/**
 *
 * @author Admin
 */
public class ProfileSetting {

    public String subTitle;
    public String objectTitle; // if format1 -> disable, if format 2 ->enalbe
    public String format;  // "format1 || format 2"
    public String action; // "add,edit,delete"
    public String onlyEdit;

    public List<ParameterSubProfile> listParameterSubProfile;
    public List<SubObject> listSubObjecet;

}
