/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vn.ssdc.vnpt.selfCare.services;

import com.google.common.base.Strings;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import vn.ssdc.vnpt.common.services.ConfigurationService;
import vn.ssdc.vnpt.selfCare.model.SCDevice;
import vn.ssdc.vnpt.selfCare.model.SCSubscriber;
import vn.ssdc.vnpt.selfCare.model.SCSubscriberDevice;
import vn.ssdc.vnpt.selfCare.model.SCSubscriberTemplate;
import vn.ssdc.vnpt.selfCare.model.searchForm.SCDeviceSearchForm;
import vn.ssdc.vnpt.selfCare.model.searchForm.SCSubscriberDeviceSearchForm;
import vn.ssdc.vnpt.selfCare.model.searchForm.SCSubscriberSearchForm;
import vn.ssdc.vnpt.subscriber.model.Subscriber;
import vn.ssdc.vnpt.subscriber.model.SubscriberDevice;
import vn.ssdc.vnpt.subscriber.model.SubscriberTemplate;
import vn.ssdc.vnpt.subscriber.services.SubscriberDeviceService;
import vn.ssdc.vnpt.subscriber.services.SubscriberService;
import vn.ssdc.vnpt.subscriber.services.SubscriberTemplateService;

/**
 *
 * @author Admin
 */
@Service
public class SelfCareServiceSubscriber {

    @Autowired
    private SubscriberTemplateService subscriberTemplateService;

    @Autowired
    private SubscriberService subscriberService;

    @Autowired
    private SubscriberDeviceService subscriberDeviceService;

    @Autowired
    public ConfigurationService configurationService;

    @Autowired
    public SelfCareServiceDevice selfCareServiceDevice;

    public List<SCSubscriberTemplate> searchSubscriberTemplate(SCSubscriberSearchForm sCSubcriberTemplateSearchForm) {
        if (sCSubcriberTemplateSearchForm.id != null) {
            List<SCSubscriberTemplate> subscriberTemplates = new ArrayList<>();
            if (subscriberTemplateService.get(sCSubcriberTemplateSearchForm.id) != null) {
                subscriberTemplates.add(SCSubscriberTemplate.convertToSCSubscriberTemplate(subscriberTemplateService.get(sCSubcriberTemplateSearchForm.id)));
            }
            return subscriberTemplates;
        }

        List<SCSubscriberTemplate> sCSubscriberTemplates = new ArrayList<>();
        List<SubscriberTemplate> subscriberTemplates = new ArrayList<>();
        if (sCSubcriberTemplateSearchForm.page == null) {
            sCSubcriberTemplateSearchForm.page = Integer.valueOf(configurationService.get("page_default").value);
        }
        if (sCSubcriberTemplateSearchForm.limit != null) {
//            sCSubcriberTemplateSearchForm.limit = Integer.valueOf(configurationService.get("limit_default").value);
            subscriberTemplates = subscriberTemplateService.getSubcribers(sCSubcriberTemplateSearchForm.page - 1, sCSubcriberTemplateSearchForm.limit);
        } else {
            subscriberTemplates = subscriberTemplateService.getAll();
        }

        for (SubscriberTemplate tmp : subscriberTemplates) {
            sCSubscriberTemplates.add(SCSubscriberTemplate.convertToSCSubscriberTemplate(tmp));
        }

        return sCSubscriberTemplates;
    }

    public long countSubcriberTemplate(SCSubscriberSearchForm sCSubcriberTemplateSearchForm) {
        // tim theo subcriber template id
        if (sCSubcriberTemplateSearchForm.page == null) {
            sCSubcriberTemplateSearchForm.page = Integer.valueOf(configurationService.get("page_default").value);
        }
        if (sCSubcriberTemplateSearchForm.limit == null) {
            sCSubcriberTemplateSearchForm.limit = Integer.valueOf(configurationService.get("limit_default").value);
            return subscriberTemplateService.getAll() != null ? subscriberTemplateService.getAll().size() : 0;
        }
        return subscriberTemplateService.getPage(sCSubcriberTemplateSearchForm.page - 1, sCSubcriberTemplateSearchForm.limit).getTotalElements();
    }

