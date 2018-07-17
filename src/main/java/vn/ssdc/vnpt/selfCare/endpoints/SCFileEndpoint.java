package vn.ssdc.vnpt.selfCare.endpoints;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import vn.ssdc.vnpt.selfCare.model.SCFile;
import vn.ssdc.vnpt.selfCare.services.SelfCareServiceFile;
import vn.ssdc.vnpt.selfCare.model.searchForm.SCFileSearchForm;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.List;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;

/**
 * Created by THANHLX on 11/29/2017.
 */
@Component
@Path("/self-care/files")
@Produces(APPLICATION_JSON)
@Consumes({APPLICATION_JSON, TEXT_PLAIN})
@Api("Self-Care Files")
public class SCFileEndpoint {

    @Autowired
    private SelfCareServiceFile selfCareServiceFile;

    @GET
    @ApiOperation(value = "Read file by id")
    @ApiResponse(code = 200, message = "Success", response = SCFile.class)
    @Path("/{id}")
    public SCFile read(@PathParam("id") String id) throws ParseException {
        return selfCareServiceFile.get(id);
    }

    @POST
    @ApiOperation(value = "Create file")
    @Produces(APPLICATION_JSON)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public SCFile create(
            @FormDataParam("fileName") String fileName,
            @FormDataParam("oui") String oui,
            @FormDataParam("productClass") String productClass,
            @FormDataParam("manufacturer") String manufacturer,
            @FormDataParam("modelName") String modelName,
            @FormDataParam("firmwareVersion") String firmwareVersion,
            @FormDataParam("file") InputStream file,
            @FormDataParam("fileUrl") String fileUrl,
            @FormDataParam("username") String username,
            @FormDataParam("password") String password,
            @FormDataParam("isBasicFirmware") boolean isBasicFirmware,
            @FormDataParam("md5") String md5,
            @FormDataParam("size") String size
            ) {
        return selfCareServiceFile.create(fileName, oui, productClass, manufacturer, modelName, firmwareVersion, file, fileUrl, username, password, isBasicFirmware, md5, size);
    }

    @PUT
    @Path("/{id}")
    @ApiOperation(value = "Update file")
    @Produces(APPLICATION_JSON)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public SCFile update(
            @PathParam("id") String id,
            @FormDataParam("fileName") String fileName,
            @FormDataParam("oui") String oui,
            @FormDataParam("productClass") String productClass,
            @FormDataParam("manufacturer") String manufacturer,
            @FormDataParam("modelName") String modelName,
            @FormDataParam("firmwareVersion") String firmwareVersion,
            @FormDataParam("file") InputStream file,
            @FormDataParam("fileUrl") String fileUrl,
            @FormDataParam("username") String username,
            @FormDataParam("password") String password,
            @FormDataParam("isBasicFirmware") boolean isBasicFirmware,
            @FormDataParam("md5") String md5,
            @FormDataParam("size") String size
    ) throws ParseException, IOException{
        return selfCareServiceFile.update(id, fileName, oui, productClass, manufacturer, modelName, firmwareVersion, file, fileUrl, username, password, isBasicFirmware, md5, size);
    }

    @DELETE
    @ApiOperation(value = "Delete file by id")
    @Path("/{id}")
    public void delete(@PathParam("id") String id) throws ParseException {
        selfCareServiceFile.delete(id);
    }

    /**
     * Search devices
     *
     * @param searchParameter
     * @return
     */
    @POST
    @Path("/search")
    @ApiOperation(value = "Search files")
    @ApiResponse(code = 200, message = "Success", response = SCFile.class)
    public List<SCFile> search(@RequestBody SCFileSearchForm searchParameter) throws ParseException {
        return selfCareServiceFile.search(searchParameter);
    }

    /**
     * Count files
     *
     * @param searchParameter
     * @return
     */
    @POST
    @Path("/count")
    @ApiOperation(value = "Count files")
    @ApiResponse(code = 200, message = "Success")
    public int count(@RequestBody SCFileSearchForm searchParameter) {
        return selfCareServiceFile.count(searchParameter);
    }
}
