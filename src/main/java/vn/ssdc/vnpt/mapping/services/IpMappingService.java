/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vn.ssdc.vnpt.mapping.services;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.net.util.SubnetUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import vn.ssdc.vnpt.AcsClient;
import vn.ssdc.vnpt.label.model.Label;
import vn.ssdc.vnpt.label.services.LabelService;
import vn.ssdc.vnpt.mapping.model.IpMapping;
import vn.vnpt.ssdc.core.SsdcCrudService;
import vn.vnpt.ssdc.jdbc.factories.RepositoryFactory;

/**
 *
 * @author kiendt
 */
@Service
public class IpMappingService extends SsdcCrudService<Long, IpMapping> {

    private static final Logger logger = LoggerFactory.getLogger(vn.ssdc.vnpt.mapping.services.IpMappingService.class);

    @Autowired
    LabelService labelService;

    @Autowired
    AcsClient acsClient;

    @Autowired
    public IpMappingService(RepositoryFactory repositoryFactory) {
        this.repository = repositoryFactory.create(IpMapping.class);
    }

    public List<IpMapping> search(String limit, String indexPage, String whereExp) {
        List<IpMapping> ipMappingList = new ArrayList<IpMapping>();
        if (!whereExp.isEmpty()) {
            ipMappingList = this.repository.search(whereExp, new PageRequest(Integer.parseInt(indexPage), Integer.parseInt(limit))).getContent();
        } else {
            ipMappingList = this.repository.findAll(new PageRequest(Integer.parseInt(indexPage), Integer.parseInt(limit))).getContent();
        }
        return ipMappingList;
    }

    public List<IpMapping> search(String whereExp) {
        List<IpMapping> ipMappingList = new ArrayList<IpMapping>();
        if (!whereExp.isEmpty()) {
            ipMappingList = this.repository.search(whereExp);
        }
        return ipMappingList;
    }

    public int count(String limit, String indexPage, String whereExp) {
        int count = 0;
        if (!whereExp.isEmpty()) {
            count = this.repository.search(whereExp, new PageRequest(Integer.parseInt(indexPage), Integer.parseInt(limit))).getContent().size();
        } else {
            count = this.repository.findAll(new PageRequest(Integer.parseInt(indexPage), Integer.parseInt(limit))).getContent().size();
        }
        return count;
    }

    public List<IpMapping> getIPByParentTree(String parentId) {
        String[] parentIds = parentId.split(",");
        String query = "";
        for (int i = 0; i < parentIds.length; i++) {
            query += " or label_id in ('" + parentIds[i] + ",')";
        }
        if (!query.isEmpty()) {
            query = query.substring(3);
        }
        return this.repository.search(query);
    }

    /**
     * Example subnet = 10.2.8.0/24
     *
     * @param ip
     * @param subnet
     * @return
     */
    private boolean checkConditionIpBelongSubnet(String ip, String subnet) {
        try {
            SubnetUtils utils = new SubnetUtils(subnet);
            if (utils.getInfo().isInRange(ip)) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean checkIpBelongSubnet(String ip) {
        List<IpMapping> all = this.repository.findAll();
        for (int i = 0; i < all.size(); i++) {
            if (checkConditionIpBelongSubnet(ip, all.get(i).ipMappings)) {
                return true;
            }
        }
        return false;
    }

    public void addLabel(String deviceId, String ip) {
        List<IpMapping> all = this.repository.findAll();
        String label = "";
        for (int i = 0; i < all.size(); i++) {
            if (checkConditionIpBelongSubnet(ip, all.get(i).ipMappings)) {
                label += all.get(i).labelId;
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

    public boolean checkIPMappingExist(String id) {
        int a = this.repository.search(" label_id like '%" + id + "%'").size();
        if (a == 0) {
            return true;
        }
        return false;
    }

    public boolean checkDuplicateIpMapping(Long id, String ip) {
        SubnetUtils utils = new SubnetUtils(ip);
        String startIp = utils.getInfo().getLowAddress();
        String endIp = utils.getInfo().getHighAddress();
        List<IpMapping> ipMappingList = new ArrayList<IpMapping>();
        String whereExp = " label_id != '" + id + "," + "'";
        ipMappingList = this.repository.search(whereExp);

        boolean isBelong = false;
        for (IpMapping ipMapping : ipMappingList) {

            String startIpRange = ipMapping.startIp;
            String endIpRange = ipMapping.endIp;
            if (isValidRangeFather(startIpRange, endIpRange, startIp, endIp)) {
                System.out.println(startIpRange + " - " + endIpRange);
                isBelong = true;
            }
        }
        return isBelong;
    }

    public List<IpMapping> getByLabelID(Long labelId) {
        List<IpMapping> ipMappingList = new ArrayList<IpMapping>();
        String whereExp = " label_id = '%s,'";
        ipMappingList = this.repository.search(String.format(whereExp, labelId));
        return ipMappingList;
    }

    public boolean isValidRangeFather(String ipStart, String ipEnd,
            String ipStartToCheck, String ipEndToCheck) {
        try {
            long ipLo = ipToLong(InetAddress.getByName(ipStart));
            long ipHi = ipToLong(InetAddress.getByName(ipEnd));
            long ipLoToTest = ipToLong(InetAddress.getByName(ipStartToCheck));
            long ipHiToTest = ipToLong(InetAddress.getByName(ipEndToCheck));
            if (ipLoToTest >= ipLo && ipHiToTest <= ipHi) {
                return true;
            }
            return false;
        } catch (UnknownHostException e) {
            System.out.println("e : " + e.toString());
            return false;
        }
    }

    public long ipToLong(InetAddress ip) {
        byte[] octets = ip.getAddress();
        long result = 0;
        for (byte octet : octets) {
            result <<= 8;
            result |= octet & 0xff;
        }
        return result;
    }
}
