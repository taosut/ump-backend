package vn.ssdc.vnpt.rabbitmq.endpoints;

import com.google.common.base.Strings;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.swagger.annotations.Api;
import org.jivesoftware.smack.AccountManager;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.IQ;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;
import vn.ssdc.vnpt.AcsClient;
import vn.ssdc.vnpt.devices.model.*;
import vn.ssdc.vnpt.devices.services.*;
import vn.ssdc.vnpt.logging.services.LoggingPolicyService;
import vn.ssdc.vnpt.file.model.BackupFile;
import vn.ssdc.vnpt.policy.model.PolicyJob;
import vn.ssdc.vnpt.policy.model.PolicyPreset;
import vn.ssdc.vnpt.policy.services.PolicyJobService;
import vn.ssdc.vnpt.policy.services.PolicyTaskService;
import vn.ssdc.vnpt.provisioning.services.ProvisioningService;
import vn.ssdc.vnpt.selfCare.model.searchForm.SCDeviceSearchForm;
import vn.ssdc.vnpt.websocket.MyStompSessionHandler;
import vn.ssdc.vnpt.xmpp.models.ConnectionIQ;
import vn.vnpt.ssdc.core.ObjectCache;
import vn.vnpt.ssdc.event.AMQPSubscribes;
import vn.vnpt.ssdc.event.Event;

import javax.ws.rs.*;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import org.apache.commons.collections.map.HashedMap;
import org.jivesoftware.smack.Roster;
import vn.ssdc.vnpt.label.model.Label;
import vn.ssdc.vnpt.label.services.LabelService;
import vn.ssdc.vnpt.mapping.services.AccountMappingService;
import vn.ssdc.vnpt.qos.model.QosKpiDataELK;
import vn.ssdc.vnpt.qos.model.QosGraph;
import vn.ssdc.vnpt.qos.model.QosKpi;
import vn.ssdc.vnpt.qos.services.QosELKService;
import vn.ssdc.vnpt.qos.services.QosGraphService;
import vn.ssdc.vnpt.qos.services.QosKpiService;
import vn.ssdc.vnpt.selfCare.model.SCDevice;
import vn.ssdc.vnpt.selfCare.model.searchForm.SCDeviceGroupSearchForm;
import vn.ssdc.vnpt.selfCare.services.SelfCareServiceDevice;
import vn.ssdc.vnpt.selfCare.services.SelfCareServiceDeviceGroup;

/**
 * Created by THANHLX on 5/26/2017.
 */
@Component
@Path("cwmpmq")
@Produces(APPLICATION_JSON)
@Consumes({APPLICATION_JSON, TEXT_PLAIN})
@Api("Cwmp MQ")
public class CwmpEndPoint {

    private static final Logger logger = LoggerFactory.getLogger(CwmpEndPoint.class);
    @Autowired
    private AcsClient acsClient;

    @Autowired
    private ProvisioningService provisioningService;

    @Autowired
    private DiagnosticService diagnosticService;

    @Autowired
    private PolicyTaskService policyTaskService;

    @Autowired
    private DeviceTypeService deviceTypeService;

    @Autowired
    private BlackListDeviceService blackListDeviceService;

    @Autowired
    private DataModelService dataModelService;

    @Autowired
    private ParameterDetailService parameterDetailService;

    @Autowired
    private TagService tagService;

    @Autowired
    private PolicyJobService policyJobService;

    @Autowired
    private LoggingPolicyService loggingPolicyService;

    @Autowired
    private DeviceTypeVersionService deviceTypeVersionService;

    @Autowired
    private ObjectCache ssdcCache;

    @Autowired
    private vn.ssdc.vnpt.mapping.services.IpMappingService ipMappingService;

    @Autowired
    private AccountMappingService accountMappingService;

    @Autowired
    private LabelService labelService;

    @Autowired
    private SelfCareServiceDevice selfCareServiceDevice;

    @Autowired
    private SelfCareServiceDeviceGroup selfCareServiceDeviceGroup;

