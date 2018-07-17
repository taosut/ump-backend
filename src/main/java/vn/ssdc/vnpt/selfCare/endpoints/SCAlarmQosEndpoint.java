package vn.ssdc.vnpt.selfCare.endpoints;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import vn.ssdc.vnpt.qos.model.QosAlarmDeviceNew;
import vn.ssdc.vnpt.selfCare.model.SCAlarm;
import vn.ssdc.vnpt.selfCare.model.SCQosAlarmDevice;
import vn.ssdc.vnpt.selfCare.model.searchForm.SCAlarmQosDashboardSeachForm;
import vn.ssdc.vnpt.selfCare.model.searchForm.SCAlarmQosSeachForm;
import vn.ssdc.vnpt.selfCare.services.SelfCareServiceAlarmQos;

import javax.ws.rs.*;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;

@Component
@Path("/self-care/alarms-qos")
@Produces(APPLICATION_JSON)
@Consumes({APPLICATION_JSON, TEXT_PLAIN})
@Api("Self-Care Alarms Qos")
public class SCAlarmQosEndpoint {
    @Autowired
    SelfCareServiceAlarmQos selfCareServiceAlarmQos;

    /**
     * Search alarms
     *
     * @param searchParameter
     * @return
     */
    @POST
    @Path("/search")
    @ApiOperation(value = "Get alarms from elastic+")
    @ApiResponse(code = 200, message = "Success", response = SCAlarm.class)
    public List<SCQosAlarmDevice> search(@RequestBody SCAlarmQosSeachForm searchParameter) throws IOException, ParseException {
        List<SCQosAlarmDevice> lstReturn = new ArrayList<>();
        List<QosAlarmDeviceNew> lstQosAlarmDeviceNews = selfCareServiceAlarmQos.getPage(searchParameter);
        for(QosAlarmDeviceNew qosAlarmDeviceNew : lstQosAlarmDeviceNews){
            SCQosAlarmDevice qosAlarmDevice = new SCQosAlarmDevice(qosAlarmDeviceNew);
            lstReturn.add(qosAlarmDevice);
        }
        return lstReturn;
    }

    /**
     * Count alarms
     *
     * @param searchParameter
     * @return
     */
    @POST
    @Path("/count")
    @ApiOperation(value = "Total alarms from elastic+")
    @ApiResponse(code = 200, message = "Success")
    public Integer count(@RequestBody SCAlarmQosSeachForm searchParameter) throws IOException, ParseException {
        return selfCareServiceAlarmQos.count(searchParameter);
    }

    /**
     * Delete alarms
     *
     * @param strId
     * @return
     */

    @DELETE
    @ApiOperation(value = "Delete alarms from elastic+")
    @ApiResponse(code = 200, message = "Success")
    @Path("/{id}")
    public void delete(@PathParam("id") String strId) throws  IOException {
        selfCareServiceAlarmQos.deleteIndex(strId);
    }

    /**
     * Update alarms complete
     *
     * @param strId
     * @return
     */

    @POST
    @ApiOperation(value = "Update alarms from elastic+")
    @ApiResponse(code = 200, message = "Success")
    @Path("updateComplete/{strId}")
    public void updateStatusComplete(@PathParam("strId") String strId) throws IOException {
        selfCareServiceAlarmQos.updateStatusComplete(strId);
    }

    /**
     * API get alarms severity
     *
     * @param searchParameter
     * @return
     */

    @POST
    @Path("/alarmsSeverity")
    @ApiOperation(value = "Get alarms severity")
    @ApiResponse(code = 200, message = "Success")
    public String alarmsSeverity(@RequestBody SCAlarmQosDashboardSeachForm searchParameter) {
        return "";
    }
}
