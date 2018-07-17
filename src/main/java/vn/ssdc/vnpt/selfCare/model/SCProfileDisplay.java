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
public class SCProfileDisplay {

    public long id;
    public String name;
    public String alias;
    public Set<SCSubProfile> list_sub_not_instance;
    public Set<SCSubProfile> list_sub_instance;
}
