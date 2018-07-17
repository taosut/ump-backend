/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vn.ssdc.vnpt.selfCare.services;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.StringReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import org.apache.commons.lang3.StringUtils;
import org.dom4j.Document;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.Presence;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.xml.sax.InputSource;
import vn.ssdc.vnpt.AcsClient;
import vn.ssdc.vnpt.common.services.ConfigurationService;
import vn.ssdc.vnpt.devices.model.BlacklistDevice;
import vn.ssdc.vnpt.devices.model.DeviceGroup;
import vn.ssdc.vnpt.devices.model.DeviceType;
import vn.ssdc.vnpt.devices.model.DeviceTypeVersion;
import vn.ssdc.vnpt.devices.model.Parameter;
import vn.ssdc.vnpt.devices.model.Tag;
import vn.ssdc.vnpt.devices.services.BlackListDeviceService;
import vn.ssdc.vnpt.devices.services.DataModelService;
import vn.ssdc.vnpt.devices.services.DeviceGroupService;
import vn.ssdc.vnpt.devices.services.DeviceTypeService;
import vn.ssdc.vnpt.devices.services.DeviceTypeVersionService;
import vn.ssdc.vnpt.devices.services.TagService;
import vn.ssdc.vnpt.selfCare.model.SCDevice;
import vn.ssdc.vnpt.selfCare.model.SCFile;
import vn.ssdc.vnpt.selfCare.model.SCPing;
import vn.ssdc.vnpt.selfCare.model.SCTask;
import vn.ssdc.vnpt.selfCare.model.searchForm.SCDeviceSearchForm;
import vn.ssdc.vnpt.selfCare.model.searchForm.SCFileSearchForm;
import vn.ssdc.vnpt.user.model.User;
import vn.ssdc.vnpt.user.services.UserService;
import vn.vnpt.ssdc.core.ObjectCache;

/**
 *
 * @author Admin
 */
@Service
public class SelfCareServiceDevice {

    @Autowired
    private AcsClient acsClient;

    @Autowired
    public ConfigurationService configurationService;

    @Autowired
    private DeviceTypeService deviceTypeService;

    @Autowired
    private DeviceTypeVersionService deviceTypeVersionService;

    @Autowired
    private DeviceGroupService deviceGroupService;

    @Autowired
    private TagService tagService;

    @Autowired
    private ObjectCache ssdcCache;

    @Autowired
    private DataModelService dataModelService;

    @Autowired
    private UserService userService;

    @Autowired
    private BlackListDeviceService blackListDeviceService;

    @Autowired
    private SelfCareServiceUser selfCareServiceUser;

    @Autowired
    private SelfCareServiceFile selfCareServiceFile;

    @Value("${xmpp.domain}")
    private String xmppDomain;

    @Value("${xmpp.port}")
    private int xmppPort;

    @Value("${xmpp.host}")
    private String xmppHost;

    @Value("${xmpp.resource}")
    private String xmppResource;

    @Value("${xmpp.username}")
    private String xmppUsername;

    @Value("${xmpp.password}")
    private String xmppPassword;

    @Value("${checkOnlineType}")
    private String checkOnlineType;

    @Value("${xmpp.urlPresence}")
    private String urlPresence;

    public static final String ID = "_id";
    public static final String IP = "_ip";
    public static final String SERIAL_NUMBER = "summary.serialNumber";
    public static final String MANUFACTURER = "summary.manufacturer";
    public static final String OUI = "summary.oui";
    public static final String PRODUCT_CLASS = "summary.productClass";
    public static final String FIRMWARE_VERSION = "summary.softwareVersion";
    public static final String LAST_INFORM = "_lastInform";
    public static final String LAST_BOOT = "_lastBoot";
    public static final String LAST_BOOTSTRAP = "_lastBootstrap";
    public static final String LAST_CONNECTION_REQUEST = "_lastConnectionRequest";
    public static final String LAST_SYNCHRONIZE = "_lastSynchronize";
    public static final String MAC_ADDRESS = "summary.mac";
    public static final String MODEL_NAME = "summary.modelName";
    public static final String LABEL = "_tags";
    public static final String CREATED = "_registered";
    public static final String HARDWARE_VERSION = "summary.hardwareVersion";
    public static final String CONNECTION_REQUEST_URL = "summary.connectionRequestURL";
    public static final String PERIODIC_INFORM_INTERVAL = "summary.periodicInformInterval";
    public static final String TAGS = "_tags";
    public static final String TIME_STAMP = "_timestamp";
    public static final String STB_IP = "summary.ip";
    public static final String ACCOUNT = "summary.account";
    public static final String LABELID = "_labels";