    public List<SCSubscriber> searchSubscriber(SCSubscriberSearchForm scSubscriber) {
        String whereExp = generateQuery(scSubscriber);
        List<Subscriber> subscribers = new ArrayList<>();
        List<Subscriber> subscribersFinal = new ArrayList<>();
        if (scSubscriber.page == null) {
            scSubscriber.page = Integer.valueOf(configurationService.get("page_default").value);
        }
        if (scSubscriber.limit != null) {
            subscribers = subscriberService.findByQuery(scSubscriber.page - 1, scSubscriber.limit, whereExp);
        } else {
            subscribers = subscriberService.findByQuery(whereExp);
        }

        if (!Strings.isNullOrEmpty(scSubscriber.deviceId)) {
            List<Subscriber> subscribersWithDevices = subscriberDeviceService.findByDeviceId(scSubscriber.deviceId);
            for (Subscriber tmp1 : subscribersWithDevices) {
                for (Subscriber tmp2 : subscribers) {
                    if (tmp1.id.equals(tmp2.id)) {
                        subscribersFinal.add(tmp1);
                    }
                }
            }
        } else {
            subscribersFinal.addAll(subscribers);
        }

        List<SCSubscriber> sCSubscribers = new ArrayList<>();
        for (Subscriber subscriber : subscribersFinal) {
            Set<String> setsDeviceId = new HashSet<>();
            if (!Strings.isNullOrEmpty(subscriber.subscriberId)) {
                List<SubscriberDevice> subscriberDevices = subscriberDeviceService.findBySubscribeId(subscriber.subscriberId);
                for (SubscriberDevice subscriberDevice : subscriberDevices) {
                    setsDeviceId.add(subscriberDevice.deviceId);
                }
            }
            SCSubscriber scSubscriberTmp = SCSubscriber.convertToSCSubscriber(subscriber);
            scSubscriberTmp.deviceIds = setsDeviceId;
            sCSubscribers.add(scSubscriberTmp);
        }
        return sCSubscribers;
    }

    public List<SCSubscriberDevice> searchSubscriberDevice(SCSubscriberDeviceSearchForm sCSubscriberDeviceSearchForm) {

        List<SubscriberDevice> subscriberDevices = new ArrayList<>();
        String whereExp = "";

        if (!Strings.isNullOrEmpty(sCSubscriberDeviceSearchForm.subscriberId)) {
            whereExp += " and " + "subscriber_id" + " = '" + sCSubscriberDeviceSearchForm.subscriberId + "'";
        }
        if (!Strings.isNullOrEmpty(sCSubscriberDeviceSearchForm.deviceId)) {
            whereExp += " and " + "device_id" + " = '" + sCSubscriberDeviceSearchForm.deviceId + "'";
        }
        if (!whereExp.isEmpty() && whereExp.startsWith(" and")) {
            whereExp = whereExp.substring(4);
        }

        if (sCSubscriberDeviceSearchForm.page == null) {
            sCSubscriberDeviceSearchForm.page = Integer.valueOf(configurationService.get("page_default").value);
        }
        if (sCSubscriberDeviceSearchForm.limit != null) {
//            sCSubscriberDeviceSearchForm.limit = Integer.valueOf(configurationService.get("limit_default").value);
            subscriberDevices = subscriberDeviceService.findByQuery(sCSubscriberDeviceSearchForm.page - 1, sCSubscriberDeviceSearchForm.limit, whereExp);
        } else {
            subscriberDevices = subscriberDeviceService.findByQuery(whereExp);
        }
        List<SCSubscriberDevice> sCSubscriberDevices = new ArrayList<>();
        for (SubscriberDevice subscriberDevice : subscriberDevices) {
            sCSubscriberDevices.add(SCSubscriberDevice.convertToScSubcriberDevice(subscriberDevice));
        }
        return sCSubscriberDevices;
    }

