/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vn.ssdc.vnpt.selfCare.model;

import io.swagger.annotations.ApiModelProperty;
import java.util.Set;
import vn.ssdc.vnpt.subscriber.model.SubscriberTemplate;

/**
 *
 * @author Admin
 */
public class SCSubscriberTemplate {

    public Long id;
    public String name;
    @ApiModelProperty(example = "[\"key\",\"key2\"]")
    public Set<String> templateKeys;

    public static SubscriberTemplate convertToSubscriberTemplate(SCSubscriberTemplate scSubcriberTemplate) {
        SubscriberTemplate subscriberTemplate = new SubscriberTemplate();
        subscriberTemplate.id = scSubcriberTemplate.id;
        subscriberTemplate.name = scSubcriberTemplate.name;
        subscriberTemplate.templateKeys = scSubcriberTemplate.templateKeys;
        return subscriberTemplate;
    }

    public static SCSubscriberTemplate convertToSCSubscriberTemplate(SubscriberTemplate sCSubcriberTemplate) {
        SCSubscriberTemplate subscriberTemplate = new SCSubscriberTemplate();
        subscriberTemplate.id = sCSubcriberTemplate.id;
        subscriberTemplate.name = sCSubcriberTemplate.name;
        subscriberTemplate.templateKeys = sCSubcriberTemplate.templateKeys;
        return subscriberTemplate;
    }

}
