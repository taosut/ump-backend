/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vn.ssdc.vnpt.selfCare.model.searchForm;

import io.swagger.annotations.ApiModelProperty;

/**
 *
 * @author Admin
 */
public class SCDeviceGroupSearchForm {

    public Integer limit;
    public Integer page;

    public Long id;

    public String deviceGroupName;
    public String manufacture;
    public String modelName;
    public String firmwareVersion;
    @ApiModelProperty("Ho chi minh, Ha noi")
    public String label;

    public String userName;

}