    @Autowired
    private QosGraphService qosGraphService;

    @Autowired
    private QosKpiService qosKpiService;

    @Autowired
    private QosELKService qosELKService;

    @Value("${xmpp.host}")
    private String xmppHost;

    @Value("${xmpp.port}")
    private int xmppPort;

    @Value("${xmpp.username}")
    private String xmppUsername;

    @Value("${xmpp.password}")
    private String xmppPassword;

    @Value("${xmpp.resource}")
    private String xmppResource;

    @Value("${xmpp.domain}")
    private String xmppDomain;

    @GET
    @Path("/{deviceId}/newDeviceRegistered")
    public void newDeviceRegistered(@PathParam("deviceId") String deviceId) {
        processNewDeviceRegistered(deviceId);
    }

    public void processNewDeviceRegistered(String deviceId) {
        if (blackListDeviceService.findByDeviceId(deviceId).size() == 0) {
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
                if (currenDeviceType == null) {
                    this.acsClient.refreshAll(deviceId, true);
                } else {
                    DeviceTypeVersion currentDeviceTypeVersion = deviceTypeVersionService.findByPk(currenDeviceType.id, firmwareVersion);
                    if (currentDeviceTypeVersion == null) {
                        this.acsClient.refreshAll(deviceId, true);
                    } else {
                        Map<String, Object> datas = getInitDeviceDatas(currentDeviceTypeVersion, deviceId);
                        Map<String, String> mapParam = new HashMap<String, String>();
                        for (Map.Entry<String, JsonElement> entry : body.entrySet()) {
                            if (!dataModelService.ignoredParam.contains(entry.getKey()) && body.get(entry.getKey()).isJsonObject()) {
                                mapParam = loop(mapParam, entry.getValue().getAsJsonObject(), entry.getKey() + ".");
                            }
                        }

                        for (Map.Entry<String, String> entry : mapParam.entrySet()) {
                            datas.put(entry.getKey(), entry.getValue());
                        }
                        acsClient.initDevice(deviceId, datas);
                        synchronizeDevice(deviceId);
                    }
                }
                if (productClass.equals("STB")) {
                    registerNewDeviceInXMPP(deviceId);
                }
            }
        } else {
            logger.info("New device registered #{} in black list", deviceId);
            acsClient.deleteDevice(deviceId);
        }
    }

    @AMQPSubscribes(queue = "new-device-registered")
    public void processMessageNewDeviceRegistered(Event event) {
        logger.info("New device registered #{}", event.message.get("deviceId"));
        String deviceId = event.message.get("deviceId");
        if (blackListDeviceService.findByDeviceId(deviceId).size() == 0) {
            String oui = event.message.get("oui");
            String productClass = event.message.get("productClass");
            Type type = new TypeToken<List<List<String>>>() {
            }.getType();
            List<List<String>> parameterValues = new Gson().fromJson(event.message.get("parameterValues"), type);
            processAMQPNewDeviceRegistered(deviceId, oui, productClass, parameterValues);
            updateLabelOfDevice(deviceId);
        } else {
            logger.info("New device registered #{} in black list", deviceId);
            acsClient.deleteDevice(deviceId);
        }
    }

    public void processAMQPNewDeviceRegistered(String deviceId, String oui, String productClass, List<List<String>> parameterValues) {
        DeviceType currenDeviceType = deviceTypeService.findByPk(oui, productClass);
        if (currenDeviceType == null) {
            this.acsClient.refreshAll(deviceId, true);
        } else {
            Map<String, String> acsQuery = new HashMap<String, String>();
            acsQuery.put("query", "{\"_id\":\"" + deviceId + "\"}");
            JsonArray arrayTmpObject = new Gson().fromJson(acsClient.search("devices", acsQuery).getBody(), JsonArray.class);
            if (arrayTmpObject.size() > 0) {
                JsonObject body = arrayTmpObject.get(0).getAsJsonObject();
                String firmwareVersion = body.get("summary.softwareVersion") != null ? body.get("summary.softwareVersion").getAsJsonObject().get("_value").getAsString() : "";
                if (!firmwareVersion.isEmpty()) {
                    DeviceTypeVersion currentDeviceTypeVersion = deviceTypeVersionService.findByPk(currenDeviceType.id, firmwareVersion);
                    if (currentDeviceTypeVersion == null) {
                        this.acsClient.refreshAll(deviceId, true);
                    } else {
                        Map<String, Object> datas = getInitDeviceDatas(currentDeviceTypeVersion, deviceId);
                        synchronizeDevice(deviceId);
                        for (List<String> parameterValue : parameterValues) {
                            datas.put(parameterValue.get(0) + "._value", parameterValue.get(1));
                        }
                        acsClient.initDevice(deviceId, datas);
                        synchronizeDevice(deviceId);
                    }
                }
            }
        }
        if (productClass.equals("STB")) {
            registerNewDeviceInXMPP(deviceId);
        }
    }

    public Map<String, Object> getInitDeviceDatas(DeviceTypeVersion deviceTypeVersion, String deviceId) {
        Map<String, Object> datas = new HashMap<>();
        Map<String, Parameter> parameters = deviceTypeVersion.parameters;
        for (Map.Entry<String, Parameter> entry : parameters.entrySet()) {
            Parameter parameter = entry.getValue();
            if (parameter.path.substring(parameter.path.length() - 1).equals(".")) {
                datas.put(parameter.path + "_object", true);
                if (parameter.access != null && parameter.access.equals("true")) {
                    datas.put(parameter.path + "_writable", true);
                } else {
                    datas.put(parameter.path + "_writable", false);
                }
                if (parameter.instance != null && parameter.instance) {
                    datas.put(parameter.path + "_instance", true);
                } else {
                    datas.put(parameter.path + "_instance", false);
                }
                TimeZone tz = TimeZone.getTimeZone("UTC");
                DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"); // Quoted "Z" to indicate UTC, no timezone offset
                df.setTimeZone(tz);
                datas.put(parameter.path + "_timestamp", df.format(new Date()));
            } else {
                datas.put(parameter.path + "._value", parameter.defaultValue);
                datas.put(parameter.path + "._type", "xsd:" + parameter.dataType);
                if (parameter.access != null && parameter.access.equals("true")) {
                    datas.put(parameter.path + "._writable", true);
                } else {
                    datas.put(parameter.path + "._writable", false);
                }
                TimeZone tz = TimeZone.getTimeZone("UTC");
                DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"); // Quoted "Z" to indicate UTC, no timezone offset
                df.setTimeZone(tz);
                datas.put(parameter.path + "._timestamp", df.format(new Date()));
            }
        }
        return datas;
    }

    private Map<String, String> loop(Map<String, String> mapParam, JsonObject body, String key) {
        for (Map.Entry<String, JsonElement> entry : body.entrySet()) {
            if (!dataModelService.ignoredParam.contains(entry.getKey()) && body.get(entry.getKey()).isJsonObject()) {
                if (body.get(entry.getKey()).getAsJsonObject().get("_value") != null) {
                    String path = key + entry.getKey() + "._value";
                    mapParam.put(path, body.get(entry.getKey()).getAsJsonObject().get("_value").getAsString());
                } else {
                    loop(mapParam, entry.getValue().getAsJsonObject(), key + entry.getKey() + ".");
                }
            }
        }
        return mapParam;
    }

    public void synchronizeDevice(String deviceId) {
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
            if (currenDeviceType == null) {
                this.acsClient.refreshAll(deviceId, false);
            } else {
                DeviceTypeVersion currentDeviceTypeVersion = deviceTypeVersionService.findByPk(currenDeviceType.id, firmwareVersion);
                if (currentDeviceTypeVersion == null) {
                    this.acsClient.refreshAll(deviceId, false);
                } else {
                    List<Tag> lTag = tagService.findByDeviceTypeVersionIdAssignedSynchronized(currentDeviceTypeVersion.id);
                    List<String> listObjects = new ArrayList<String>();
                    List<String> listParameters = new ArrayList<String>();
                    ParameterDetail parameterDetail = parameterDetailService.findByParams("InternetGatewayDevice.DeviceInfo.", currentDeviceTypeVersion.id);
                    if (parameterDetail != null) {
                        listObjects.add("InternetGatewayDevice.DeviceInfo.");
                    } else {
                        listObjects.add("Device.DeviceInfo.");
                    }
                    //Set get list available interface
                    if (lTag != null && lTag.size() != 0) {
                        for (int index = 0; index < lTag.size(); index++) {
                            Map<String, Parameter> parameters = lTag.get(index).parameters;
                            for (Map.Entry<String, Parameter> tmp : parameters.entrySet()) {
                                Parameter parameter = new Gson().fromJson(new Gson().toJson(tmp.getValue()), Parameter.class);
                                if (!"object".equals(parameter.dataType)) {
                                    if (parameter.tr069Name.contains("{i}")) {
                                        String parentPath = parameter.tr069ParentObject.substring(0, parameter.tr069ParentObject.indexOf("{i}"));;
                                        if (!listObjects.contains(parentPath)) {
                                            Boolean isDescent = false;
                                            for (int i = 0; i < listObjects.size(); i++) {
                                                if (parentPath.contains(listObjects.get(i))) {
                                                    isDescent = true;
                                                }
                                                if (listObjects.get(i).contains(parentPath)) {
                                                    listObjects.remove(i);
                                                }
                                            }
                                            if (!isDescent) {
                                                listObjects.add(parentPath);
                                            }
                                        }
                                    } else {
                                        listParameters.add(parameter.path);
                                    }
                                }
                            }
                        }
                        for (String path : listObjects) {
                            acsClient.refreshObject(deviceId, path, true);
                        }
                        if (listParameters.size() > 0) {
                            acsClient.getParameterValues(deviceId, listParameters, true);
                        }
                    }
                }
            }
        }
        acsClient.synchronizeDevice(deviceId);
    }

    @Value("${websocketUrl}")
    public String websocketUrl;

    public StompSession webSocketStompClient() throws Exception {
        WebSocketClient simpleWebSocketClient = new StandardWebSocketClient();
        List<Transport> transports = new ArrayList<>(1);
        transports.add(new WebSocketTransport(simpleWebSocketClient));

        SockJsClient sockJsClient = new SockJsClient(transports);
        WebSocketStompClient stompClient = new WebSocketStompClient(sockJsClient);
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        StompSessionHandler sessionHandler = new MyStompSessionHandler();
        return stompClient.connect(websocketUrl, sessionHandler).get();
    }

    @AMQPSubscribes(queue = "completed-task")
    public void processMessageCompletedTask(Event event) throws Exception {
        logger.info("Completed task {} for #{}", new Object[]{event.message.get("taskName"), event.message.get("deviceId")});
        Map<String, String> message = event.message;

        if ("refreshAll".equals(message.get("taskName"))) {
            dataModelService.addDataModel(message.get("deviceId"));
            provisioningService.createProvisioningTasks(message.get("deviceId"));
        } else {
            if ("updateDiagnosticResult".equals(message.get("taskName"))) {
                Long diagnosticTaskId = Long.valueOf(message.get("diagnosticTaskId")).longValue();
                diagnosticService.updateResult(diagnosticTaskId);
                webSocketStompClient().send("/app/diagnosticCompleted/" + diagnosticService.get(diagnosticTaskId).taskId, message);
            }
        }

        Map<String, String> acsQuery = new HashMap<String, String>();
        String deviceId = message.get("deviceId");
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
                            ssdcCache.put(cacheId, profile, new HashSet<Parameter>().getClass());
                        } catch (Exception e) {
                            logger.info(e.getMessage());
                        }
                    }
                }
            }
        }

        webSocketStompClient().send("/app/taskCompleted/" + message.get("taskId"), message);
    }

    @AMQPSubscribes(queue = "fault-task")
    public void processMessageFaultTask(Event event) throws Exception {
        logger.info("Fault task {} for #{}", new Object[]{event.message.get("taskName"), event.message.get("deviceId")});
        Map<String, String> message = event.message;
        if ("createDiagnostic".equals(message.get("taskName"))) {
            DiagnosticTask diagnosticTask = diagnosticService.findByTaskId(message.get("taskId"));
            diagnosticTask.status = 2;
            diagnosticService.update(diagnosticTask.id, diagnosticTask);
        }
        webSocketStompClient().send("/app/taskFault/" + message.get("taskId"), message);
    }

    @GET
    @Path("/updateLabel/{deviceId}")
    public void updateLabel(@PathParam("deviceId") String deviceId) throws Exception, MalformedURLException, UnknownHostException {
        updateLabelOfDevice(deviceId);
    }

    @Autowired
    private SelfCareServiceDevice selfCareService;

    @GET
    @Path("/initLabelIds")
    public void initLabel() throws ParseException {
        SCDeviceSearchForm searchParameter = new SCDeviceSearchForm();
        List<SCDevice> listDevices = selfCareService.searchDevice(searchParameter);
        for (SCDevice device : listDevices) {
            updateLabelOfDevice(device.id);
        }
    }

    @AMQPSubscribes(queue = "inform")
    public void processMessageInform(Event event) throws Exception, MalformedURLException, UnknownHostException {
        String deviceId = event.message.get("deviceId");
        logger.info("Inform for #{}", new Object[]{deviceId});
        Map<String, String> message = event.message;
        String[] eventCodes = new Gson().fromJson(message.get("eventCodes"), String[].class
        );
        for (String eventCode : eventCodes) {
            if ("0 BOOTSTRAP".equals(eventCode) || "1 BOOT".equals(eventCode)) {
                updateLabelOfDevice(deviceId);
                if (deviceTypeVersionService.findbyDevice(deviceId) == null) {
                    processNewDeviceRegistered(deviceId);
                }
            }
            if ("0 BOOTSTRAP".equals(eventCode)) {
                synchronizeDevice(deviceId);
                provisioningService.createProvisioningTasks(deviceId);
                webSocketStompClient().send("/app/factoryResetCompleted/" + deviceId, message);
            } else if ("1 BOOT".equals(eventCode)) {
                webSocketStompClient().send("/app/rebootCompleted/" + deviceId, message);
            } else if ("8 DIAGNOSTICS COMPLETE".equals(eventCode)) {
                DiagnosticTask task = diagnosticService.findInProcess(deviceId);
                if (task != null) {
                    List<String> listPath = new ArrayList<String>();
                    for (Map.Entry<String, Parameter> entry : task.parameterFull.entrySet()) {
                        listPath.add(entry.getValue().path);
                    }
                    this.acsClient.updateDiagnosticResult(task.deviceId, task.id, listPath, true);
                }
            } else if ("6 CONNECTION REQUEST".equals(eventCode)) {
                webSocketStompClient().send("/app/connectionRequest/" + deviceId, message);
            }
        }
    }

    public void updateLabelOfDevice(String deviceId) {
        Map<String, String> acsQuery = new HashMap<String, String>();
        acsQuery.put("query", "{\"_id\":\"" + deviceId + "\"}");
        JsonArray arrayTmpObject = new Gson().fromJson(acsClient.search("devices", acsQuery).getBody(), JsonArray.class);
        if (arrayTmpObject.size() > 0) {
            JsonObject body = arrayTmpObject.get(0).getAsJsonObject();
            acsClient.resetTags(deviceId, new HashedMap());
            JsonObject inforObject = body.get("_deviceId").getAsJsonObject();
            String productClass = inforObject.get("_ProductClass") != null ? inforObject.get("_ProductClass").getAsString() : "";
            if (productClass.equals("STB")) {
                String accountMapping = body.get("Device").getAsJsonObject().get("MyTV").getAsJsonObject().get("Username").getAsJsonObject().get("_value").getAsString();
                if (Strings.isNullOrEmpty(accountMapping) || !accountMappingService.checkAccountBelongAccountMapping(accountMapping)) {
                    String ip = body.get("summary.ip").getAsJsonObject().get("_value").getAsString();
                    if (Strings.isNullOrEmpty(ip) || !ipMappingService.checkIpBelongSubnet(ip)) {
                        addUngroupLabel(deviceId);
                    } else {
                        ipMappingService.addLabel(deviceId, ip);
                    }
                } else {
                    accountMappingService.addLabel(deviceId, accountMapping);
                }

            } else {
                String ip = body.get("summary.ip").getAsJsonObject().get("_value").getAsString();
                ipMappingService.addLabel(deviceId, ip);
            }

        }
    }

    public void addUngroupLabel(String deviceId) {
        Label label = labelService.findUngroupLabel();
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

    @AMQPSubscribes(queue = "transfer-completed")
    public void transferCompleted(Event event) throws Exception, MalformedURLException, UnknownHostException {
        String deviceId = event.message.get("deviceId");
        logger.info("Transfer completed of #{}", new Object[]{deviceId});
        Map<String, String> data = event.message;
        String commandKey = data.get("commandKey");
        String taskId = commandKey.substring(
                commandKey.indexOf("TASK_ID_") + "TASK_ID_".length(),
                commandKey.indexOf(" ", commandKey.indexOf("TASK_ID_"))).trim();
        Map<String, String> message = new HashMap<>();
        message.put("status", data.get("status"));
        webSocketStompClient().send("/app/transferCompleted/" + taskId, message);
    }

    @AMQPSubscribes(queue = "connection-request-with-xmpp")
    public void processMessageConnectionRequestError(Event event) throws XMPPException {
        logger.info("Send connection request with xmpp for #{}", new Object[]{event.message.get("deviceId")});
        ConnectionConfiguration config = new ConnectionConfiguration(xmppHost, xmppPort);
        XMPPConnection connection = new XMPPConnection(config);
        connection.connect();
        connection.login(xmppUsername, xmppPassword, xmppResource);
        IQ req = new ConnectionIQ();
        String deviceId = event.message.get("deviceId");
        req.setTo(String.format("%s@%s/%s", deviceId, xmppDomain, xmppResource));
        connection.sendPacket(req);
    }

    @AMQPSubscribes(queue = "start-policy")
    public void processMessageStartPolicy(Event event) {
        logger.info("Start policy #{} of #{}", event.message.get("policyJobId"), event.message.get("deviceId"));
        String deviceId = event.message.get("deviceId");
        long policyJobId = Long.valueOf(event.message.get("policyJobId")).longValue();
        PolicyJob policyJob = policyJobService.get(policyJobId);
        policyJob.currentNumber = (policyJob.currentNumber + 1);
        policyJobService.update(policyJobId, policyJob);
        if (policyJob.currentNumber >= policyJob.maxNumber) {
            policyJobService.deletePreset(policyJobId);
        }
        if (policyJob.limited != null) {
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date date = new Date();
            date.setTime(Long.valueOf(event.message.get("time")).longValue());
            int count = loggingPolicyService.countExistedTask(deviceId, policyJobId, df.format(date));
            if ((count + 1) >= policyJob.limited) {
                PolicyPreset policyPreset = policyJobService.createPolicyPreset(policyJob);
                policyPreset.precondition = "{\"$and\":[{\"_id\":{\"$ne\":\"" + deviceId + "\"}}," + policyPreset.precondition + "]}";
                acsClient.updatePolicyPreset(policyPreset, "Policy Job " + policyJobId);
            }
        }
    }

    @AMQPSubscribes(queue = "restore")
    public void processMessageRestore(Event event) {
        logger.info("Restore of #{}", event.message.get("deviceId"));
        String deviceId = event.message.get("deviceId");
        BackupFile backupFile = acsClient.searchBackupFile(deviceId);
        if (backupFile != null) {
            acsClient.downloadUrlFile(deviceId, "3 Vendor Configuration File", backupFile.url, "", "", "", "", "", 0, "", 0, true, "", "", true);
        }
    }

    @GET
    @Path("/{deviceId}/synchronize")
    public void synchronize(@PathParam("deviceId") String deviceId) {
        synchronizeDevice(deviceId);
    }

    @AMQPSubscribes(queue = "configuration-policy")
    public void processMessageConfigurationPolicy(Event event) {
        logger.info("Run policy configuration #{} of #{}", event.message.get("policyJobId"), event.message.get("deviceId"));
        String deviceId = event.message.get("deviceId");
        String policyJobId = event.message.get("policyJobId");
        PolicyJob policyJob = policyJobService.get(Long.valueOf(policyJobId).longValue());
        if (policyJob.actionName.equals("parameters")) {
            policyJobService.processParametersPolicy(deviceId, policyJob);
        } else {
            policyJobService.processConfigurationPolicy(deviceId, policyJob);
        }
    }

    @GET
    @Path("/processConfigPolicy/{deviceId}/{policyJobId}")
    public void processConfigPolicy(@PathParam("deviceId") String deviceId, @PathParam("policyJobId") String policyJobId) {
        PolicyJob policyJob = policyJobService.get(Long.valueOf(policyJobId).longValue());
        if (policyJob.actionName.equals("parameters")) {
            policyJobService.processParametersPolicy(deviceId, policyJob);
        } else {
            policyJobService.processConfigurationPolicy(deviceId, policyJob);
        }
    }

    @Value("${checkOnlineType}")
    private String checkOnlineType;

    private void registerNewDeviceInXMPP(String deviceId) {
        try {
            ConnectionConfiguration config = new ConnectionConfiguration(xmppHost, xmppPort);
            XMPPConnection connection = new XMPPConnection(config);
            connection.connect();
            AccountManager accountManager = new AccountManager(connection);
            accountManager.createAccount(deviceId, deviceId);
            connection.disconnect();

            if (checkOnlineType.equals("roster")) {
                connection.connect();
                connection.login(deviceId, deviceId, xmppResource);
                connection.getRoster().setSubscriptionMode(Roster.SubscriptionMode.accept_all);
                connection.disconnect();

                connection.connect();
                connection.login(xmppUsername, xmppPassword, xmppResource);
                connection.getRoster().setSubscriptionMode(Roster.SubscriptionMode.accept_all);
                connection.disconnect();

                connection.connect();
                connection.login(xmppUsername, xmppPassword, xmppResource);
                String userJID = String.format("%s@%s/%s", deviceId, xmppDomain, xmppResource);
                connection.getRoster().createEntry(userJID, deviceId, null);
                connection.disconnect();

                connection.connect();
                connection.login(deviceId, deviceId, xmppResource);
                String adminJID = String.format("%s@%s/%s", xmppUsername, xmppDomain, xmppResource);
                connection.getRoster().createEntry(adminJID, xmppPassword, null);
            }
            connection.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @AMQPSubscribes(queue = "measurement-completed")
    public void listenQosQueue(Event event) {
        logger.info("Reading qos elk message #{} of #{}", event.message.get("deviceId"), event.message.get("parameterValues"));
        String deviceId = event.message.get("deviceId");
        try {
            SCDevice device = selfCareServiceDevice.getDevice(deviceId);
            // STB
            if (device.manufacturer.equals("VNPT Technology")) {
                qosELKService.processDataFromMessageQueue(device, event);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void listenQosQueue1(Event event, String timeStamp) {
        logger.info("Reading qos elk message #{} of #{}", event.message.get("deviceId"), event.message.get("parameterValues"));
        String deviceId = event.message.get("deviceId");
        try {
            SCDevice device = selfCareServiceDevice.getDevice(deviceId);
            // STB
            if (device.manufacturer.equals("VNPT Technology")) {
                qosELKService.processDataFromMessageQueue1(device, event, timeStamp);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
