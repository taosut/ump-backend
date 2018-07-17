/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vn.ssdc.vnpt.selfCare.services;

import com.google.common.base.Strings;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.net.util.SubnetUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import vn.ssdc.vnpt.mapping.model.AccountMapping;
import vn.ssdc.vnpt.devices.model.DeviceGroup;
import vn.ssdc.vnpt.devices.services.DeviceGroupService;
import vn.ssdc.vnpt.mapping.model.IpMapping;
import vn.ssdc.vnpt.label.model.Label;
import vn.ssdc.vnpt.label.services.LabelService;
import vn.ssdc.vnpt.mapping.services.AccountMappingService;
import vn.ssdc.vnpt.mapping.services.IpMappingService;
import vn.ssdc.vnpt.selfCare.model.SCDevice;
import vn.ssdc.vnpt.selfCare.model.SCLabel;
import vn.ssdc.vnpt.selfCare.model.searchForm.SCDeviceSearchForm;
import vn.ssdc.vnpt.selfCare.model.searchForm.SCLabelForm;
import vn.ssdc.vnpt.umpexception.UmpNbiException;

/**
 *
 * @author Admin
 */
@Service
public class SelfCareServiceLabel {

    @Autowired
    private LabelService labelService;

    @Autowired
    private SelfCareServiceDevice selfCareServiceDevice;

    @Autowired
    private SelfCareServiceLabel selfCareServiceLabel;

    @Autowired
    private IpMappingService ipMappingService;

    @Autowired
    private AccountMappingService accountMappingService;

    @Autowired
    private DeviceGroupService deviceGroupService;

    public SCLabel convertFromLabelToSCLabel(Label label) {
        SCLabel scLabel = new SCLabel();
        scLabel.id = label.id;
        scLabel.name = label.name;
        scLabel.description = label.description;
        scLabel.parentId = Long.valueOf(label.parentId);
        scLabel.parentName = label.parentName;
        scLabel.deviceGroupId = label.deviceGroupId;
        return scLabel;
    }

    public Label convertFromSCLabelToLabel(SCLabel scLabel) {
        Label label = new Label();
        label.id = scLabel.id;
        label.name = scLabel.name;
        label.description = scLabel.description;
        label.parentName = scLabel.parentName;
        label.parentId = String.valueOf(scLabel.parentId);
        label.deviceGroupId = scLabel.deviceGroupId;
        return label;
    }

    public Label convertFromSCLabelToLabel(SCLabelForm scLabel) {
        Label label = new Label();
        label.name = scLabel.name;
        label.description = scLabel.description;
        label.parentName = scLabel.parentName;
        label.parentId = String.valueOf(scLabel.parentId);
        return label;
    }

    public String createAllLabel(Label label) {
        String labelStr = "";
        List<Label> labels = labelService.loadLabelTreeByNode(label.parentId);
        labelStr += " OR " + createLabel(labels, label.name);
        return labelStr.substring(4);
    }

    public String createLabel(List<Label> labels, String parentName) {
        String totalChild = "";
        Label label = null;
        String parentId = "";
        do {
            for (Label tmp : labels) {
                if (label != null) {
                    parentId = String.valueOf(label.parentId);
                } else {
                    parentId = tmp.parentId;
                }
                if (!parentId.equals("0")) {
                    label = labelService.get(Long.valueOf(parentId));
                    totalChild += " AND " + label.name;
                }
            }

        } while (!parentId.equals("0"));
        return parentName + totalChild;
    }

    public Map<String, String> getIPFromCIDR(String cidr) {
        SubnetUtils utils = new SubnetUtils(cidr);
        String[] addresses = utils.getInfo().getAllAddresses();
        int count = utils.getInfo().getAddressCount();
        String startIpRange = "";
        String endIpRange = "";
        if (count == 0) {
            startIpRange = cidr.substring(0, cidr.indexOf("/"));
            endIpRange = cidr.substring(0, cidr.indexOf("/"));
        } else {
            startIpRange = addresses[0];
            endIpRange = addresses[addresses.length - 1];
        }
        Map<String, String> map = new HashMap<>();
        map.put("start_ip", startIpRange);
        map.put("end_ip", endIpRange);
        return map;
    }

    public void checkDulicateMapping(Long id, SCLabelForm scLabelForm) throws UmpNbiException {
        if (scLabelForm.ipMapping == null || scLabelForm.accountMapping == null) {
            return;
        }
        for (String ipMappingTmp : scLabelForm.ipMapping) {
            if (ipMappingService.checkDuplicateIpMapping(id, ipMappingTmp)) {
                throw new UmpNbiException("error_ipmapping_duplicate");
            }
        }
        for (String accountMappingTmp : scLabelForm.accountMapping) {
            if (accountMappingService.checkDuplicateAccountMapping(id, accountMappingTmp)) {
                throw new UmpNbiException("error_accountmapping_duplicate");
            }
        }
    }

    public void createIpMappingForLabel(Set<String> ipMappings, Long id, String labelCommon) {
        for (String ipMappingTmp : ipMappings) {
            IpMapping ipMapping = new IpMapping();
            ipMapping.label = labelCommon;
            ipMapping.ipMappings = ipMappingTmp;
            ipMapping.startIp = selfCareServiceLabel.getIPFromCIDR(ipMappingTmp).get("start_ip");
            ipMapping.endIp = selfCareServiceLabel.getIPFromCIDR(ipMappingTmp).get("end_ip");
            ipMapping.labelId = String.valueOf(id) + ",";
            ipMappingService.create(ipMapping);
        }
    }

    public void createAccountMappingForLabel(Set<String> accountPrefixs, Long id, String labelCommon) {
        for (String accountMappingTmp : accountPrefixs) {
            AccountMapping accountMapping = new AccountMapping();
            accountMapping.label = labelCommon;
            accountMapping.accountPrefix = accountMappingTmp;
            accountMapping.labelId = String.valueOf(id) + ",";
            accountMappingService.create(accountMapping);
        }
    }

    public SCLabel mappingLabel(SCLabelForm first, SCLabel second) {
//        if (first.id != null) {
//            second.id = first.id;
//        }

        if (!Strings.isNullOrEmpty(first.description)) {
            second.description = first.description;
        }
//        if (first.deviceGroupId != null) {
//            second.deviceGroupId = first.deviceGroupId;
//        }
//        if (!Strings.isNullOrEmpty(first.ipMapping)) {
//            second.ipMapping = first.ipMapping;
//        }
        if (!Strings.isNullOrEmpty(first.name)) {
            second.name = first.name;
        }
        if (first.parentId != null) {
            second.parentId = first.parentId;
        }
        if (!Strings.isNullOrEmpty(first.parentName)) {
            second.parentName = first.parentName;
        }
        return second;
    }

    public boolean checkInUse(Long id) throws Exception {
        try {
            Label label = labelService.get(id);
            List<DeviceGroup> deviceGroups = deviceGroupService.findAllByLabelId(id);
            if (!deviceGroups.isEmpty()) {
                return true;
            }
            SCDeviceSearchForm searchForm = new SCDeviceSearchForm();
            searchForm.label = label.name;
            List<SCDevice> devices = selfCareServiceDevice.searchDevice(searchForm);
            return !devices.isEmpty();
        } catch (Exception e) {
            throw e;
        }

    }

}
