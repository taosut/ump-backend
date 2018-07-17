/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vn.ssdc.vnpt.demo;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.io.IOException;
import java.util.Set;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import vn.ssdc.vnpt.qos.model.QosKpiDataELK;
import vn.ssdc.vnpt.qos.services.QosELKService;

/**
 *
 * @author kiendt
 */
@Component
@Path("qos/demo")
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
@Api("Qos Demo")
public class DemoQosEndpoint {

    @Autowired
    QosELKService qosELKService;

    @POST
    @Path("/create")
    @ApiOperation(value = "Create Qos Data")
    public void createDataDemo(@RequestBody Set<QosKpiDataELK> qosDataElks) throws IOException {
        for (QosKpiDataELK tmp : qosDataElks) {
            qosELKService.create(tmp);
        }
    }

}
