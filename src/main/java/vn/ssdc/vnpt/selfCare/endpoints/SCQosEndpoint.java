/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vn.ssdc.vnpt.selfCare.endpoints;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import java.io.IOException;
import java.util.*;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import vn.ssdc.vnpt.devices.model.DeviceTypeVersion;
import vn.ssdc.vnpt.devices.model.Tag;
import vn.ssdc.vnpt.devices.services.DeviceTypeVersionService;
import vn.ssdc.vnpt.devices.services.TagService;
import vn.ssdc.vnpt.qos.model.QosGraph;
import vn.ssdc.vnpt.qos.model.QosKpi;
import vn.ssdc.vnpt.qos.model.searchForm.QosGraphSearchForm;
import vn.ssdc.vnpt.qos.model.searchForm.QosKPISearchForm;
import vn.ssdc.vnpt.qos.services.QosELKService;
import vn.ssdc.vnpt.qos.services.QosGraphService;
import vn.ssdc.vnpt.qos.services.QosKpiService;
import vn.ssdc.vnpt.selfCare.model.SCQosGraph;
import vn.ssdc.vnpt.selfCare.model.SCQosKpi;
import vn.ssdc.vnpt.selfCare.model.SCQosSingleDevice;
import vn.ssdc.vnpt.selfCare.model.searchForm.SCAlarmQosDashboardSeachForm;
import vn.ssdc.vnpt.selfCare.model.searchForm.SCAlarmQosDeviceGroupSeachForm;
import vn.ssdc.vnpt.selfCare.model.searchForm.SCAlarmQosSingleDeviceSeachForm;
import vn.ssdc.vnpt.umpexception.QosException;

/**
 * @author kiendt
 */
@Component
@Path("/self-care/qos")
@Produces(APPLICATION_JSON)
@Consumes({APPLICATION_JSON, TEXT_PLAIN})
@Api("Self-Care Qos")
public class SCQosEndpoint {

    @Autowired
    private QosKpiService qosKpiService;

    @Autowired
    private QosGraphService qosGraphService;

    @Autowired
    private QosELKService qosELKService;

    @GET
    @Path("/kpis/{id}")
    @ApiOperation(value = "Get info SCQos KPI")
    public SCQosKpi getQosKpi(@PathParam("id") Long id) {
        return new SCQosKpi(qosKpiService.get(id));
    }

    @POST
    @Path("/kpis/search")
    @ApiOperation(value = "Search SCQos KPI")
    public List<SCQosKpi> searchQosKpi(@RequestBody QosKPISearchForm searchForm) {
        List<SCQosKpi> scQosKpis = new ArrayList<>();
        for (QosKpi tmp : qosKpiService.search(searchForm)) {
            scQosKpis.add(new SCQosKpi(tmp));
        }
        return scQosKpis;
    }

    @POST
    @Path("/kpis/search-absolute")
    @ApiOperation(value = "Search SCQos KPI")
    public List<SCQosKpi> searchQosKpiExactly(@RequestBody QosKPISearchForm searchForm) {
        List<SCQosKpi> scQosKpis = new ArrayList<>();
        for (QosKpi tmp : qosKpiService.searchAbsolute(searchForm)) {
            scQosKpis.add(new SCQosKpi(tmp));
        }
        return scQosKpis;
    }

    @POST
    @Path("/kpis/count")
    @ApiOperation(value = "Count SCQos KPI")
    public int countQosKpi(@RequestBody QosKPISearchForm searchForm) {
        return qosKpiService.count(searchForm);
    }

    @POST
    @Path("/kpis")
    @ApiOperation(value = "create SCQos kpi")
    public SCQosKpi addQosKpi(@RequestBody SCQosKpi scQosKpi) throws QosException {
        QosKpi qosKpi = scQosKpi.convertToQosKpi();
        qosKpi.standardObject();
        qosKpiService.validate(qosKpi);
        return new SCQosKpi(qosKpiService.create(qosKpi));
    }

    @PUT
    @Path("/kpis/{id}")
    @ApiOperation(value = "update Qos KPI")
    public SCQosKpi updateQosKpi(@PathParam("id") Long id, @RequestBody SCQosKpi scQosKpi) throws QosException {
        QosKpi qosKpi = scQosKpi.convertToQosKpi();
        qosKpi.standardObject();
        qosKpiService.validate(qosKpi);
        qosKpi.id = id;
        return new SCQosKpi(qosKpiService.update(id, qosKpi));
    }

    @DELETE
    @Path("/kpis/{id}")
    @ApiOperation(value = "delete Qos KPI")
    public void deleteQosKpi(@PathParam("id") Long id) {
        qosKpiService.delete(id);
    }

