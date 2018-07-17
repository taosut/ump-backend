/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vn.ssdc.vnpt.policy.model;

import com.google.common.base.Strings;
import vn.vnpt.ssdc.jdbc.SsdcEntity;

/**
 *
 * @author kiendt
 */
public class PolicyTemplate extends SsdcEntity<Long> {

    public String name;
    public String type;
    public String connectionDirection;
    public String url;
    public String description;

    public PolicyTemplate() {
    }

    public void standardObject() {
        if (!Strings.isNullOrEmpty(name)) {
            this.name = this.name.trim();
        }
        if (!Strings.isNullOrEmpty(type)) {
            this.type = this.type.trim();
        }
        if (!Strings.isNullOrEmpty(connectionDirection)) {
            this.connectionDirection = this.connectionDirection.trim();
        }
        if (!Strings.isNullOrEmpty(url)) {
            this.url = this.url.trim();
        }
    }

}
