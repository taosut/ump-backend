package vn.ssdc.vnpt.label.endpoints;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import vn.ssdc.vnpt.label.model.Label;
import vn.ssdc.vnpt.label.services.LabelService;
import vn.vnpt.ssdc.core.SsdcCrudEndpoint;

import javax.ws.rs.*;

import java.util.List;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Component
@Path("label")
@Api("Label")
@Produces(APPLICATION_JSON)
public class LabelEndpoint extends SsdcCrudEndpoint<Long, Label> {

    private LabelService labelService;

    @Autowired
    public LabelEndpoint(LabelService labelService) {
        this.service = this.labelService = labelService;
    }

    @GET
    @Path("/load-label-tree-by-node")
    @ApiOperation(value = "Search Alarm")
    @ApiResponse(code = 200, message = "Success", response = LabelEndpoint.class)
    public List<Label> loadLabelTreeByNode(
            @ApiParam(value = "Root Tree") @DefaultValue("") @QueryParam("parentId") String parentId) {
        return this.labelService.loadLabelTreeByNode(parentId);
    }

    @GET
    @Path("/check-label-name")
    public int checkName(
            @ApiParam(value = "Check label name in db") @DefaultValue("") @QueryParam("nameLabel") String nameLabel,
            @ApiParam(value = "Check label parent in db") @DefaultValue("") @QueryParam("parentLabel") String parentLabel) {
        return this.labelService.checkName(nameLabel, parentLabel);
    }
}
