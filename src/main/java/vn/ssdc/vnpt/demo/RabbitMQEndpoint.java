/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vn.ssdc.vnpt.demo;

import com.google.gson.Gson;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import vn.ssdc.vnpt.rabbitmq.endpoints.CwmpEndPoint;
import vn.vnpt.ssdc.event.Event;

/**
 *
 * @author kiendt
 */
@Component
@Path("rabbitmq")
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
@Api("Qos Rabbitmq")
public class RabbitMQEndpoint {

//    @Value("${rabbitmq.host}")
//    private String rabbitMqHost;
    @Autowired
    private RabbitTemplate template;

    @Autowired
    private CwmpEndPoint cwmpEndPoint;
//
//    private void initRabbitMq() throws IOException, TimeoutException {
//        ConnectionFactory factory = new ConnectionFactory();
//        factory.setHost(rabbitMqHost);
//        Connection connection = factory.newConnection();
//        Channel channel = connection.createChannel();
//    }

    @POST
    @Path("/publicMessage")
    @ApiOperation(value = "Create Qos Data")
    public void createDataDemo(@RequestBody ArrayList<RabbitMqModel> datas) throws IOException {
        for (RabbitMqModel data : datas) {
            Event event = new Event();
            event.message = new HashMap<>();
            event.message.put("deviceId", data.deviceId);
            event.message.put("parameterValues", new Gson().toJson(data.parameterValues));
            cwmpEndPoint.listenQosQueue1(event, data.time);
        }

    }

}