    @GET
    @Path("/graphs/{id}")
    @ApiOperation(value = "Get info Qos Graph")
    public SCQosGraph getQosGraph(@PathParam("id") Long id) {
        return new SCQosGraph(qosGraphService.get(id));
    }

    @POST
    @Path("/graphs/search")
    @ApiOperation(value = "Search Qos Graph")
    public List<SCQosGraph> searchQosGraph(@RequestBody QosGraphSearchForm searchForm) {
        List<SCQosGraph> scQosGraphs = new ArrayList<>();
        for (QosGraph tmp : qosGraphService.search(searchForm)) {
            scQosGraphs.add(new SCQosGraph(tmp));
        }
        return scQosGraphs;
    }

    @POST
    @Path("/graphs/count")
    @ApiOperation(value = "Count Qos Graph")
    public int countQosGraph(@RequestBody QosGraphSearchForm searchForm) {
        return qosGraphService.count(searchForm);
    }

    @POST
    @Path("/graphs")
    @ApiOperation(value = "create qos graph")
    public SCQosGraph addQosGraph(@RequestBody SCQosGraph scQosGraph) {
        QosGraph qosGraph = scQosGraph.convertToQosGraph();
        qosGraph.standardObject();
        qosGraphService.validate(qosGraph);
        return new SCQosGraph(qosGraphService.create(qosGraph));
    }

    @PUT
    @Path("/graphs/{id}")
    @ApiOperation(value = "update Qos Graph")
    public SCQosGraph updateQosGraph(@PathParam("id") Long id, @RequestBody SCQosGraph scQosGraph) {
        QosGraph qosGraph = scQosGraph.convertToQosGraph();
        qosGraph.standardObject();
        qosGraphService.validate(qosGraph);
        qosGraph.id = id;
        return new SCQosGraph(qosGraphService.update(id, qosGraph));
    }

    @DELETE
    @Path("/graphs/{id}")
    @ApiOperation(value = "update Qos Graph")
    public void deleteQosGraph(@PathParam("id") Long id) {
        qosGraphService.delete(id);
    }

    ////////////////////////////////////
    //GET 3 FIX ALARM GRAPH
    @POST
    @Path("/dashboardQos/alarmSeverity")
    @ApiOperation(value = "Get data for Dashboard Qos")
    public Map<String, String> getDashboarDataAlarmSeverity(@RequestBody SCAlarmQosDashboardSeachForm searchForm) throws IOException {
        //1.Alarm Severity
        Map<String, String> mapReturn = new HashMap<>();
        mapReturn.put("data", qosELKService.getAlarmSeverity(searchForm).toString());
        return mapReturn;
    }

    /////////////////////////////////////
    @POST
    @Path("/dashboardQos/top5AlarmByKpi")
    @ApiOperation(value = "Get data for Dashboard Qos")
    public Map<String, String> getDashboarDataTop5AlarmByKpi(@RequestBody SCAlarmQosDashboardSeachForm searchForm) throws IOException {
        //2.Top 5 Alarms By KPI
        Map<String, String> mapReturn = new HashMap<>();
        mapReturn.put("data", qosELKService.getTop5AlarmByKpi(searchForm).toString());
        return mapReturn;
    }

    /////////////////////////////////////
    @POST
    @Path("/dashboardQos/alarmTrends")
    @ApiOperation(value = "Get data for Dashboard Qos")
    public List<List<Object>> getDashboarDataAlarmTrends(@RequestBody SCAlarmQosDashboardSeachForm searchForm) throws IOException {
        //3.Alarm Trends
        return qosELKService.getAlarmTrends(searchForm);
    }

    /////////////////////////////////////
    //GET QOS KPI DASHBOARD
    @POST
    @Path("/dashboardQos/qosKpi")
    @ApiOperation(value = "Get data for Dashboard Qos")
    public LinkedHashMap<String, String> getDashboarDataQosKpi(@RequestBody SCAlarmQosDashboardSeachForm searchForm) throws IOException {
        LinkedHashMap<String, String> mapReturn = new LinkedHashMap<>();
        List<QosKpi> lstQosKpi = qosKpiService.getForDashboard();
        //

        for (int i = lstQosKpi.size()-1 ; i >= 0 ; i--) {
            QosKpi qosKpi = lstQosKpi.get(i);
            JsonArray lstSingleQosKpi = qosELKService.getForEachQosKpi(searchForm, qosKpi);
            mapReturn.put(qosKpi.kpiIndex, lstSingleQosKpi.toString());
        }
        return mapReturn;
    }

    ///////////////////////////////////////
    //GET QOS FOR SINGLE DEVICE
    @Autowired
    DeviceTypeVersionService deviceTypeVersionService;

    @Autowired
    TagService tagService;

