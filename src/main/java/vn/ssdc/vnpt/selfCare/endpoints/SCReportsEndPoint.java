package vn.ssdc.vnpt.selfCare.endpoints;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import vn.ssdc.vnpt.devices.model.DeviceTypeVersion;
import vn.ssdc.vnpt.devices.services.DeviceTypeService;
import vn.ssdc.vnpt.devices.services.DeviceTypeVersionService;
import vn.ssdc.vnpt.label.model.Label;
import vn.ssdc.vnpt.label.services.LabelService;
import vn.ssdc.vnpt.reports.model.ReportsData;
import vn.ssdc.vnpt.reports.services.ReportsDataService;
import vn.ssdc.vnpt.selfCare.model.SCDevice;
import vn.ssdc.vnpt.selfCare.model.searchForm.SCDeviceSearchForm;
import vn.ssdc.vnpt.selfCare.model.searchForm.SCReportsSearchForm;
import vn.ssdc.vnpt.selfCare.services.SelfCareServiceDevice;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import java.sql.Timestamp;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;

/**
 * Created by TUANHA2 on 11/27/2017.
 */
@Component
@Path("/self-care/reports")
@Produces(APPLICATION_JSON)
@Consumes({APPLICATION_JSON, TEXT_PLAIN})
@Api("Self-Care Reports")
public class SCReportsEndPoint {
    @Autowired
    private SelfCareServiceDevice selfCareService;

    @Autowired
    private LabelService labelService;

    @Autowired
    private DeviceTypeService deviceTypeService;

    @Autowired
    private DeviceTypeVersionService deviceTypeVersionService;

    @Autowired
    private ReportsDataService reportsDataService;

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(SCReportsEndPoint.class);

    /**
     * Reports
     *
     * @param scReportsSearchForm
     * @return specificID from table reports_data in mysql
     */
    @POST
    @Path("/getIdReports")
    @ApiOperation(value = "Get Id Reports")
    @ApiResponse(code = 200, message = "Success")
    public long getIdReports(@RequestBody SCReportsSearchForm scReportsSearchForm) throws ParseException {
        String idReports = scReportsSearchForm.id;
        String reportName = scReportsSearchForm.reportName;
        String labels = scReportsSearchForm.label;
        String model = scReportsSearchForm.model;
        String firmwareVersion = scReportsSearchForm.firmwareVersion;
        //
        String username = scReportsSearchForm.username;
        //
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        long specificID = timestamp.getTime();

        //check if blank
        if (labels!=null && labels.equals("")) {
            labels = "ALL";
        }
        if (model!=null && model.equals("")) {
            model = null;
        }
        if (firmwareVersion!=null && firmwareVersion.equals("")) {
            firmwareVersion = null;
        }
        //if not null convert - to ,
        if(model!=null){
            model = model.replaceAll("-","','");
        }
        if(firmwareVersion!=null){
            firmwareVersion = firmwareVersion.replaceAll("-","','");
        }
        if (labels!=null&&!labels.equalsIgnoreCase("ALL")) {
            //GET SPECIFIC
            List<Label> arrLabels = labelService.getAll();
            List<DeviceTypeVersion> lstdeviceTypeVersions = deviceTypeVersionService.findByDeviceTypeVersionForReports(model,firmwareVersion);

            String[] id = labels.split("-");
            //For each label
            for (int i = 0; i < id.length; i++) {
                String LabelsName = "";
                Long labelsId = Long.parseLong(id[i]);
                ///
                for (int k = 0; k < arrLabels.size(); k++) {
                    if (labelsId.equals(arrLabels.get(k).id)) {
                        LabelsName = arrLabels.get(k).name;

                        ///
                        SCDeviceSearchForm searchByLabel = new SCDeviceSearchForm();
                        searchByLabel.label = LabelsName;
                        Integer countByLabel = selfCareService.countDevice(searchByLabel);
                        ///
                        for(DeviceTypeVersion deviceTypeVersion_byModel : lstdeviceTypeVersions){
                            String strModel = deviceTypeVersion_byModel.modelName;
                            ///
                            SCDeviceSearchForm searchByLabelMode = new SCDeviceSearchForm();
                            searchByLabelMode.label = LabelsName;
                            searchByLabelMode.modelName = strModel;
                            Integer countByLabelModel = selfCareService.countDevice(searchByLabelMode);
                            ///
                            for(DeviceTypeVersion deviceTypeVersion_byFW : lstdeviceTypeVersions){
                                String strFirmwareVersion = deviceTypeVersion_byFW.firmwareVersion;

                                SCDeviceSearchForm scDeviceSearchForm = new SCDeviceSearchForm();
                                scDeviceSearchForm.label = LabelsName;
                                scDeviceSearchForm.modelName = strModel;
                                scDeviceSearchForm.firmwareVersion = strFirmwareVersion;
                                scDeviceSearchForm.userName = username;

                                Integer count = selfCareService.countDevice(scDeviceSearchForm);
                                //
                                List<SCDevice> lstScDevices = new ArrayList<>();
                                if(idReports.equalsIgnoreCase("4") || idReports.equalsIgnoreCase("3")){
                                    lstScDevices = selfCareService.searchDevice(scDeviceSearchForm);
                                }

                                if (count != 0) {
                                    handleDataReports(LabelsName, strModel, strFirmwareVersion, count, countByLabel, countByLabelModel, specificID, lstScDevices, idReports);
                                }
                            }
                        }

                    }
                }
            }
        } else {
            //GET ALL LABELS
            List<Label> arrLabels = labelService.getAll();
            List<DeviceTypeVersion> lstdeviceTypeVersions = deviceTypeVersionService.findByDeviceTypeVersionForReports(model,firmwareVersion);

            for (Label label : arrLabels) {
                String LabelsName = label.name;
                ///
                SCDeviceSearchForm searchByLabel = new SCDeviceSearchForm();
                searchByLabel.label = LabelsName;
                Integer countByLabel = selfCareService.countDevice(searchByLabel);
                ///

                for(DeviceTypeVersion deviceTypeVersion_byModel : lstdeviceTypeVersions){
                    String strModel = deviceTypeVersion_byModel.modelName;
                    ///
                    SCDeviceSearchForm searchByLabelMode = new SCDeviceSearchForm();
                    searchByLabelMode.label = LabelsName;
                    searchByLabelMode.modelName = strModel;
                    Integer countByLabelModel = selfCareService.countDevice(searchByLabelMode);
                    ///
                    for(DeviceTypeVersion deviceTypeVersion_byFW : lstdeviceTypeVersions){
                        String strFirmwareVersion = deviceTypeVersion_byFW.firmwareVersion;

                        SCDeviceSearchForm scDeviceSearchForm = new SCDeviceSearchForm();
                        scDeviceSearchForm.label = LabelsName;
                        scDeviceSearchForm.modelName = strModel;
                        scDeviceSearchForm.firmwareVersion = strFirmwareVersion;
                        scDeviceSearchForm.userName = username;
                        Integer count = selfCareService.countDevice(scDeviceSearchForm);
                        //
                        List<SCDevice> lstScDevices = new ArrayList<>();
                        if(idReports.equalsIgnoreCase("4") || idReports.equalsIgnoreCase("3")){
                            lstScDevices = selfCareService.searchDevice(scDeviceSearchForm);
                        }

                        if (count != 0) {
                            handleDataReports(LabelsName, strModel, strFirmwareVersion, count, countByLabel, countByLabelModel, specificID, lstScDevices, idReports);
                        }
                    }
                }
            }
        }

        return specificID;
    }

