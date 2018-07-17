/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vn.ssdc.vnpt.mapping.model;

import vn.vnpt.ssdc.jdbc.SsdcEntity;

/**
 *
 * @author kiendt
 */
public class AccountMapping extends SsdcEntity<Long> {

    public String accountPrefix;
    public String label;
    public String labelId;

}