    public String generateQuery(SCSubscriberSearchForm scSubscriber) {
        String whereExp = "";

        if (scSubscriber.id != null) {
            whereExp += " and " + "id" + " = " + scSubscriber.id + "";
        }

        if (!Strings.isNullOrEmpty(scSubscriber.subscriberId)) {
            whereExp += " and " + "subscriber_id" + " like '%" + scSubscriber.subscriberId + "%'";
        }
        if (scSubscriber.subscriberTemplateId != null) {
            whereExp += " and " + "subscriber_data_template_ids" + " like '%" + scSubscriber.subscriberTemplateId + "%'";
        }

        if (!Strings.isNullOrEmpty(scSubscriber.deviceId)) {

        }

        if (scSubscriber.limit == null) {
            scSubscriber.limit = Integer.valueOf(configurationService.get("limit_default").value);
        }
        if (scSubscriber.page == null) {
            scSubscriber.page = Integer.valueOf(configurationService.get("page_default").value);
        }

        if (!whereExp.isEmpty() && whereExp.startsWith(" and")) {
            whereExp = whereExp.substring(4);
        }
        return whereExp;
    }

    public void updateSubscriberDevicesBySubscriber(SCSubscriber scSubscriber) throws Exception {
        List<SubscriberDevice> subscriberDevices = subscriberDeviceService.findBySubscribeId(scSubscriber.subscriberId);
        for (SubscriberDevice subscriberDevice : subscriberDevices) {
            subscriberDeviceService.delete(subscriberDevice.id);
        }
        addSubscriberDevicesBySubscriber(scSubscriber);
    }

    public void addSubscriberDevicesBySubscriber(SCSubscriber scSubscriber) throws ParseException, Exception {
        try {
            if (scSubscriber.deviceIds != null && !scSubscriber.deviceIds.isEmpty()) {
                for (String device : scSubscriber.deviceIds) {
                    if (device.split("-").length == 3) {
                        String[] info = device.split("-");
                        String oui = info[0];
                        String productClass = info[1];
                        String serialNumber = info[2];
                        SCDeviceSearchForm deviceSearchForm = new SCDeviceSearchForm();
                        deviceSearchForm.oui = oui;
                        deviceSearchForm.productClass = productClass;
                        deviceSearchForm.serialNumber = serialNumber;
                        List<SCDevice> devices = selfCareServiceDevice.searchDevice(deviceSearchForm);
                        SubscriberDevice subscriberDevice = new SubscriberDevice();
                        subscriberDevice.subscriberId = scSubscriber.subscriberId;
                        if (!devices.isEmpty()) {
                            SCDevice deviceTMp = devices.get(0);
                            subscriberDevice.deviceId = device;
                            subscriberDevice.oui = deviceTMp.oui;
                            subscriberDevice.productClass = deviceTMp.productClass;
                            subscriberDevice.serialNumber = deviceTMp.serialNumber;
                            subscriberDevice.manufacturer = deviceTMp.manufacturer;
                        } else {
                            subscriberDevice.deviceId = oui + "-" + productClass + "-" + serialNumber;
                            subscriberDevice.oui = oui;
                            subscriberDevice.productClass = productClass;
                            subscriberDevice.serialNumber = serialNumber;
                        }
                        subscriberDeviceService.create(subscriberDevice);
                    }
                }
            }
        } catch (Exception e) {
            throw e;
        }

    }

    public void deleteBySubscriberId(String subscriberId) throws Exception {
        try {
        List<SubscriberDevice> subscriberDevices = subscriberDeviceService.findBySubscribeId(subscriberId);
            for (SubscriberDevice device : subscriberDevices) {
                subscriberDeviceService.delete(device.id);
            }
        } catch (Exception e) {
            throw e;
        }
    }
}
