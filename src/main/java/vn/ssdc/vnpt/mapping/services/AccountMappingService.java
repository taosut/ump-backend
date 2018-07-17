/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vn.ssdc.vnpt.mapping.services;

import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import vn.ssdc.vnpt.AcsClient;
import vn.ssdc.vnpt.mapping.model.AccountMapping;
import vn.ssdc.vnpt.label.model.Label;
import vn.ssdc.vnpt.label.services.LabelService;
import vn.vnpt.ssdc.core.SsdcCrudService;
import vn.vnpt.ssdc.jdbc.factories.RepositoryFactory;

/**
 *
 * @author kiendt
 */
@Service
public class AccountMappingService extends SsdcCrudService<Long, AccountMapping> {

    @Autowired
    AcsClient acsClient;

    @Autowired
    LabelService labelService;

    @Autowired
    public AccountMappingService(RepositoryFactory repositoryFactory) {
        this.repository = repositoryFactory.create(AccountMapping.class);
    }

    public List<AccountMapping> getByLabelID(Long labelId) {
        List<AccountMapping> ipMappingList = new ArrayList<AccountMapping>();
        String whereExp = " label_id = '%s,'";
        ipMappingList = this.repository.search(String.format(whereExp, labelId));
        return ipMappingList;
    }

    public boolean checkDuplicateAccountMapping(Long id, String accountMapping) {
        List<AccountMapping> ipMappingList = new ArrayList<AccountMapping>();
        String whereExp = " account_prefix = '" + accountMapping + "'" + " and label_id != '" + id + "," + "'";
        ipMappingList = this.repository.search(whereExp);
        return ipMappingList == null || ipMappingList.isEmpty() ? false : true;
    }

    public boolean checkAccountBelongAccountMapping(String accountMapping) {
        List<AccountMapping> accountMappings = this.repository.findAll();
        String[] acc = accountMapping.split("_");
        for (int i = 0; i < accountMappings.size(); i++) {
            if (checkConditionAccountMapping(acc[0], accountMappings.get(i).accountPrefix)) {
                return true;
            }
        }
        return false;
    }

    private boolean checkConditionAccountMapping(String accountNeedChecked, String condition) {
        if (accountNeedChecked.toLowerCase().equals(condition.toLowerCase())) {
            return true;
        }
        return false;
    }

    public void addLabel(String deviceId, String accountMapping) {
        List<AccountMapping> accountMappings = this.repository.findAll();
        String[] acc = accountMapping.split("_");
        String label = "";
        for (int i = 0; i < accountMappings.size(); i++) {
            if (checkConditionAccountMapping(acc[0], accountMappings.get(i).accountPrefix)) {
                label += accountMappings.get(i).labelId;
            }
        }

        String[] split = label.split(",");
        for (int i = 0; i < split.length; i++) {
            if (split[i] != null && !split[i].isEmpty()) {
                createAllLabel1(deviceId, split[i]);
            }
        }
    }

    private void createAllLabel1(String deviceId, String id) {
        Label label = labelService.get(Long.valueOf(id));
        String parentId = label.parentId;
        acsClient.addLabel(deviceId, label.name);
        acsClient.addLabelId(deviceId, label.id);
        while (!parentId.equals("0")) {
            label = labelService.get(Long.valueOf(label.parentId));
            parentId = label.parentId;
            acsClient.addLabel(deviceId, label.name);
            acsClient.addLabelId(deviceId, label.id);
        }
    }
}
