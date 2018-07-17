/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vn.ssdc.vnpt.devices.endpoints;

import com.google.common.base.Strings;
import io.swagger.annotations.Api;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import org.apache.commons.net.util.SubnetUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import vn.ssdc.vnpt.mapping.model.IpMapping;
import vn.ssdc.vnpt.label.model.Label;
import vn.ssdc.vnpt.label.services.LabelService;
import vn.ssdc.vnpt.selfCare.model.SCDeviceGroup;
import vn.ssdc.vnpt.selfCare.model.SCLabel;
import vn.ssdc.vnpt.selfCare.services.SelfCareServiceDeviceGroup;
import vn.ssdc.vnpt.selfCare.services.SelfCareServiceLabel;

/**
 *
 * @author Admin
 */
@Component
@Path("stb")
@Produces(APPLICATION_JSON)
@Api("Stbs")
public class STBEndpoint {

    private static final String ROOT_FILE = "stb-ipmapping-data/area.xml";

    private static final Logger logger = LoggerFactory.getLogger(STBEndpoint.class);

    class AreaSTB {

        Long id;
        Long areaType;
        String ipAddress;
        String endAddress;
        String ipSource;
        String name;
        Long parentId;
        Long newId;

        public AreaSTB() {
        }

    }

    @Autowired
    LabelService labelService;

    @Autowired
    vn.ssdc.vnpt.mapping.services.IpMappingService ipMappingService;

    @Autowired
    SelfCareServiceDeviceGroup selfCareServiceDeviceGroup;

    @Autowired
    SelfCareServiceLabel selfCareServiceLabel;

    private Document readFile(String fileName) throws DocumentException {
        InputStream is = getClass().getClassLoader().getResourceAsStream(fileName);
        SAXReader reader = new SAXReader();
        Document document = reader.read(is);
        return document;
    }

    @POST
    @Path("/import")
    public void importData() {
        try {
            logger.info(" Start import from {}", ROOT_FILE);
            Document document = readFile(ROOT_FILE);
            List<Node> records = document.selectNodes("//RECORDS/RECORD");
            List<AreaSTB> areaChilds = new ArrayList<>();
            List<AreaSTB> areaRoots = new ArrayList<>();
            for (Node node : records) {
                AreaSTB areaNode = new AreaSTB();
                areaNode.id = Strings.isNullOrEmpty(node.selectSingleNode("id").getText()) ? null : Long.valueOf(node.selectSingleNode("id").getText());
//            areaNode.areaType = Strings.isNullOrEmpty(node.selectSingleNode("area_type").getText()) ? null : Long.valueOf(node.selectSingleNode("area_type").getText());
                areaNode.parentId = Strings.isNullOrEmpty(node.selectSingleNode("parent_id").getText()) ? null : Long.valueOf(node.selectSingleNode("parent_id").getText());
                areaNode.name = node.selectSingleNode("name").getText();
                areaNode.ipAddress = node.selectSingleNode("ip_address").getText();
                areaNode.endAddress = node.selectSingleNode("end_address").getText();
                areaNode.ipSource = node.selectSingleNode("cidr_sign").getText();
                if (null == areaNode.parentId || areaNode.parentId == -1) {
                    areaRoots.add(areaNode);
                } else {
                    areaChilds.add(areaNode);
                }
            }
            // insert area root
            logger.info(" Start insert root label");
            insertRootlabel(areaRoots);
            logger.info(" Start insert child label behind root label");
            insertChildLabelBehindRoot(areaRoots, areaChilds);
            logger.info(" Start insert child label");
            for (AreaSTB tmp : areaChilds) {
                if (tmp.newId == null) {
                    loopFindChild(tmp, areaChilds);
                }
            }
        } catch (Exception e) {
            logger.error("{}", e);
        }

    }

    public void insertRootlabel(List<AreaSTB> areaRoots) {
        Label rootLabel = labelService.get(1l);
        for (AreaSTB area : areaRoots) {
            if (area.parentId == null) {
                Label label = generateLabel(area, rootLabel);
                Label labelVietNam = labelService.create(label);
                createDeviceGroupForEachlabel(labelVietNam);
                if (!Strings.isNullOrEmpty(area.ipSource)) {
                    createIpMapping(area, labelVietNam);
                }
                for (AreaSTB areaTmp : areaRoots) {
                    if (areaTmp.parentId != null && areaTmp.parentId == -1) {
                        Label labelChild = generateLabel(areaTmp, labelVietNam);
                        Label labelProvin = labelService.create(labelChild);
                        createDeviceGroupForEachlabel(labelProvin);
                        areaTmp.newId = labelProvin.id;
                        if (!Strings.isNullOrEmpty(areaTmp.ipSource)) {
                            createIpMapping(areaTmp, labelProvin);
                        }
                    }
                }
                break;
            }
        }

    }

