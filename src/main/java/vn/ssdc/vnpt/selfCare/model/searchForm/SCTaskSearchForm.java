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
public class SCTaskSearchForm {
    
    
    public Integer limit;
    public Integer page;
    public String deviceId;
    public String taskName;
    public Date fromCreateDate;
    public Date toCreateDate;
    public Long retries;
    public String fault;
}
