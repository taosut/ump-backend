package vn.ssdc.vnpt.selfCare.endpoints;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import java.text.ParseException;
import java.util.List;
import java.util.Map;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.IQ;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.client.HttpStatusCodeException;
import vn.ssdc.vnpt.AcsClient;
import vn.ssdc.vnpt.devices.model.DeviceTypeVersion;
import vn.ssdc.vnpt.devices.model.DiagnosticTask;
import vn.ssdc.vnpt.devices.services.DiagnosticService;
import vn.ssdc.vnpt.selfCare.model.SCBackupFile;
import vn.ssdc.vnpt.selfCare.model.SCFile;
import vn.ssdc.vnpt.selfCare.services.SelfCareServiceBackupFile;
import vn.ssdc.vnpt.selfCare.services.SelfCareServiceFile;
import vn.ssdc.vnpt.selfCare.model.*;
import vn.ssdc.vnpt.selfCare.model.searchForm.SCDeviceSearchForm;

import javax.ws.rs.*;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;

import vn.ssdc.vnpt.devices.services.DeviceTypeVersionService;
import vn.ssdc.vnpt.devices.services.TagService;
import vn.ssdc.vnpt.selfCare.model.searchForm.SCDiagnosticForm;
import vn.ssdc.vnpt.selfCare.model.SCTaskForm;
import vn.ssdc.vnpt.selfCare.services.SelfCareServiceDevice;
import vn.ssdc.vnpt.xmpp.models.ConnectionIQ;

/**
 * Created by THANHLX on 11/27/2017.
 */
@Component
@Path("/self-care/devices")
@Produces(APPLICATION_JSON)
@Consumes({APPLICATION_JSON, TEXT_PLAIN})
@Api("Self-Care Devices")
public class SCDeviceEndpoint {

    @Autowired
    private AcsClient acsClient;

    @Autowired
    private DeviceTypeVersionService deviceTypeVersionService;

    @Autowired
    private TagService tagService;

    @Autowired
    private SelfCareServiceDevice selfCareService;

    @Autowired
    private DiagnosticService diagnosticService;

    @Autowired
    private SelfCareServiceBackupFile selfCareServiceBackupFile;

    @Autowired
    private SelfCareServiceFile selfCareServiceFile;

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
    @ApiOperation(value = "Read device by device id")
    @ApiResponse(code = 200, message = "Success", response = SCDevice.class)
    @Path("/{deviceId}")
    public SCDevice read(@PathParam("deviceId") String deviceId) throws ParseException {
        return selfCareService.getDevice(deviceId);
    }

    @POST
    @ApiOperation(value = "Delete device by device id with 2 mode", notes = "mode = 'temporarily' or 'permanently'")
    @ApiResponse(code = 200, message = "Success")
    @Path("/{deviceId}/delete/{mode}")
    public SCTask delete(@PathParam("deviceId") String deviceID, @PathParam("mode") String mode) throws Exception {
        if (deviceID != null && !deviceID.equals("")
                && mode != null && !mode.equals("")) {
            try {
                selfCareService.deleteDevice(deviceID, mode);
                SCTask scTask = new SCTask();
                scTask.httpStatus = 200;
                return scTask;
            } catch (Exception e) {
                throw e;
            }
        } else {
            throw new Exception("Mode not null");
        }
    }

    /**
     * Search devices
     *
     * @param searchParameter
     * @return
     */
    @POST
    @Path("/search")
    @ApiOperation(value = "Get devices")
    @ApiResponse(code = 200, message = "Success", response = SCDevice.class)
    public List<SCDevice> search(@RequestBody SCDeviceSearchForm searchParameter) throws ParseException {
        return selfCareService.searchDevice(searchParameter);
    }

    /**
     * Count devices
     *
     * @param searchParameter
     * @return
     */
    @POST
    @Path("/count")
    @ApiOperation(value = "Count devices")
    @ApiResponse(code = 200, message = "Success")
    public int count(@RequestBody SCDeviceSearchForm searchParameter) throws ParseException {
        return selfCareService.countDevice(searchParameter);
    }

