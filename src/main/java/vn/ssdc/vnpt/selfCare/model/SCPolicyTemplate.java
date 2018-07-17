/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vn.ssdc.vnpt.selfCare.model;

import com.google.common.base.Strings;
import vn.ssdc.vnpt.policy.model.PolicyTemplate;
import vn.ssdc.vnpt.qos.model.QosKpi;

/**
 *
 * @author kiendt
 */
public class SCPolicyTemplate {

    public Long id;
    public String name;
    public String type;
    public String connectionDirection;
    public String url;
    public String description;

    public SCPolicyTemplate() {
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

    public SCPolicyTemplate(PolicyTemplate policyTemplate) {
        this.id = policyTemplate.id;
        this.name = policyTemplate.name;
        this.type = policyTemplate.type;
        this.connectionDirection = policyTemplate.connectionDirection;
        this.url = policyTemplate.url;
        this.description = policyTemplate.description;
    }

    public PolicyTemplate convertToPolicyTemplate() {
        PolicyTemplate policyTemplate = new PolicyTemplate();
        policyTemplate.id = this.id;
        policyTemplate.name = this.name;
        policyTemplate.type = this.type;
        policyTemplate.connectionDirection = this.connectionDirection;
        policyTemplate.url = this.url;
        policyTemplate.description = this.description;
        return policyTemplate;
    }

}