    SimpleDateFormat dt1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    public static SCTask convertToSCTask(ResponseEntity<String> responseEntity) {
        JsonObject obj = new Gson().fromJson(responseEntity.getBody(), JsonObject.class);
        SCTask scTask = new SCTask();
        if (obj != null && obj.get("_id") != null) {
            scTask.taskId = obj.get("_id").getAsString();
        }
        scTask.httpStatus = responseEntity.getStatusCodeValue();
        return scTask;
    }

    public static Date parseDate(String dateStr) throws ParseException {
        SimpleDateFormat dt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        dt.setTimeZone(TimeZone.getTimeZone("GMT+0"));
        return dt.parse(dateStr);
    }

    public static String getProjection() {
        StringBuilder builder = new StringBuilder();
        builder.append(SelfCareServiceDevice.ID).append(",").
                append("_deviceId._SerialNumber").append(",")
                .append("_deviceId._Manufacturer").append(",")
                .append("_deviceId._OUI").append(",")
                .append("_deviceId._ProductClass").append(",")
                .append(SelfCareServiceDevice.FIRMWARE_VERSION).append(",")
                .append(SelfCareServiceDevice.MODEL_NAME).append(",")
                .append(SelfCareServiceDevice.HARDWARE_VERSION).append(",")
                .append(SelfCareServiceDevice.CONNECTION_REQUEST_URL).append(",")
                .append(SelfCareServiceDevice.LAST_INFORM).append(",")
                .append(SelfCareServiceDevice.LAST_BOOT).append(",")
                .append(SelfCareServiceDevice.LAST_BOOTSTRAP).append(",")
                .append(SelfCareServiceDevice.LAST_SYNCHRONIZE).append(",")
                .append(SelfCareServiceDevice.CREATED).append(",")
                .append(SelfCareServiceDevice.LAST_CONNECTION_REQUEST).append(",")
                .append(SelfCareServiceDevice.IP).append(",")
                .append(SelfCareServiceDevice.MAC_ADDRESS).append(",")
                .append(SelfCareServiceDevice.PERIODIC_INFORM_INTERVAL).append(",")
                .append(SelfCareServiceDevice.STB_IP).append(",")
                .append(SelfCareServiceDevice.LABEL).append(",")
                .append(SelfCareServiceDevice.LABELID).append(",")
                .append(SelfCareServiceDevice.ACCOUNT);
        return builder.toString();
    }

    public SCDevice getDevice(String deviceId) throws ParseException {
        ResponseEntity<String> responseEntity = acsClient.getDevice(deviceId, getProjection());
        String body = (String) responseEntity.getBody();
        List<SCDevice> lstDevice = parseInforDevice(body);
        if (!lstDevice.isEmpty()) {
            SCDevice scDevice = parseInforDevice(body).get(0);
            return scDevice;
        }
        return null;

    }

    public List<SCDevice> searchDevice(SCDeviceSearchForm searchParameter) throws ParseException {
        ResponseEntity<String> responseEntity = doSearch(searchParameter);
        List<SCDevice> listDevice = parseInforDevice(responseEntity.getBody());
        return listDevice;
    }

