package vn.ssdc.vnpt.selfCare.endpoints;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import vn.ssdc.vnpt.logging.services.LoggingPolicyService;
import vn.ssdc.vnpt.selfCare.model.SCDeviceActivity;
import vn.ssdc.vnpt.selfCare.model.searchForm.SCDeviceActivitySearchForm;
import vn.ssdc.vnpt.selfCare.services.SelfCareServiceDeviceActivity;

import javax.ws.rs.*;
import java.io.IOException;
import java.text.ParseException;
import java.util.*;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;

/**
 * Created by THANHLX on 11/23/2017.
 */
@Component
@Path("/self-care/device-activities")
@Produces(APPLICATION_JSON)
@Consumes({APPLICATION_JSON, TEXT_PLAIN})
@Api("Self-Care Device Activities")
public class SCDeviceActivityEndpoint {

    @Autowired
    private LoggingPolicyService loggingPolicyService;

    @Autowired
    private SelfCareServiceDeviceActivity selfCareServiceDeviceActivity;

    @DELETE
    @Path("/{id}")
    @ApiOperation(value = "Delete device activity from elastic+ by id")
    public void delete(@PathParam("id") String id) {
        loggingPolicyService.removeById(id);
    }

    /**
     * Search for alarms
     *
     * @param searchParameter
     * @return
     */
    @POST
    @Path("/search")
    @ApiOperation(value = "Get device acitivities from elastic+")
    @ApiResponse(code = 200, message = "Success", response = SCDeviceActivity.class)
    public List<SCDeviceActivity> search(@RequestBody SCDeviceActivitySearchForm searchParameter) throws IOException, ParseException {

        return selfCareServiceDeviceActivity.search(searchParameter);
    }

    @POST
    @Path("/count")
    @ApiOperation(value = "Count device acitivities from elastic+")
    @ApiResponse(code = 200, message = "Success", response = SCDeviceActivity.class)
    public long count(@RequestBody SCDeviceActivitySearchForm searchParameter) throws IOException, ParseException {
        return selfCareServiceDeviceActivity.count(searchParameter);
    }
}
