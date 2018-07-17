/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vn.ssdc.vnpt.selfCare.model;

import io.swagger.annotations.ApiModelProperty;
import java.util.Map;
import java.util.Set;
import vn.ssdc.vnpt.subscriber.model.Subscriber;

/**
 *
 * @author Admin
 */
public class SCSubscriber {

    public String subscriberId;
    @ApiModelProperty(example = "[ \"3\",\"4\" ]")
    public Set<String> subscriberDataTemplateIds;
    @ApiModelProperty(example = "{ \"Param1\":\"value_1\", \"Param2\":\"value_2\" }")
    public Map<String, String> subscriberData;
    @ApiModelProperty(example = "[ \"a06518-968380GERG-VNPT00a532c2\",\"a06518-968380GERG-VNPT00a532c2\" ]")
    public Set<String> deviceIds;

    public Long id;

    public static SCSubscriber convertToSCSubscriber(Subscriber subscriber) {
        SCSubscriber scSubscriber = new SCSubscriber();
        scSubscriber.id = subscriber.id;
        scSubscriber.subscriberData = subscriber.subscriberData;
        scSubscriber.subscriberDataTemplateIds = subscriber.subscriberDataTemplateIds;
        scSubscriber.subscriberId = subscriber.subscriberId;
        scSubscriber.deviceIds = subscriber.getDeviceIds();
        return scSubscriber;
    }

    public static Subscriber convertToSubscriber(SCSubscriber scSubscriber) {
        Subscriber subscriber = new Subscriber();
        subscriber.id = scSubscriber.id;
        subscriber.subscriberData = scSubscriber.subscriberData;
        subscriber.subscriberDataTemplateIds = scSubscriber.subscriberDataTemplateIds;
        subscriber.subscriberId = scSubscriber.subscriberId;
        subscriber.setDeviceIds(scSubscriber.deviceIds);
        return subscriber;
    }
}