    public ResponseEntity<String> doSearch(SCDeviceSearchForm searchParameter) throws ParseException {
        Map<String, Object> mapParam = new HashMap<String, Object>();
        dt1.setTimeZone(TimeZone.getTimeZone("GMT+0"));
        if (!Strings.isNullOrEmpty(searchParameter.deviceId)) {
            mapParam.put("_id", String.format("/%s/", searchParameter.deviceId));
        }

        if (!Strings.isNullOrEmpty(searchParameter.firmwareVersion)) {
            mapParam.put(FIRMWARE_VERSION, searchParameter.firmwareVersion);
        }
        if (!Strings.isNullOrEmpty(searchParameter.manufacturer)) {
            mapParam.put("_deviceId._Manufacturer", searchParameter.manufacturer);
        }
        if (!Strings.isNullOrEmpty(searchParameter.modelName)) {
            mapParam.put(MODEL_NAME, searchParameter.modelName);
        }
        if (!Strings.isNullOrEmpty(searchParameter.serialNumber)) {
            mapParam.put("_deviceId._SerialNumber", String.format("/%s/", searchParameter.serialNumber));
        }
        if (!Strings.isNullOrEmpty(searchParameter.oui)) {
            mapParam.put("_deviceId._OUI", searchParameter.oui);
        }
        if (!Strings.isNullOrEmpty(searchParameter.productClass)) {
            mapParam.put("_deviceId._ProductClass", searchParameter.productClass);
        }
        if (!Strings.isNullOrEmpty(searchParameter.ipAddress)) {
            mapParam.put(STB_IP, searchParameter.ipAddress);
        }
        if (!Strings.isNullOrEmpty(searchParameter.account)) {
            mapParam.put(ACCOUNT, String.format("/%s/", searchParameter.account));
        }
        if (!Strings.isNullOrEmpty(searchParameter.label)) {
            if (searchParameter.label.contains(",")) {
                String[] labelsStr = searchParameter.label.split(",");
                JsonArray obj = new JsonArray();
                for (int i = 0; i < labelsStr.length; i++) {
                    if (labelsStr[i].contains(" AND ")) {
                        JsonArray andObj = new JsonArray();
                        String[] andLabelsStr = labelsStr[i].split(" AND ");
                        for (String andLabelStr : andLabelsStr) {
                            JsonObject tmp = new JsonObject();
                            tmp.addProperty(TAGS, andLabelStr);
                            andObj.add(tmp);
                        }
                        JsonObject tmp = new JsonObject();
                        tmp.add("$and", andObj);
                        obj.add(tmp);
                    } else {
                        JsonObject tmp = new JsonObject();
                        tmp.addProperty(TAGS, labelsStr[i]);
                        obj.add(tmp);
                    }
                }
                mapParam.put("$or", obj);
            } else {
                if (searchParameter.label.contains(" AND ")) {
                    JsonArray andObj = new JsonArray();
                    String[] andLabelsStr = searchParameter.label.split(" AND ");
                    for (String andLabelStr : andLabelsStr) {
                        JsonObject tmp = new JsonObject();
                        tmp.addProperty(TAGS, andLabelStr);
                        andObj.add(tmp);
                    }
                    mapParam.put("$and", andObj);
                } else {
                    mapParam.put(TAGS, searchParameter.label);
                }
            }
        }
        if (searchParameter.registeredTo != null && searchParameter.registeredFrom != null) {
            JsonObject obj = new JsonObject();
            obj.addProperty("$lte", dt1.format(searchParameter.registeredTo));
            obj.addProperty("$gte", dt1.format(searchParameter.registeredFrom));
            mapParam.put(CREATED, obj);
        }

        if (searchParameter.registeredFrom != null && searchParameter.registeredTo == null) {
            JsonObject obj = new JsonObject();
            obj.addProperty("$gte", dt1.format(searchParameter.registeredFrom));
            mapParam.put(CREATED, obj);
        }

        if (searchParameter.registeredTo != null && searchParameter.registeredFrom == null) {
            JsonObject obj = new JsonObject();
            obj.addProperty("$lte", dt1.format(searchParameter.registeredTo));
            mapParam.put(CREATED, obj);
        }

        if (searchParameter.lastInformTo != null && searchParameter.lastInformFrom != null) {
            JsonObject obj = new JsonObject();
            obj.addProperty("$lte", dt1.format(searchParameter.lastInformTo));
            obj.addProperty("$gte", dt1.format(searchParameter.lastInformFrom));
            mapParam.put(LAST_INFORM, obj);
        }

        if (searchParameter.lastInformFrom != null && searchParameter.lastInformTo == null) {
            JsonObject obj = new JsonObject();
            obj.addProperty("$gte", dt1.format(searchParameter.lastInformFrom));
            mapParam.put(LAST_INFORM, obj);
        }

        if (searchParameter.lastInformTo != null && searchParameter.lastInformFrom == null) {
            JsonObject obj = new JsonObject();
            obj.addProperty("$lte", dt1.format(searchParameter.lastInformTo));
            mapParam.put(LAST_INFORM, obj);
        }

        Map<String, String> mCondition = new HashMap<>();
        if (!mapParam.isEmpty()) {
            String obj = new Gson().toJson(mapParam);
            mCondition.put("query", obj);
        }

        if (searchParameter.userName != null) {
            String permissionQuery = getPermissionQuery(searchParameter.userName);
            String query = null;
            if (mCondition.get("query") != null) {
                if (permissionQuery != null) {
                    query = "{\"$and\":[" + mCondition.get("query") + "," + permissionQuery + "]}";
                } else {
                    query = mCondition.get("query");
                }
            } else {
                if (permissionQuery != null) {
                    query = permissionQuery;
                }
            }
            if (query != null) {
                mCondition.put("query", query);
            }
        }

        if (searchParameter.deviceGroupId != null) {
            DeviceGroup dg = deviceGroupService.get(searchParameter.deviceGroupId);
            String query = dg.query;
            if (!query.equals("{}") && mCondition.get("query") != null) {
                String finalQuery = "{\"$and\":[" + query + "," + mCondition.get("query") + "]}";
                mCondition.put("query", finalQuery);
            } else if (!query.equals("{}")) {
                mCondition.put("query", query);
            } else if (query.equals("{}")) {
                JsonArray arr = new JsonArray();
                for (String tmp : dg.devices) {
                    JsonObject obj = new JsonObject();
                    obj.addProperty("_id", tmp);
                    arr.add(obj);
                }
                JsonObject queryDevice = new JsonObject();
                queryDevice.add("$or", arr);
                mCondition.put("query", queryDevice.toString());
            } else if (mCondition.get("query") != null) {
                mCondition.put("query", mCondition.get("query"));
            }
        }

        if (searchParameter.limit != null) {
            mCondition.put("limit", String.valueOf(searchParameter.limit));
            mCondition.put("skip", String.valueOf((searchParameter.page - 1) * searchParameter.limit));
        }
        mCondition.put("projection", getProjection());
        ResponseEntity<String> responseEntity = acsClient.search("devices", mCondition);
        System.out.println(mCondition.toString());
        return responseEntity;
    }