    public void handleDataReports(String strLabel, String strModel, String strFirmwareVersion, Integer strCountFW,
                                  Integer strCountByLabel, Integer strCountByLabelModel, Long specificID,
                                  List<SCDevice> lstScDevices, String idReports) {
        logger.info("*******************************************************");
        logger.info("***idReports***" + idReports);
        logger.info("***LabelsName***" + strLabel);
        logger.info("***strModel***" + strModel);
        logger.info("***strFirmwareVersion***" + strFirmwareVersion);
        logger.info("***count***" + strCountFW);
        logger.info("***countByLabel***" + strCountByLabel);
        logger.info("***countByLabelModel***" + strCountByLabelModel);
        logger.info("***specificID***" + specificID);

        if (idReports.equalsIgnoreCase("4")) {
            for(SCDevice scDevice : lstScDevices){
                ReportsData reportsData = new ReportsData();
                reportsData.label = strLabel;
                reportsData.model = strModel;
                reportsData.firmware_version = strFirmwareVersion;
                reportsData.count_by_firmware = strCountFW;
                reportsData.count_by_label_model = strCountByLabelModel;
                reportsData.count_by_label = strCountByLabel;
                reportsData.specific_id = specificID;
                reportsData.serial_number = scDevice.serialNumber;
                reportsData.ip_address = scDevice.ip;
                reportsData.manufacturer = scDevice.manufacturer;
                reportsDataService.create(reportsData);
            }
        } else if (idReports.equalsIgnoreCase("3")){
            int count_Online = 0 ;

            for (SCDevice scDevicesTemp : lstScDevices) {
                if (scDevicesTemp.status.equalsIgnoreCase("online")) {
                    count_Online++;
                }
            }
            ReportsData reportsData = new ReportsData();
            reportsData.label = strLabel;
            reportsData.model = strModel;
            reportsData.firmware_version = strFirmwareVersion;
            reportsData.count_by_firmware = strCountFW;
            reportsData.count_by_label_model = strCountByLabelModel;
            reportsData.count_by_label = strCountByLabel;
            reportsData.specific_id = specificID;
            reportsData.count_online = count_Online;
            reportsDataService.create(reportsData);
        }else{
            ReportsData reportsData = new ReportsData();
            reportsData.label = strLabel;
            reportsData.model = strModel;
            reportsData.firmware_version = strFirmwareVersion;
            reportsData.count_by_firmware = strCountFW;
            reportsData.count_by_label_model = strCountByLabelModel;
            reportsData.count_by_label = strCountByLabel;
            reportsData.specific_id = specificID;
            reportsDataService.create(reportsData);
        }

    }
}
