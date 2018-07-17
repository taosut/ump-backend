/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vn.ssdc.vnpt.selfCare.endpoints;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import vn.ssdc.vnpt.AcsClient;
import vn.ssdc.vnpt.selfCare.model.SCTask;
import vn.ssdc.vnpt.selfCare.model.searchForm.SCTaskSearchForm;
import vn.ssdc.vnpt.selfCare.services.SelfCareServiceDevice;
import vn.ssdc.vnpt.selfCare.services.SelfCareServiceTask;

/**
 *
 * @author Admin
 */
@Component
@Path("/self-care/tasks")
@Produces(APPLICATION_JSON)
@Consumes({APPLICATION_JSON, TEXT_PLAIN})
@Api("Self-Care Tasks")
public class SCTaskEndPoint {

    @Autowired
    private AcsClient acsClient;

    @Autowired
    private SelfCareServiceTask selfCareServiceTask;

    @GET
    @Path("/{deviceId}")
    @ApiOperation(value = "Get List Task by deviceId")
    public List<SCTask> getTasksByDeviceId(@PathParam("deviceId") String deviceId) {
        Map<String, String> queryParams = new HashMap<String, String>();
        queryParams.put("query", "{\"" + "device" + "\":\"" + deviceId + "\"}");
        ResponseEntity<String> responseEntity = this.acsClient.search("tasks", queryParams);
        return selfCareServiceTask.convertResponseToSCTask(responseEntity);
    }

    @POST
    @Path("/search")
    @ApiOperation(value = "Get List Task by query")
    public List<SCTask> search(@RequestBody SCTaskSearchForm sCTaskSearchForm) {
        return selfCareServiceTask.search(sCTaskSearchForm);
    }
    
    
    @POST
    @Path("/count")
    @ApiOperation(value = "Get List Task by query")
    public long count(@RequestBody SCTaskSearchForm sCTaskSearchForm) {
        return selfCareServiceTask.count(sCTaskSearchForm);
    }

    @DELETE
    @Path("/{taskId}")
    @ApiOperation(value = "Delete task")
    public void delete(@PathParam("taskId") String taskId) {
        acsClient.deleteTask(taskId);
    }

    @PUT
    @Path("/{taskId}")
    @ApiOperation(value = "retry task")
    public void retry(@PathParam("taskId") String taskId) {
        acsClient.retryTask(taskId);
    }

}