    public void insertChildLabelBehindRoot(List<AreaSTB> areaRoots, List<AreaSTB> areaChilds) {
        for (AreaSTB areaRoot : areaRoots) {
            for (AreaSTB areaChild : areaChilds) {
                if (areaChild.parentId == areaRoot.id) {
                    Label labelChild = generateLabel(areaChild, areaRoot);
                    Label label = labelService.create(labelChild);
                    // create deviceGroup for label
                    createDeviceGroupForEachlabel(label);
                    areaChild.newId = label.id;
                    if (!Strings.isNullOrEmpty(areaChild.ipSource)) {
                        createIpMapping(areaChild, label);
                    }
                }
            }
        }
    }

    public void loopFindChild(AreaSTB area, List<AreaSTB> areaChilds) {
        for (AreaSTB tmp : areaChilds) {
            if (area.parentId == tmp.id && tmp.newId != null && area.id != tmp.id) {
                // neu da chen insert 
                Label labelChild = generateLabel(area, tmp);
                Label label = labelService.create(labelChild);
                // create DeviceGroup for label
                createDeviceGroupForEachlabel(label);
                area.newId = label.id;
                if (!Strings.isNullOrEmpty(area.ipSource)) {
                    createIpMapping(area, label);
                }
            }
            if (area.parentId == tmp.id && tmp.newId == null && area.id != tmp.id) {
                loopFindChild(tmp, areaChilds);
            }
        }
    }

    private void createDeviceGroupForEachlabel(Label label) {
        SCLabel scLabelCreated = selfCareServiceLabel.convertFromLabelToSCLabel(label);
        // create ipmapping
        String labelCommon = selfCareServiceLabel.createAllLabel(label);
        // create device group for this label
        SCDeviceGroup scDeviceGroup = new SCDeviceGroup();
        scDeviceGroup.manufacturer = "All";
        scDeviceGroup.modelName = "All";
        scDeviceGroup.firmwareVersion = "All";
        scDeviceGroup.name = scLabelCreated.name;
        Set<String> labels = new HashSet<>();
        labels.add(labelCommon);
        scDeviceGroup.labels = labels;
        Set<Long> labelIds = new HashSet<>();
        labelIds.add(scLabelCreated.id);
        scDeviceGroup.labelIds = labelIds;
        scLabelCreated.deviceGroupId = selfCareServiceDeviceGroup.create(scDeviceGroup).id;
        label = selfCareServiceLabel.convertFromSCLabelToLabel(scLabelCreated);
        labelService.update(label.id, label);
    }

    public void createIpMapping(AreaSTB area, Label label) {
        if (area.ipSource.contains(";")) {
            String[] listArea = area.ipSource.split(";");
            for (int i = 0; i < listArea.length; i++) {
                IpMapping ipMapping = new IpMapping();
                ipMapping.label = createAllLabel(label);
                String ip = listArea[i];
                ipMapping.ipMappings = ip;
                ipMapping.startIp = getIPFromCIDR(ip).get("start_ip");
                ipMapping.endIp = getIPFromCIDR(ip).get("end_ip");
                ipMapping.labelId = String.valueOf(label.id) + ",";
                ipMappingService.create(ipMapping);
            }
        } else {
            IpMapping ipMapping = new IpMapping();
            ipMapping.label = createAllLabel(label);
            ipMapping.ipMappings = area.ipSource;
            ipMapping.startIp = getIPFromCIDR(area.ipSource).get("start_ip");
            ipMapping.endIp = getIPFromCIDR(area.ipSource).get("end_ip");
            ipMapping.labelId = String.valueOf(label.id) + ",";
            ipMappingService.create(ipMapping);
        }

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

        while (!parentId.equals("0")) {
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
        }
        return parentName + totalChild;
    }

    private Map<String, String> getIPFromCIDR(String cidr) {
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

    private Label generateLabel(AreaSTB currentArea, AreaSTB parentArea) {
        Label labelChild = new Label();
        labelChild.name = currentArea.name;
        labelChild.description = currentArea.name;
        labelChild.parentId = String.valueOf(parentArea.newId);
        labelChild.parentName = parentArea.name;
        return labelChild;
    }

    private Label generateLabel(AreaSTB currentArea, Label parentLabel) {
        Label labelChild = new Label();
        labelChild.name = currentArea.name;
        labelChild.description = currentArea.name;
        labelChild.parentId = String.valueOf(parentLabel.id);
        labelChild.parentName = parentLabel.name;
        return labelChild;
    }
}
