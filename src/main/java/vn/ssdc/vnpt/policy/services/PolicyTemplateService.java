/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vn.ssdc.vnpt.policy.services;

import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import vn.ssdc.vnpt.policy.model.PolicyTemplate;
import vn.ssdc.vnpt.qos.model.QosKpi;
import vn.ssdc.vnpt.umpexception.QosException;
import vn.ssdc.vnpt.utils.CommonService;
import vn.vnpt.ssdc.core.SsdcCrudService;
import vn.vnpt.ssdc.jdbc.factories.RepositoryFactory;

/**
 *
 * @author kiendt
 */
@Service
public class PolicyTemplateService extends SsdcCrudService<Long, PolicyTemplate> {

    @Autowired
    private CommonService baseService;

    @Autowired
    public PolicyTemplateService(RepositoryFactory repositoryFactory) {
        this.repository = repositoryFactory.create(PolicyTemplate.class);
    }

    public void validate(PolicyTemplate policyTemplate) throws QosException {
        //Download, Upload, Ping
        List<String> policyTypeAllows = Arrays.asList(new String[]{"Download", "Upload", "Ping"});
        if (!baseService.validateAllowValue(policyTemplate.type, policyTypeAllows)) {
            throw new QosException("policy_type_invalid_value");
        }
        //Trong nước, Quốc tế, Ngoại mạng, Nội mạng
//        List<String> policyConnectionDirection = Arrays.asList(new String[]{"Trong nước", "Quốc tế", "Ngoại mạng", "Nội mạng"});
//        if (!baseService.validateAllowValue(policyTemplate.type, policyTypeAllows)) {
//            throw new QosException("policy_type_invalid_value");
//        }
    }

    public List<PolicyTemplate> findByQuery(String query, Integer index, Integer limit) {
        return this.repository.search(query, new PageRequest(index, limit)).getContent();
    }

    public Page<PolicyTemplate> getPage(int page, int limit) {
        return this.repository.findAll(new PageRequest(page, limit));
    }

    public List<PolicyTemplate> findByQuery(String query) {
        return this.repository.search(query);
    }

}
