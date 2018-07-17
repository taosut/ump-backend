/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vn.ssdc.vnpt.qos.endpoints;

import io.searchbox.client.JestClient;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
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

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import vn.ssdc.vnpt.alarm.model.AlarmDetailELK;
import vn.ssdc.vnpt.qos.model.QosKpiDataELK;
import vn.ssdc.vnpt.qos.model.QosGraph;
import vn.ssdc.vnpt.qos.model.searchForm.QosGraphDataSearchForm;
import vn.ssdc.vnpt.qos.model.searchForm.QosGraphSearchForm;
import vn.ssdc.vnpt.qos.services.QosELKService;
import vn.ssdc.vnpt.qos.services.QosGraphService;
import vn.ssdc.vnpt.qos.services.QosKpiService;
import vn.ssdc.vnpt.utils.StringUtils;

/**
 *
 * @author kiendt
 */
@Component
@Path("qos/graphs")
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
@Api("Qos Graph")
public class QosGraphEndpoint {

    @Autowired
    private QosGraphService qosGraphService;


    private static final Logger logger = LoggerFactory.getLogger(QosGraphEndpoint.class);

    @GET
    @Path("/{id}")
    @ApiOperation(value = "Get info Qos Graph")
    public QosGraph get(@PathParam("id") Long id) {
        return qosGraphService.get(id);
    }

    @POST
    @Path("/search")
    @ApiOperation(value = "Search Qos Graph")
    public List<QosGraph> search(@RequestBody QosGraphSearchForm searchForm) {
        return qosGraphService.search(searchForm);
    }

    @POST
    @Path("/count")
    @ApiOperation(value = "Count Qos Graph")
    public int count(@RequestBody QosGraphSearchForm searchForm) {
        return qosGraphService.count(searchForm);
    }

    @POST
    @ApiOperation(value = "create qos graph")
    public QosGraph add(@RequestBody QosGraph qosGraph) {
        qosGraph.standardObject();
        qosGraphService.validate(qosGraph);
        return qosGraphService.create(qosGraph);
    }

    @PUT
    @Path("/{id}")
    @ApiOperation(value = "update Qos Graph")
    public QosGraph update(@PathParam("id") Long id, @RequestBody QosGraph qosGraph) {
        qosGraph.standardObject();
        qosGraphService.validate(qosGraph);
        qosGraph.id = id;
        return qosGraphService.update(id, qosGraph);
    }

    @DELETE
    @Path("/{id}")
    @ApiOperation(value = "update Qos Graph")
    public void delete(@PathParam("id") Long id) {
        qosGraphService.delete(id);
    }


}
