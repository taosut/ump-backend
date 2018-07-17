/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vn.ssdc.vnpt.selfCare.services;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import vn.ssdc.vnpt.notification.model.NotificationSetting;
import vn.ssdc.vnpt.notification.services.NotificationSettingService;
import vn.ssdc.vnpt.selfCare.model.SCNotificationSetting;
import vn.ssdc.vnpt.selfCare.model.searchForm.SCNotificationSettingSearchForm;

/**
 *
 * @author kiendt
 */
@Service
public class SelfCareServiceNotificationSetting {

    @Autowired
    NotificationSettingService notificationSettingService;

    private RestTemplate restTemplate;

    public SelfCareServiceNotificationSetting() {
        this.restTemplate = new RestTemplate();
    }

    public List<SCNotificationSetting> search(SCNotificationSettingSearchForm searchForm) {
        return doSearch(searchForm);
    }

    public int count(SCNotificationSettingSearchForm searchForm) {
        searchForm.limit = null;
        searchForm.page = null;
        List<SCNotificationSetting> data = search(searchForm);
        return data.isEmpty() ? 0 : data.size();
    }

    private List<SCNotificationSetting> doSearch(SCNotificationSettingSearchForm searchForm) {
        List<NotificationSetting> data = new ArrayList<>();
        Set<String> conditions = generateConditionForSearch(searchForm);
        if (searchForm.limit != null && searchForm.page != null) {
            if (!conditions.isEmpty()) {
                String query = String.join(" AND ", conditions);
                data = notificationSettingService.findByQuery(query, searchForm.page - 1, searchForm.limit);
            } else {
                data = notificationSettingService.getPage(searchForm.page - 1, searchForm.limit).getContent();
            }
        } else {
            if (!conditions.isEmpty()) {
                String query = String.join(" AND ", conditions);
                data = notificationSettingService.findByQuery(query);
            } else {
                data = notificationSettingService.getAll();
            }
        }

        List<SCNotificationSetting> sCNotificationSettings = new ArrayList<>();
        for (NotificationSetting tmp : data) {
            sCNotificationSettings.add(new SCNotificationSetting(tmp));
        }
        return sCNotificationSettings;
    }

    private Set<String> generateConditionForSearch(SCNotificationSettingSearchForm searchForm) {
        Set<String> conditions = new HashSet<>();
        if (searchForm.userId != null) {
            conditions.add(String.format("user_id = %s", searchForm.userId));
        }
        return conditions;
    }

}