    public String getPermissionQuery(String userName) {
        Set<String> queries = new HashSet<>();
        for (String deviceGroupId : selfCareServiceUser.getAllDeviceGroupIds(userName)) {
            DeviceGroup deviceGroup = deviceGroupService.get(Long.parseLong(deviceGroupId));
            queries.add(deviceGroup.query);
            if (deviceGroup.query.trim().equals("{}") || deviceGroup.query.trim().equals("{ }")) {
                return null;
            }
        }
        String query = "{\"_id\":\"\"}";
        if (queries.size() > 0) {
            String tmpQuery = StringUtils.join(queries, ",");
            if (!tmpQuery.equals("{ }") && !tmpQuery.equals("{}")) {
                query = "{\"$or\":[" + tmpQuery + "]}";
            } else {
                return null;
            }
        }
        return query;
    }

    public boolean checkLastFirmwareVersion(JsonObject object) {
        String manufacturer = object.get("_deviceId") == null ? "" : object.get("_deviceId").getAsJsonObject().get("_Manufacturer").getAsString();
        String oui = object.get("_deviceId") == null ? "" : object.get("_deviceId").getAsJsonObject().get("_OUI").getAsString();
        String productClass = object.get("_deviceId") == null ? "" : object.get("_deviceId").getAsJsonObject().get("_ProductClass").getAsString();
        String modelName = object.get(SelfCareServiceDevice.MODEL_NAME) == null ? "" : object.get(SelfCareServiceDevice.MODEL_NAME).getAsJsonObject().get("_value").getAsString();
        String firmwareVersion = object.get(SelfCareServiceDevice.FIRMWARE_VERSION) == null ? "" : object.get(SelfCareServiceDevice.FIRMWARE_VERSION).getAsJsonObject().get("_value").getAsString();
        SCFileSearchForm searchParameter = new SCFileSearchForm();
        searchParameter.manufacturer = manufacturer;
        searchParameter.oui = oui;
        searchParameter.productClass = productClass;
        searchParameter.modelName = modelName;
        searchParameter.limit = 1;
        try {
            List<SCFile> scFiles = selfCareServiceFile.search(searchParameter);
            if (scFiles.size() > 0) {
                if (scFiles.get(0).firmwareVersion.equals(firmwareVersion)) {
                    return true;
                }
            }
        } catch (Exception e) {
        }
        return false;
    }

