/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vn.ssdc.vnpt.selfCare.model;

import java.util.Set;

/**
 *
 * @author Admin
 */
public class SCRole {
    
    public Long id;
    public String name;
    public Set<Long> permissionsIds;
    public String description;
    public Set<String> operationIds;
}
