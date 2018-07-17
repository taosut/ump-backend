/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vn.ssdc.vnpt.qos.endpoints;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import vn.ssdc.vnpt.devices.services.TagService;
import vn.ssdc.vnpt.qos.model.QosKpi;
import vn.ssdc.vnpt.qos.model.searchForm.QosKPISearchForm;
import vn.ssdc.vnpt.qos.services.QosKpiService;
import vn.ssdc.vnpt.umpexception.QosException;

/**
 *
 * @author kiendt
 */
@Component
@Path("qos/kpis")
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
@Api("Qos Kpi")
public class QosKpiEndpoint {

    @Autowired
    private QosKpiService qosKpiService;

    @GET
    @Path("/{id}")
    @ApiOperation(value = "Get info Qos KPI")
    public QosKpi get(@PathParam("id") Long id) {
        return qosKpiService.get(id);
    }

    @POST
    @Path("/search")
    @ApiOperation(value = "Search Qos KPI")
    public List<QosKpi> search(@RequestBody QosKPISearchForm searchForm) {
        return qosKpiService.search(searchForm);
    }

    @POST
    @Path("/count")
    @ApiOperation(value = "Count Qos KPI")
    public int count(@RequestBody QosKPISearchForm searchForm) {
        return qosKpiService.count(searchForm);
    }

    @POST
    @ApiOperation(value = "create qos kpi")
    public QosKpi add(@RequestBody QosKpi qosKpi) throws QosException {
        return qosKpiService.create(qosKpi);
    }

    @PUT
    @Path("/{id}")
    @ApiOperation(value = "update Qos KPI")
    public QosKpi update(@PathParam("id") Long id, @RequestBody QosKpi qosKpi) throws QosException {
        qosKpi.id = id;
        return qosKpiService.update(id, qosKpi);
    }

    @DELETE
    @Path("/{id}")
    @ApiOperation(value = "update Qos KPI")
    public void delete(@PathParam("id") Long id) {
        qosKpiService.delete(id);
    }

}
