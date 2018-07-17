/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vn.ssdc.vnpt.selfCare.services;

import com.google.common.base.Strings;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import vn.ssdc.vnpt.policy.model.PolicyTemplate;
import vn.ssdc.vnpt.policy.services.PolicyTemplateService;
import vn.ssdc.vnpt.selfCare.model.SCPolicyTemplate;
import vn.ssdc.vnpt.selfCare.model.searchForm.SCPolicyTemplateSearchForm;
import vn.ssdc.vnpt.utils.CommonService;

/**
 *
 * @author kiendt
 */
@Service
public class SelfCareServicePolicyTemplate {

    @Autowired
    PolicyTemplateService policyTemplateService;

    @Autowired
    private CommonService baseService;

    private RestTemplate restTemplate;

    public SelfCareServicePolicyTemplate() {
        this.restTemplate = new RestTemplate();
    }

    public List<SCPolicyTemplate> search(SCPolicyTemplateSearchForm searchForm) {
        List<PolicyTemplate> data = new ArrayList<>();
        Set<String> conditions = generateConditionForSearch(searchForm);
        if (searchForm.limit != null && searchForm.page != null) {
            if (!conditions.isEmpty()) {
                String query = String.join(" AND ", conditions);
                data = policyTemplateService.findByQuery(query, searchForm.page - 1, searchForm.limit);
            } else {
                data = policyTemplateService.getPage(searchForm.page - 1, searchForm.limit).getContent();
            }
        } else {
            if (!conditions.isEmpty()) {
                String query = String.join(" AND ", conditions);
                data = policyTemplateService.findByQuery(query);
            } else {
                data = policyTemplateService.getAll();
            }
        }

        List<SCPolicyTemplate> policyTemplates = new ArrayList<>();
        for (PolicyTemplate tmp : data) {
            policyTemplates.add(new SCPolicyTemplate(tmp));
        }
        return policyTemplates;
    }

    public int count(SCPolicyTemplateSearchForm searchForm) {
        searchForm.limit = null;
        searchForm.page = null;
        List<SCPolicyTemplate> data = search(searchForm);
        return data.isEmpty() ? 0 : data.size();
    }

    /**
     * generate query from search object
     *
     * @param searchForm
     * @return
     */
    private Set<String> generateConditionForSearch(SCPolicyTemplateSearchForm searchForm) {
        Set<String> conditions = new HashSet<>();
        if (!Strings.isNullOrEmpty(searchForm.name)) {
            conditions.add(String.format("name like '%s'", baseService.generateSearchLikeInput(searchForm.name)));
        }
        if (!Strings.isNullOrEmpty(searchForm.type)) {
            conditions.add(String.format("type like '%s'", baseService.generateSearchLikeInput(searchForm.type)));
        }
        if (!Strings.isNullOrEmpty(searchForm.connectionDirection)) {
            conditions.add(String.format("connection_direction like '%s'", baseService.generateSearchLikeInput(searchForm.connectionDirection)));
        }

        if (searchForm.policyTemplateId != null) {
            conditions.add(String.format("id = %s", searchForm.policyTemplateId));
        }
        return conditions;
    }

}