    public boolean checkOnline(JsonObject object) {
        try {
            if (checkOnlineType.equals("inform")) {
                String lastInform = object.get(SelfCareServiceDevice.LAST_INFORM).getAsString();
                String lastConnectionRequest = object.get(SelfCareServiceDevice.LAST_CONNECTION_REQUEST) == null ? "" : object.get(SelfCareServiceDevice.LAST_CONNECTION_REQUEST).getAsString();
                JsonObject periodicInformIntervalObj = object.get(SelfCareServiceDevice.PERIODIC_INFORM_INTERVAL).getAsJsonObject();
                if (periodicInformIntervalObj != null
                        && "".equals(periodicInformIntervalObj.get("_value").getAsString())) {
                    return false;
                }
                int periodicInformInterval = Integer.valueOf(periodicInformIntervalObj.get("_value").getAsString());
                if (lastInform.isEmpty()) {
                    return false;
                }

                Date lastInformTime = parseDate(lastInform);

                if (!lastConnectionRequest.isEmpty()) {
                    Date lastConnectionRequestTime = parseDate(lastConnectionRequest);
                    if (lastConnectionRequestTime.getTime() > lastInformTime.getTime()) {
                        return false;
                    }
                }
                Date now = new Date();
                if ((now.getTime() - lastInformTime.getTime()) > periodicInformInterval * 1000 * 1.1) {
                    return false;
                }
            } else {
                String deviceId = object.get("_id").getAsString();
                return getXmppStatus(deviceId);
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    public int countDevice(SCDeviceSearchForm searchParameter) throws ParseException {
        ResponseEntity<String> responseEntity = doSearch(searchParameter);
        return Integer.valueOf(responseEntity.getHeaders().get("totalAll").get(0));
    }

    public List<SCDevice> parseInforDevice(String body) throws ParseException {
        JsonArray array = new Gson().fromJson(body, JsonArray.class);
        List<SCDevice> listDevice = new ArrayList<>();
        for (int i = 0; i < array.size(); i++) {
            SCDevice scDevice = new SCDevice();
            JsonObject object = array.get(i).getAsJsonObject();
            String id = object.get("_id").getAsString();
            scDevice.id = id;
            String softwareVersion = object.get(SelfCareServiceDevice.FIRMWARE_VERSION) == null ? "" : object.get(SelfCareServiceDevice.FIRMWARE_VERSION).getAsJsonObject().get("_value").getAsString();
            scDevice.firmwareVersion = softwareVersion;
            String serialNumber = object.get(SelfCareServiceDevice.SERIAL_NUMBER) == null ? "" : object.get(SelfCareServiceDevice.SERIAL_NUMBER).getAsString();
            scDevice.serialNumber = serialNumber;
            String manufacturer = object.get(SelfCareServiceDevice.MANUFACTURER) == null ? "" : object.get(SelfCareServiceDevice.MANUFACTURER).getAsString();
            scDevice.manufacturer = manufacturer;
            String oui = object.get(SelfCareServiceDevice.OUI) == null ? "" : object.get(SelfCareServiceDevice.OUI).getAsString();
            scDevice.oui = oui;
            String productClass = object.get(SelfCareServiceDevice.PRODUCT_CLASS) == null ? "" : object.get(SelfCareServiceDevice.PRODUCT_CLASS).getAsString();
            scDevice.productClass = productClass;
            // Check MAC is null, is string or is object
            String mac = object.get(SelfCareServiceDevice.MAC_ADDRESS) == null ? "" : (object.get(SelfCareServiceDevice.MAC_ADDRESS).isJsonObject() ? object.get(SelfCareServiceDevice.MAC_ADDRESS).getAsJsonObject().get("_value").getAsString() : object.get(SelfCareServiceDevice.MAC_ADDRESS).getAsString());
            scDevice.mac = mac;
            String modelName = object.get(SelfCareServiceDevice.MODEL_NAME) == null ? "" : object.get(SelfCareServiceDevice.MODEL_NAME).getAsJsonObject().get("_value").getAsString();
            scDevice.modelName = modelName;
            Date lastBoot = object.get(SelfCareServiceDevice.LAST_BOOT) == null ? null : parseDate(object.get(SelfCareServiceDevice.LAST_BOOT).getAsString());
            scDevice.lastBoot = lastBoot;
            Date lastBootstrap = object.get(SelfCareServiceDevice.LAST_BOOTSTRAP) == null ? null : parseDate(object.get(SelfCareServiceDevice.LAST_BOOTSTRAP).getAsString());
            scDevice.lastBootstrap = lastBootstrap;
            Date lastInform = object.get(SelfCareServiceDevice.LAST_INFORM) == null ? null : parseDate(object.get(SelfCareServiceDevice.LAST_INFORM).getAsString());
            scDevice.lastInform = lastInform;
            Date lastSynchronize = object.get(SelfCareServiceDevice.LAST_SYNCHRONIZE) == null ? null : parseDate(object.get(SelfCareServiceDevice.LAST_SYNCHRONIZE).getAsString());
            scDevice.lastSynchronize = lastSynchronize;
            Date registered = object.get(SelfCareServiceDevice.CREATED) == null ? null : parseDate(object.get(SelfCareServiceDevice.CREATED).getAsString());
            scDevice.registered = registered;
            String connectionRequest = object.get(SelfCareServiceDevice.CONNECTION_REQUEST_URL).getAsJsonObject() == null ? "" : object.get(SelfCareServiceDevice.CONNECTION_REQUEST_URL).getAsJsonObject().get("_value").getAsString();
            scDevice.connectionRequest = connectionRequest;
            String ip = object.get(SelfCareServiceDevice.STB_IP) == null ? "" : object.get(SelfCareServiceDevice.STB_IP).getAsJsonObject().get("_value").getAsString();
            scDevice.ip = ip;
            String account = object.get(SelfCareServiceDevice.ACCOUNT) == null ? "" : object.get(SelfCareServiceDevice.ACCOUNT).getAsJsonObject().get("_value").getAsString();
            scDevice.account = account;

            if (checkOnline(object)) {
                scDevice.status = "Online";
            } else {
                scDevice.status = "Offline";
            }

            if (checkLastFirmwareVersion(object)) {
                scDevice.isLastFirmwareVersion = true;
            } else {
                scDevice.isLastFirmwareVersion = false;
            }

            // parse label
            LinkedHashSet<String> tags = new LinkedHashSet<String>();
            if (object.get(SelfCareServiceDevice.TAGS) != null) {
                JsonArray arr = object.get(SelfCareServiceDevice.TAGS).getAsJsonArray();
                for (int i1 = 0; i1 < arr.size(); i1++) {
                    tags.add(arr.get(i1).getAsString());
                }
                scDevice.labels = tags;
            }

            // parse labelId
            LinkedHashSet<Long> labelIds = new LinkedHashSet<Long>();
            if (object.get(SelfCareServiceDevice.LABELID) != null) {
                JsonArray arr = object.get(SelfCareServiceDevice.LABELID).getAsJsonArray();
                for (int i1 = 0; i1 < arr.size(); i1++) {
                    labelIds.add(arr.get(i1).getAsLong());
                }
                scDevice.labelIds = labelIds;
            }
            listDevice.add(scDevice);
        }
        return listDevice;
    }

    public SCPing pingToDevice(String deviceId) throws ParseException {
        SCDevice device = getDevice(deviceId);
        SCPing scPing = new SCPing();
        if (device == null) {
            scPing.httpStatus = 500;
            scPing.result = "Cannot found this device!";
            return scPing;
        }
        String ipDevice = device.ip;
        ResponseEntity response = acsClient.pingDevice(ipDevice);
        String body = (String) response.getBody();
        if (!response.toString().contains("200 OK,PING")) {
            body = "Error Ping To " + ipDevice + " . Please Try Again Later !";
        }
        scPing.httpStatus = response.getStatusCodeValue();
        scPing.result = body;
        return scPing;
    }

    public void deleteDevice(String deviceId, String mode) throws Exception {
        Map<String, String> acsQuery = new HashMap<String, String>();
        acsQuery.put("query", "{\"_id\":\"" + deviceId + "\"}");
        JsonArray arrayTmpObject = new Gson().fromJson(acsClient.search("devices", acsQuery).getBody(), JsonArray.class);
        if (arrayTmpObject.size() > 0) {
            JsonObject body = arrayTmpObject.get(0).getAsJsonObject();
            JsonObject inforObject = body.get("_deviceId").getAsJsonObject();
            String productClass = inforObject.get("_ProductClass") != null ? inforObject.get("_ProductClass").getAsString() : "";
            String oui = inforObject.get("_OUI").getAsString() != null ? inforObject.get("_OUI").getAsString() : "";
            String firmwareVersion = body.get("summary.softwareVersion") != null ? body.get("summary.softwareVersion").getAsJsonObject().get("_value").getAsString() : "";
            DeviceType currenDeviceType = deviceTypeService.findByPk(oui, productClass);
            if (currenDeviceType != null) {
                DeviceTypeVersion currentDeviceTypeVersion = deviceTypeVersionService.findByPk(currenDeviceType.id, firmwareVersion);
                if (currentDeviceTypeVersion != null) {
                    List<Tag> lTag = tagService.findByDeviceTypeVersionIdAssignedSynchronized(currentDeviceTypeVersion.id);
                    for (Tag tag : lTag) {
                        String cacheId = deviceId + "-" + tag.id.toString();
                        try {
                            Set<Parameter> profile = dataModelService.getProfileOfDevices(deviceId, tag.id);
                            ssdcCache.remove(cacheId, new HashSet<Parameter>().getClass());
                        } catch (Exception e) {
                            throw e;
                        }
                    }
                }
            }
        }
        acsClient.deleteDevice(deviceId);
        if (mode.equals("permanently")) {
            BlacklistDevice blacklistDevice = new BlacklistDevice();
            blacklistDevice.deviceId = deviceId;
            blackListDeviceService.create(blacklistDevice);
        }
    }

    /**
     * true = online false = offline
     *
     * @param deviceId
     * @return
     */
    public boolean getXmppStatus(String deviceId) {
        if (checkOnlineType.equals("plugin")) {
            String url = String.format(urlPresence + "?jid=%s@%s&type=xml", deviceId, xmppDomain);
            RestTemplate restTemplate = new RestTemplate();
            String xmlResult = restTemplate.getForObject(url, String.class);
            try {
                SAXReader reader = new SAXReader();
                Document document = reader.read(new InputSource(new StringReader(xmlResult)));
                Node rootElement = document.selectSingleNode("presence");
                if (rootElement == null) {
                    return false;
                }
                String type = rootElement.valueOf("@type");
                if (!Strings.isNullOrEmpty(type)) {
                    if (type.equals("unavailable") || type.equals("error")) {
                        return false;
                    }
                }
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            ConnectionConfiguration config = new ConnectionConfiguration(xmppHost, xmppPort);
            XMPPConnection connection = new XMPPConnection(config);
            try {
                connection.connect();
                connection.login(xmppUsername, xmppPassword, xmppResource);
                Thread.sleep(10000);
                String username = String.format("%s@%s", deviceId, xmppDomain);
                Roster roster = connection.getRoster();
                Presence user = roster.getPresence(username);
                if (user.isAvailable()) {
                    return true;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
        return false;
    }
}
