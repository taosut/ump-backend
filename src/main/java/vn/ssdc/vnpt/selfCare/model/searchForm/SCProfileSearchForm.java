/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vn.ssdc.vnpt.selfCare.model.searchForm;

import java.util.Set;

/**
 *
 * @author kiendt
 */
public class SCProfileSearchForm {

    public Integer limit;
    public Integer page;

    public Long deviceTypeVersionId;
    public String name;
    public Set<String> correspondingModule;
}
