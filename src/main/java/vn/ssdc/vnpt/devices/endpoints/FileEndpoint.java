package vn.ssdc.vnpt.devices.endpoints;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import vn.ssdc.vnpt.AcsClient;
import vn.ssdc.vnpt.dto.AcsResponse;
import vn.vnpt.ssdc.utils.ObjectUtils;

import javax.ws.rs.*;

import java.util.*;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;

/**
 * Created by Lamborgini on 3/30/2017.
 */
@Component
@Path("files")
@Produces(APPLICATION_JSON)
@Consumes({APPLICATION_JSON, TEXT_PLAIN})
@Api("Files")
public class FileEndpoint {

    @Autowired
    private AcsClient acsClient;

    @POST
    @Path("/search-file/{query}/{limit}/{offset}/{parameters}")
    public AcsResponse searchFile(@ApiParam(required = false, value = "Query string in mongo db syntax") @DefaultValue("") @PathParam("query") String query,
                                  @ApiParam(value = "Starting index of the returned list, default is 0") @PathParam("offset") String offset,
                                  @ApiParam(value = "Number of returned devices, default is 50") @PathParam("limit") String limit,
                                  @ApiParam(value = "List of selected parameters, separated by comma") @PathParam("parameters") String parameters) {
        AcsResponse response = new AcsResponse();
        Map<String, String> queryParams = new HashMap<String, String>();
        if (query.startsWith("{")) {
            queryParams.put("query", query);
        } else {
            queryParams.put("query", "{" + query + "}");
        }
        queryParams.put("limit", limit);
        queryParams.put("sort", "{\"uploadDate\":-1}");
        queryParams.put("skip", offset);
        if (!ObjectUtils.empty(parameters)) {
            queryParams.put("projection", parameters);
        }
        ResponseEntity<String> responseEntity = this.acsClient.searchFile("files", queryParams);
        response.httpResponseCode = responseEntity.getStatusCodeValue();
        response.body = responseEntity.getBody();
        response.nbOfItems = Integer.parseInt(responseEntity.getHeaders().get("totalAll").get(0));

        return response;
    }

    @POST
    @Path("/update-file")
    public AcsResponse updateFile(Map<String, Object> request) {
        AcsResponse response = new AcsResponse();
        Map<String, String> queryParams = new HashMap<String, String>();

        queryParams.put("update", "{\"_id\" : ObjectId(\"593771aeddcfbe56abd9279d\")},{$set: {\"metadata.oui\": \"Ahihi\"}}");

        ResponseEntity<String> responseEntity = this.acsClient.searchFile("files", queryParams);
        response.httpResponseCode = responseEntity.getStatusCodeValue();
        response.body = responseEntity.getBody();
        response.nbOfItems = Integer.parseInt(responseEntity.getHeaders().get("totalAll").get(0));

        return response;
    }

    @POST
    @Path("/delete-file/{paramId}/{paramName}")
    public String deleteFile(@ApiParam(value = "Id file in mongodb") @PathParam("paramId") String paramId,
                             @ApiParam(value = "File name in mongodb") @PathParam("paramName") String paramName) {
        return this.acsClient.delete("files", paramId, paramName);
    }

    @POST
    @Path("/check-by-version/{list}")
    public boolean checkByVersion(@ApiParam(value = "List oui,productClass,version to check existed") @PathParam("list") String list) {

        AcsResponse response = new AcsResponse();
        Map<String, String> queryParams = new HashMap<String, String>();
        StringBuilder sbOR = new StringBuilder("{\"$or\":[");
        String[] listArray = list.split(",");
        if(listArray.length > 0){
            for (int i = 0; i < listArray.length;) {
                StringBuilder sbAND = new StringBuilder(",{");
                sbAND.append(String.format("\"metadata.oui\":\"%s\"", listArray[i]));
                sbAND.append(String.format(",\"metadata.productClass\":\"%s\"", listArray[i+1]));
                sbAND.append(String.format(",\"metadata.version\":\"%s\"", listArray[i+2]));
                sbAND.append("}");
                sbOR.append(sbAND.toString());
                i = i+3;
            }
        }

        sbOR.deleteCharAt(8);
        sbOR.append("]}");
        queryParams.put("query", sbOR.toString());
        queryParams.put("projection", "_id");

        ResponseEntity<String> responseEntity = this.acsClient.searchFile("files", queryParams);
        response.httpResponseCode = responseEntity.getStatusCodeValue();
        if (responseEntity.getBody().contains("_id")) {
            return false;
        }

        return true;
    }
}