    @POST
    @Path("/{deviceId}/reboot")
    @ApiOperation(value = "Reboot")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Tasks have been successfully executed", response = SCTask.class)
        ,@ApiResponse(code = 202, message = "Tasks have been queued to be executed at the next inform", response = SCTask.class)
    })
    public SCTask reboot(@PathParam("deviceId") String deviceId, @RequestBody SCTaskForm scTaskForm) {
        ResponseEntity<String> responseEntity = acsClient.reboot(deviceId, scTaskForm.now);
        return selfCareService.convertToSCTask(responseEntity);
    }

    @POST
    @Path("/{deviceId}/reset-factory")
    @ApiOperation(value = "reset-factory")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Tasks have been successfully executed", response = SCTask.class)
        ,@ApiResponse(code = 202, message = "Tasks have been queued to be executed at the next inform", response = SCTask.class)
    })
    public SCTask resetFactory(@PathParam("deviceId") String deviceId, @RequestBody SCTaskForm scTaskForm) {
        ResponseEntity<String> responseEntity = acsClient.factoryReset(deviceId, scTaskForm.now);
        return selfCareService.convertToSCTask(responseEntity);
    }

    @POST
    @Path("/{deviceId}/get-parameter-values")
    @ApiOperation(value = "GetParameterValue of device", notes = "{\"now\":false,\"parameters\":{\"parameters\":[[\"InternetGatewayDevice.DeviceInfo.SerialNumber\"],[\"InternetGatewayDevice.DeviceInfo.ModelName\"]]}}")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Tasks have been successfully executed", response = SCTask.class)
        ,@ApiResponse(code = 202, message = "Tasks have been queued to be executed at the next inform", response = SCTask.class)
    })
    public SCTask getParameterValues(@PathParam("deviceId") String deviceId, @RequestBody SCTaskForm scTaskForm) {
        List<String> parameters = (List<String>) scTaskForm.parameters.get("parameters");
        ResponseEntity<String> responseEntity = this.acsClient.getParameterValues(deviceId, parameters, scTaskForm.now);
        return selfCareService.convertToSCTask(responseEntity);
    }

    @POST
    @Path("/{deviceId}/set-parameter-values")
    @ApiOperation(value = "Set parameter values", notes = "parameters ex: {\"InternetGatewayDevice.ManagementServer.Username\":\"ump\",\"InternetGatewayDevice.ManagementServer.Password\":\"ump@2016\"}")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Tasks have been successfully executed", response = SCTask.class)
        ,@ApiResponse(code = 202, message = "Tasks have been queued to be executed at the next inform", response = SCTask.class)
    })
    public SCTask setParameterValues(@PathParam("deviceId") String deviceId, @RequestBody SCTaskForm scTaskForm) {
        ResponseEntity<String> responseEntity = acsClient.setParameterValues(deviceId, scTaskForm.parameters, scTaskForm.now);
        return selfCareService.convertToSCTask(responseEntity);
    }

    @POST
    @Path("/{deviceId}/refresh-object")
    @ApiOperation(value = "Refresh Object", notes = " \"parameters\": {\"objectName\":\"InternetGatewayDevice.DeviceInfo.\"}")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Tasks have been successfully executed", response = SCTask.class)
        ,@ApiResponse(code = 202, message = "Tasks have been queued to be executed at the next inform", response = SCTask.class)
    })
    public SCTask refreshObject(@PathParam("deviceId") String deviceId, @RequestBody SCTaskForm scTaskForm) {
        String objectName = (String) scTaskForm.parameters.get("objectName");
        ResponseEntity<String> responseEntity = this.acsClient.refreshObject(deviceId, objectName, scTaskForm.now);
        return selfCareService.convertToSCTask(responseEntity);
    }

    @POST
    @Path("/{deviceId}/add-object")
    @ApiOperation(value = "Add Object", notes = "{\"now\":false,\"parameters\":{\"objectName\":\"InternetGatewayDevice.WANDevice.5.WANConnectionDevice.1.WANIPConnection.\",\"parameterValues\":{\"Name\":\"abc\",\"Name1\":\"abc\"}}}")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Tasks have been successfully executed", response = SCTask.class)
        ,@ApiResponse(code = 202, message = "Tasks have been queued to be executed at the next inform", response = SCTask.class)
    })
    public SCTask addObject(@PathParam("deviceId") String deviceId, @RequestBody SCTaskForm scTaskForm) {
        String objectName = (String) scTaskForm.parameters.get("objectName");
        Map<String, String> parameterValues = (Map<String, String>) scTaskForm.parameters.get("parameterValues");
        ResponseEntity<String> responseEntity = this.acsClient.addObject(deviceId, objectName, parameterValues, scTaskForm.now);
        return selfCareService.convertToSCTask(responseEntity);
    }

    @POST
    @Path("/{deviceId}/delete-object")
    @ApiOperation(value = "Delete Object", notes = " \"parameters\": {\"objectName\":\"InternetGatewayDevice.DeviceInfo.\"}")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Tasks have been successfully executed", response = SCTask.class)
        ,@ApiResponse(code = 202, message = "Tasks have been queued to be executed at the next inform", response = SCTask.class)
    })
    public SCTask deleteObject(@PathParam("deviceId") String deviceId, @RequestBody SCTaskForm scTaskForm) {
        String objectName = (String) scTaskForm.parameters.get("objectName");
        ResponseEntity<String> responseEntity = this.acsClient.deleteObject(deviceId, objectName, scTaskForm.now);
        return selfCareService.convertToSCTask(responseEntity);
    }

    @POST
    @Path("/{deviceId}/downloadFile")
    @ApiOperation(value = "Download SCFile", notes = "{\"now\":false,\"parameters\":{\"fileId\":\"5a040752a747740b665ae154\",\"fileName\":\"a06518-968380GERG-new1-1FirmwareUpgradeImage\"}}")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Tasks have been successfully executed", response = SCTask.class)
        ,@ApiResponse(code = 202, message = "Tasks have been queued to be executed at the next inform", response = SCTask.class)
    })
    public SCTask downloadFile(@PathParam("deviceId") String deviceId, @RequestBody SCTaskForm scTaskForm) throws ParseException{
        String fileId = (String) scTaskForm.parameters.get("fileId");
        SCFile SCFile = selfCareServiceFile.get(fileId);
        if(SCFile != null){
            ResponseEntity<String> responseEntity = this.acsClient.downloadUrlFile(deviceId, SCFile.fileType, SCFile.url, null, null, null, null, null, 0, null, 0, true, null, null, scTaskForm.now);
            return selfCareService.convertToSCTask(responseEntity);
        }
        SCBackupFile scBackupFile = selfCareServiceBackupFile.get(fileId);
        if(scBackupFile != null){
            ResponseEntity<String> responseEntity = this.acsClient.downloadUrlFile(deviceId, scBackupFile.fileType, scBackupFile.url, null, null, null, null, null, 0, null, 0, true, null, null, scTaskForm.now);
            return selfCareService.convertToSCTask(responseEntity);
        }
        return null;
    }

    @POST
    @Path("/{deviceId}/uploadFile")
    @ApiOperation(value = "uploadFile device", notes = "{\"now\":false,\"parameters\":{\"fileType\":\"1 Vendor Configuration File or 2 Vendor Log SCFile\"}}")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Tasks have been successfully executed", response = SCTask.class)
        ,@ApiResponse(code = 202, message = "Tasks have been queued to be executed at the next inform", response = SCTask.class)
    })
    public SCTask uploadFile(@PathParam("deviceId") String deviceId, @RequestBody SCTaskForm scTaskForm) {
        String fileType = (String) scTaskForm.parameters.get("fileType");
        ResponseEntity<String> responseEntity = this.acsClient.uploadFile(deviceId, fileType, scTaskForm.now);
        return selfCareService.convertToSCTask(responseEntity);
    }

    @POST
    @Path("/{deviceId}/getRPCMethods")
    @ApiOperation(value = "getRPCMethods device")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Tasks have been successfully executed", response = SCTask.class)
        ,@ApiResponse(code = 202, message = "Tasks have been queued to be executed at the next inform", response = SCTask.class)
    })
    public SCTask getRPCMethods(@PathParam("deviceId") String deviceId, @RequestBody SCTaskForm scTaskForm) {
        ResponseEntity<String> responseEntity = acsClient.getRPCMethods(deviceId, scTaskForm.now);
        return selfCareService.convertToSCTask(responseEntity);
    }

    @POST
    @Path("/{deviceId}/get-parameter-attributes")
    @ApiOperation(value = "GetParameterAttribute device", notes = "{\"now\":false,\"parameters\":{\"parameters\":[[\"InternetGatewayDevice.DeviceInfo.SerialNumber\"],[\"InternetGatewayDevice.DeviceInfo.ModelName\"]]}}")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Tasks have been successfully executed", response = SCTask.class)
        ,@ApiResponse(code = 202, message = "Tasks have been queued to be executed at the next inform", response = SCTask.class)
    })
    public SCTask getParameterAttributes(@PathParam("deviceId") String deviceId, @RequestBody SCTaskForm scTaskForm) {
        List<String> parameters = (List<String>) scTaskForm.parameters.get("parameters");
        ResponseEntity<String> responseEntity = this.acsClient.getParameterAttributes(deviceId, parameters, scTaskForm.now);
        return selfCareService.convertToSCTask(responseEntity);
    }

    @POST
    @Path("/{deviceId}/set-parameter-attributes")
    @ApiOperation(value = "SetParameterAttribute device", notes = "{\"now\":false,\"parameters\":{\"parameters\":\"[['aaa','true','2','false'],['aa','true','2','false']]\"}}\n")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Tasks have been successfully executed", response = SCTask.class)
        ,@ApiResponse(code = 202, message = "Tasks have been queued to be executed at the next inform", response = SCTask.class)
    })
    public SCTask setParameterAttributes(@PathParam("deviceId") String deviceId, @RequestBody SCTaskForm scTaskForm) {
        String parameters = (String) scTaskForm.parameters.get("parameters");
        ResponseEntity<String> responseEntity = this.acsClient.setParameterAttributes(deviceId, parameters, scTaskForm.now);
        return selfCareService.convertToSCTask(responseEntity);
    }

    @POST
    @Path("/{deviceId}/recheck-status")
    @ApiOperation(value = "checkstatus device")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Tasks have been successfully executed", response = SCTask.class)
        ,@ApiResponse(code = 202, message = "Tasks have been queued to be executed at the next inform", response = SCTask.class)
    })
    public SCTask recheckStatus(@PathParam("deviceId") String deviceId) throws XMPPException, ParseException {
        try {
            SCDevice scDevice = selfCareService.getDevice(deviceId);
            if(scDevice.productClass.equals("STB")){
                ConnectionConfiguration config = new ConnectionConfiguration(xmppHost, xmppPort);
                XMPPConnection connection = new XMPPConnection(config);
                connection.connect();
                connection.login(xmppUsername, xmppPassword, xmppResource);
                IQ req=new ConnectionIQ();
                req.setTo(String.format("%s@%s/%s",deviceId,xmppDomain,xmppResource));
                connection.sendPacket(req);
                SCTask scTask = new SCTask();
                scTask.httpStatus = HttpStatus.OK.value();
                return scTask;
            }
            else {
                ResponseEntity<String> responseEntity = this.acsClient.recheckStatus(deviceId);
                return selfCareService.convertToSCTask(responseEntity);
            }
        } catch (HttpStatusCodeException exception) {
            SCTask scTask = new SCTask();
            scTask.httpStatus = exception.getStatusCode().value();
            return scTask;
        }
    }

    @POST
    @Path("/{deviceId}/ping")
    @ApiOperation(value = "ping to device")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Tasks have been successfully executed", response = SCPing.class)
    })
    public SCPing pingtoDevice(@PathParam("deviceId") String deviceId) throws ParseException {
        return selfCareService.pingToDevice(deviceId);
    }

    @POST
    @Path("/{deviceId}/diagnostic")
    @ApiOperation(value = "Send diagnostic to device")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Tasks have been successfully executed", response = SCDiagnostic.class)
    })
    public SCDiagnostic diagnostic(@PathParam("deviceId") String deviceId, @RequestBody SCDiagnosticForm sCDiagnosticForm) {
        ResponseEntity<String> result = acsClient.createDiagnostic(deviceId, sCDiagnosticForm.setValueParameters, true);
        DeviceTypeVersion deviceTypeVersion = deviceTypeVersionService.findbyDevice(deviceId);
        DiagnosticTask diagnosticTask = new DiagnosticTask();
        diagnosticTask.deviceId = deviceId;
        diagnosticTask.diagnosticsName = sCDiagnosticForm.name;
        diagnosticTask.parameterFull = deviceTypeVersion.diagnostics.get(diagnosticTask.diagnosticsName).parameters;
        diagnosticTask.request = sCDiagnosticForm.setValueParameters;
        JsonObject object = new Gson().fromJson(result.getBody(), JsonObject.class);
        diagnosticTask.taskId = object.get("_id").getAsString();
        return new SCDiagnostic(diagnosticService.create(diagnosticTask));
    }

    @POST
    @ApiOperation(value = "Read diagnostic task by task id")
    @ApiResponse(code = 200, message = "Success", response = SCDiagnostic.class)
    @Path("/getDiagnosticTask/{taskId}")
    public SCDiagnostic readDiagnosticTask(@PathParam("taskId") String taskId) {
        return new SCDiagnostic(diagnosticService.findByTaskId(taskId));
    }
}