    @POST
    @Path("/dashboardQos/qosKpiSingleDevice")
    @ApiOperation(value = "Get data for single device")
    public SCQosSingleDevice getDashboarDataQosKpiSingleDevice(@RequestBody SCAlarmQosSingleDeviceSeachForm searchForm) throws IOException {
        List<List<List<Object>>> mapReturn = new ArrayList<>();
        ///Device ID = > Device Type Version
        DeviceTypeVersion deviceTypeVersion = deviceTypeVersionService.findbyDevice(searchForm.deviceId);
        ///Device Type Version => List tag (Profile Id)
        List<Tag> lstProfileId = tagService.findByDeviceTypeVersion(deviceTypeVersion.id);
        ///
        ///size = 0 => no kpi
        if (lstProfileId.size() > 0) {
            String strProfileId = "";
            for (Tag tag : lstProfileId) {
                strProfileId += tag.id + ",";
            }
            //remove char ","
            strProfileId = strProfileId.substring(0, strProfileId.length() - 1);
            List<QosKpi> lstQosKpi = qosKpiService.getForSingleDevice(strProfileId);
            if (lstQosKpi.size() > 0) {
                //
                for (QosKpi qosKpi : lstQosKpi) {
                    List<List<Object>> lstData = qosELKService.getDataForSingleDevice(searchForm, qosKpi);
                    mapReturn.add(lstData);
                }
                //
            }
        }

        SCQosSingleDevice scQosSingleDevice = new SCQosSingleDevice();
        scQosSingleDevice.allDataDevice = mapReturn;
        return scQosSingleDevice;
    }

    @POST
    @Path("/dashboardQos/qosKpiDeviceGroup")
    @ApiOperation(value = "Get data for device group")
    public List<List<Object>> getDashboarDataQosKpiDeviceGroup(@RequestBody SCAlarmQosDeviceGroupSeachForm searchForm) throws IOException {
        return qosELKService.getDataForDeviceGroup(searchForm);
    }
//    @POST
//    @Path("/graphData")
//    @ApiOperation(value = "Get data for QoS graph")
//    public SCQosGraph graphData(@RequestBody QosGraphDataSearchForm qosGraphDataSearchForm) {
//        List<List<String>> setReturn = new ArrayList<>();
//        //Get QosGraph
//        Long qosGraphId = qosGraphDataSearchForm.qosGraphId;
//        QosGraph qosGraph = qosGraphService.get(qosGraphId);
//
//        SCQosGraph scQosGraph = new SCQosGraph(qosGraph);
//        //Check Type
//
//        //"pie_chart"
//        if(lstGrapType.get(0).equalsIgnoreCase(qosGraph.graphType)){
//            //Have multi KpiId , Must have larger than 2
//            Set<Long> lstKpi = qosGraph.graphIndex;
//            for(Long kpiId : lstKpi){
//                List<QosDataELK> lstData = qosELKService.getDataELKLastRecordInTime(qosGraphDataSearchForm,kpiId);
//                if(!lstData.isEmpty()){
//                    List<String> strData = new ArrayList<>();
//                    strData.add(lstData.get(0).kpiIndex);
//                    strData.add(lstData.get(0).value.toString());
//                    setReturn.add(strData);
//                }else{
//                    List<String> strData = new ArrayList<>();
//                    strData.add(qosKpiService.get(kpiId).kpiIndex);
//                    strData.add(null);
//                    setReturn.add(strData);
//                }
//            }
//        }
//        //"get_stats"
//        else if(lstGrapType.get(3).equalsIgnoreCase(qosGraph.graphType)){
//            //Only have 1 KpiId
//            Long kpiId = qosGraph.graphIndex.iterator().next();
//            List<QosDataELK> lstData = qosELKService.getDataELKLastRecordInTime(qosGraphDataSearchForm,kpiId);
//            List<String> strData = new ArrayList<>();
//            if(lstData.size()>0){
//                strData.add(lstData.get(0).value.toString());
//            }else{
//                strData.add(null);
//            }
//            setReturn.add(strData);
//        }
//        //"column_chart"
//        else if(lstGrapType.get(1).equalsIgnoreCase(qosGraph.graphType)){
//            //
//            setReturn = qosGraphService.hanldleData(qosGraph,qosGraphDataSearchForm);
//            //
//        }
//        //"line_chart"
//        else if(lstGrapType.get(2).equalsIgnoreCase(qosGraph.graphType)){
//            //
//            setReturn = qosGraphService.hanldleData(qosGraph,qosGraphDataSearchForm);
//            //
//        }
//        //"table_list"
//        else if(lstGrapType.get(4).equalsIgnoreCase(qosGraph.graphType)){
//            //
//            setReturn = qosGraphService.hanldleData(qosGraph,qosGraphDataSearchForm);
//            //
//        }
//        scQosGraph.graphData = setReturn;
//
//        return scQosGraph;
//    }

}
