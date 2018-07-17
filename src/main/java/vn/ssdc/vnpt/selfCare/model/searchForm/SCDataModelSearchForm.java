/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vn.ssdc.vnpt.selfCare.model.searchForm;

import java.util.Date;

/**
 *
 * @author Admin
 */
public class SCDataModelSearchForm {

    public Integer limit;
    public Integer page;

    public String manufacturer;
    public String oui;
    public String productClass;
    public String modelName;
    public String firmwareVersion;
    public Date createdFrom;
    public Date createdTo;

}
