package vn.ssdc.vnpt.alarm.endpoints;

import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import vn.ssdc.vnpt.alarm.model.Alarm;
import vn.ssdc.vnpt.alarm.model.AlarmDetails;
import vn.ssdc.vnpt.alarm.services.AlarmDetailsService;
import vn.vnpt.ssdc.core.SsdcCrudEndpoint;

import javax.ws.rs.*;

import java.util.List;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

/**
 * Created by thangnc on 01-Aug-17.
 */
@Component
@Path("alarm-detail")
@Api("Alarm Detail")
@Produces(APPLICATION_JSON)
public class AlarmDetailEndPoint extends SsdcCrudEndpoint<Long, AlarmDetails> {

    private AlarmDetailsService alarmDetailsService;

    @Autowired
    public AlarmDetailEndPoint(AlarmDetailsService alarmDetailsService) {
        this.service = this.alarmDetailsService = alarmDetailsService;
    }

    @GET
    @Path("/get-alarm-detail-by-alarm-type-id")
    public List<AlarmDetails> getAlarmDetailByAlarmType(@DefaultValue("") @QueryParam("alarmId") Long alarmId) {
        return this.alarmDetailsService.getAlarmDetailById(alarmId);
    }

}
