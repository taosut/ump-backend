/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vn.ssdc.vnpt.selfCare.endpoints;

import io.swagger.annotations.ApiOperation;
import java.io.Serializable;
import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import vn.ssdc.vnpt.selfCare.services.SelfCareBaseService;

/**
 *
 * @author kiendt
 *
 */
public class SCBaseEndpoint<EntityClass, SearchFormClass, ServiceClass extends SelfCareBaseService<EntityClass, SearchFormClass>> {

    @Autowired
    private ServiceClass serviceClass;

//    @POST
//    @Path("/search")
//    @ApiOperation(value = "do search")
//    public List<EntityClass> search(@RequestBody SearchFormClass searchForm) {
//        return serviceClass.search(searchForm);
//    }
//
//    @POST
//    @Path("/count")
//    @ApiOperation(value = "do count")
//    public int count(@RequestBody SearchFormClass searchForm) {
//        return serviceClass.count(searchForm);
//    }
}
