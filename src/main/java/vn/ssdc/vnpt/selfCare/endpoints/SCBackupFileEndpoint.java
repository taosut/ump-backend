package vn.ssdc.vnpt.selfCare.endpoints;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import vn.ssdc.vnpt.selfCare.model.SCBackupFile;
import vn.ssdc.vnpt.selfCare.model.searchForm.SCBackupFileSearchForm;
import vn.ssdc.vnpt.selfCare.services.SelfCareServiceBackupFile;

import javax.ws.rs.*;
import java.text.ParseException;
import java.util.List;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;

/**
 * Created by THANHLX on 11/29/2017.
 */
@Component
@Path("/self-care/backup-files")
@Produces(APPLICATION_JSON)
@Consumes({APPLICATION_JSON, TEXT_PLAIN})
@Api("Self-Care Backup Files")
public class SCBackupFileEndpoint {
    @Autowired
    private SelfCareServiceBackupFile selfCareServiceBackupFile;

    @GET
    @ApiOperation(value = "Read backup file by file id")
    @ApiResponse(code = 200, message = "Success", response = SCBackupFile.class)
    @Path("/{fileId}")
    public SCBackupFile read(@PathParam("fileId") String fileId) throws ParseException{
        return selfCareServiceBackupFile.get(fileId);
    }

    @DELETE
    @ApiOperation(value = "Delete backup file by file id")
    @Path("/{fileId}")
    public void delete(@PathParam("fileId") String fileId) throws ParseException {
        selfCareServiceBackupFile.delete(fileId);
    }

    /**
     * Search backup files
     *
     * @param searchParameter
     * @return
     */
    @POST
    @Path("/search")
    @ApiOperation(value = "Search backup files")
    @ApiResponse(code = 200, message = "Success", response = SCBackupFile.class)
    public List<SCBackupFile> search(@RequestBody SCBackupFileSearchForm searchParameter) throws ParseException{
        return selfCareServiceBackupFile.search(searchParameter);
    }

    /**
     * Count backup files
     *
     * @param searchParameter
     * @return
     */
    @POST
    @Path("/count")
    @ApiOperation(value = "Count backup files")
    @ApiResponse(code = 200, message = "Success")
    public int count(@RequestBody SCBackupFileSearchForm searchParameter) {
        return selfCareServiceBackupFile.count(searchParameter);
    }
}
